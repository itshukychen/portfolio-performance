package name.abuchen.portfolio.ui.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.CalculationLineItem;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.util.Interval;

/**
 * Service responsible for caching {@link SecurityPerformanceSnapshot} instances
 * per portfolio and invalidating or refreshing them based on incoming events.
 *
 * This cache relies on explicit events for structural or price changes. A full
 * rebuild happens on structural updates, while price-only updates trigger a
 * fast path that recalculates the affected performance records.
 */
public final class SecurityPerformanceSnapshotCacheService
{
    private static final Logger logger = LoggerFactory.getLogger(SecurityPerformanceSnapshotCacheService.class);

    private static final SecurityPerformanceSnapshotCacheService INSTANCE = new SecurityPerformanceSnapshotCacheService();

    private final ConcurrentMap<String, SnapshotCacheEntry> cache = new ConcurrentHashMap<>();

    private SecurityPerformanceSnapshotCacheService()
    {
    }

    public static SecurityPerformanceSnapshotCacheService getInstance()
    {
        return INSTANCE;
    }

    /**
     * Returns the latest cached (or lazily recomputed) snapshots for the given
     * portfolio. The caller must provide the already-loaded {@link Client}
     * instance.
     *
     * @param portfolioId
     *            the portfolio identifier
     * @param client
     *            cached {@link Client} instance
     * @return bundle containing daily, YTD and all-time snapshots
     */
    public SecurityPerformanceSnapshotBundle getSnapshots(String portfolioId, Client client)
    {
        Objects.requireNonNull(portfolioId, "portfolioId"); //$NON-NLS-1$
        Objects.requireNonNull(client, "client"); //$NON-NLS-1$

        SnapshotCacheEntry entry = cache.computeIfAbsent(portfolioId, id -> new SnapshotCacheEntry());
        return entry.ensureUpToDate(portfolioId, client);
    }

    /**
     * Marks the cached snapshots for the given portfolio as needing a refresh due
     * to price updates on a subset of securities. The actual recalculation happens
     * lazily on the next {@link #getSnapshots(String, Client)} call.
     *
     * @param portfolioId
     *            portfolio identifier
     * @param securityUuids
     *            collection of security UUIDs whose prices changed (may be empty to
     *            indicate a broad price update)
     */
    public void handlePriceUpdate(String portfolioId, Collection<String> securityUuids)
    {
        if (portfolioId == null)
            return;

        SnapshotCacheEntry entry = cache.get(portfolioId);
        if (entry != null)
        {
            entry.markPricesDirty(securityUuids);
        }
    }

    /**
     * Clears the cached snapshots for the given portfolio. Used when structural
     * changes (transactions, securities, configuration) occur.
     *
     * @param portfolioId
     *            portfolio identifier
     */
    public void handlePortfolioUpdate(String portfolioId)
    {
        if (portfolioId == null)
            return;

        cache.remove(portfolioId);
        logger.debug("Cleared snapshot cache for portfolio {}", portfolioId); //$NON-NLS-1$
    }

    /**
     * Clears all cached snapshot data.
     */
    public void clearAll()
    {
        cache.clear();
        logger.info("Cleared all cached security performance snapshots"); //$NON-NLS-1$
    }

    /**
     * Bundle wrapper containing the snapshots required by controllers.
     */
    public static final class SecurityPerformanceSnapshotBundle
    {
        private final SecurityPerformanceSnapshot allTime;
        private final SecurityPerformanceSnapshot yearToDate;
        private final SecurityPerformanceSnapshot daily;

        private SecurityPerformanceSnapshotBundle(SecurityPerformanceSnapshot allTime,
                        SecurityPerformanceSnapshot yearToDate, SecurityPerformanceSnapshot daily)
        {
            this.allTime = allTime;
            this.yearToDate = yearToDate;
            this.daily = daily;
        }

        public SecurityPerformanceSnapshot allTime()
        {
            return allTime;
        }

        public SecurityPerformanceSnapshot yearToDate()
        {
            return yearToDate;
        }

        public SecurityPerformanceSnapshot daily()
        {
            return daily;
        }
    }

    private static final class SnapshotCacheEntry
    {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private volatile SecurityPerformanceSnapshotBundle snapshots;
        private final Set<String> dirtySecurityIds = ConcurrentHashMap.newKeySet();

        SecurityPerformanceSnapshotBundle ensureUpToDate(String portfolioId, Client client)
        {
            logger.info("Ensuring up to date security performance snapshots for portfolio {}", portfolioId); //$NON-NLS-1$
            // Fast-path: if we already have snapshots and there are no pending price updates, just return them.
            if (snapshots != null && dirtySecurityIds.isEmpty())
            {
                logger.info("Returning cached security performance snapshots for portfolio {}", portfolioId); //$NON-NLS-1$
                return snapshots;
            }

            // A refresh is required; take the write lock so only one thread rebuilds or refreshes the snapshots.
            lock.writeLock().lock();
            try
            {
                // If the cache entry is empty, rebuild the snapshots from scratch.
                if (snapshots == null)
                {
                    logger.info("Rebuilding security performance snapshots for portfolio {}", portfolioId); //$NON-NLS-1$
                    snapshots = buildSnapshots(client);
                    dirtySecurityIds.clear();
                }
                else if (!dirtySecurityIds.isEmpty())
                {
                    logger.info("Refreshing security performance snapshots for portfolio {}", portfolioId); //$NON-NLS-1$
                    Set<String> affected = Set.copyOf(dirtySecurityIds);
                    refreshSnapshots(client, snapshots, affected);
                    dirtySecurityIds.clear();
                } else {
                    logger.info("Returning cached security performance snapshots for portfolio {}", portfolioId); //$NON-NLS-1$
                }

                return snapshots;
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }

        void markPricesDirty(Collection<String> securityUuids)
        {
            if (securityUuids == null || securityUuids.isEmpty())
                return;

            dirtySecurityIds.addAll(securityUuids);
        }
    }

    private static SecurityPerformanceSnapshotBundle buildSnapshots(Client client)
    {
        try
        {
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

            LocalDate today = LocalDate.now();
            Interval allTimeInterval = Interval.of(LocalDate.of(1900, 1, 1), today);

            LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
            Interval ytdInterval = Interval.of(yearStart.minusDays(1), today);

            Interval dailyInterval = Interval.of(today.minusDays(2), today);

            SecurityPerformanceSnapshot allTime = SecurityPerformanceSnapshot.create(client, converter, allTimeInterval);
            SecurityPerformanceSnapshot yearToDate = SecurityPerformanceSnapshot.create(client, converter,
                            ytdInterval);
            SecurityPerformanceSnapshot daily = SecurityPerformanceSnapshot.create(client, converter, dailyInterval);

            return new SecurityPerformanceSnapshotBundle(allTime, yearToDate, daily);
        }
        catch (Exception ex)
        {
            logger.error("Failed to build security performance snapshots: {}", ex.getMessage(), ex); //$NON-NLS-1$
            return new SecurityPerformanceSnapshotBundle(null, null, null);
        }
    }

    private static void refreshSnapshots(Client client, SecurityPerformanceSnapshotBundle bundle,
                    Set<String> affectedSecurityIds)
    {
        for (Security security : client.getSecurities())
        {
            if (!affectedSecurityIds.contains(security.getUUID()))
                continue;

            logger.info("Refreshing security performance snapshots for security {}", security.getName()); //$NON-NLS-1$
            refreshRecord(bundle.allTime, security);
            refreshRecord(bundle.yearToDate, security);
            refreshRecord(bundle.daily, security);
        }
    }

    private static void refreshRecord(SecurityPerformanceSnapshot snapshot, Security security)
    {
        snapshot.getRecord(security).ifPresent(record -> {
            updateValuationAtEndPositions(record, security);
            record.calculate();
        });
    }

    private static void updateValuationAtEndPositions(SecurityPerformanceRecord record, Security security)
    {
        var lineItems = record.getLineItems();

        for (int index = 0; index < lineItems.size(); index++)
        {
            CalculationLineItem item = lineItems.get(index);

            if (!(item instanceof CalculationLineItem.ValuationAtEnd valuation))
                continue;

            Optional<SecurityPosition> positionOpt = valuation.getSecurityPosition();
            if (positionOpt.isEmpty())
                continue;

            LocalDateTime timestamp = valuation.getDateTime();
            SecurityPrice priceAtDate = security.getSecurityPrice(timestamp.toLocalDate());
            if (priceAtDate == null)
                continue;

            SecurityPosition updatedPosition = positionOpt.get().withPrice(priceAtDate);
            lineItems.set(index, CalculationLineItem.atEnd(portfolio, updatedPosition, timestamp));
        }
    }
}
