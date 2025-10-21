package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.api.dto.SecurityDto;
import name.abuchen.portfolio.util.Interval;

/**
 * REST Controller for security operations.
 * 
 * This controller provides endpoints to manage securities within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/securities")
public class SecurityController extends BaseController {
    
    // Cache for performance snapshots per portfolio
    // Key: portfolioId, Value: SecurityPerformanceSnapshots
    private static final Map<String, SecurityPerformanceSnapshots> SNAPSHOTS_CACHE = new ConcurrentHashMap<>();
    
    // Cache for last update time to invalidate cache when portfolio changes
    private static final Map<String, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    
    /**
     * Get all active securities in a portfolio.
     * 
     * This endpoint filters out:
     * - Retired securities
     * - Options contracts (ticker symbols matching pattern: 6 digits + C/P + 8 digits)
     * 
     * @param portfolioId The portfolio ID
     * @return List of active securities (excluding retired and options contracts)
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSecurities(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting all securities for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing securities");
            }
            
            // Get or create cached performance snapshots for this portfolio
            SecurityPerformanceSnapshots snapshots = getOrCreatePerformanceSnapshots(portfolioId, client);
            
            // Get all securities (filter out retired securities and options contracts)
            List<SecurityDto> securities = client.getSecurities().stream()
                .filter(security -> !security.isRetired()) // Skip retired securities
                .filter(this::isNotOptionsContract) // Skip options contracts
                .map(security -> convertSecurityToDto(security, client, snapshots))
                .collect(Collectors.toList());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("count", securities.size());
            response.put("securities", securities);
            
            logger.info("Returning {} active securities for portfolio {} (filtered out retired and options contracts)", 
                securities.size(), portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting securities for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get a specific security by UUID.
     * 
     * This endpoint filters out retired securities and options contracts.
     * If the security is retired or an options contract, a 404 will be returned.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Security details (if not retired and not an options contract)
     */
    @GET
    @Path("/{securityUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecurityById(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid) {
        try {
            logger.info("Getting security {} for portfolio {}", securityUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing securities");
            }
            
            // Find the security by UUID (excluding retired securities and options contracts)
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .filter(s -> !s.isRetired()) // Skip retired securities
                .filter(this::isNotOptionsContract) // Skip options contracts
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found or filtered out: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found (or is retired/options contract)");
            }
            
            // Get or create cached performance snapshots for this portfolio
            SecurityPerformanceSnapshots snapshots = getOrCreatePerformanceSnapshots(portfolioId, client);
            
            // Convert to DTO
            SecurityDto securityDto = convertSecurityToDto(security, client, snapshots);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("security", securityDto);
            
            logger.info("Returning security {} ({}) for portfolio {}", 
                security.getName(), securityUuid, portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting security {} for portfolio {}: {}", 
                securityUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new security.
     * TODO: Implement security creation
     * 
     * @param portfolioId The portfolio ID
     * @param securityData Security data
     * @return Created security
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSecurity(@PathParam("portfolioId") String portfolioId,
                                   Map<String, Object> securityData) {
        // TODO: Implement security creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security creation not yet implemented");
    }
    
    /**
     * Update an existing security.
     * TODO: Implement security update
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @param securityData Updated security data
     * @return Updated security
     */
    @PUT
    @Path("/{securityUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecurity(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid,
                                   Map<String, Object> securityData) {
        // TODO: Implement security update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security update not yet implemented");
    }
    
    /**
     * Delete a security.
     * TODO: Implement security deletion
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{securityUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSecurity(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid) {
        // TODO: Implement security deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security deletion not yet implemented");
    }
    
    /**
     * Helper method to check if a security is NOT an options contract.
     * Options contracts have ticker symbols matching pattern: 6 digits + C/P + 8 digits
     * Aligned with PortfolioFileService.loadSecurities()
     */
    private boolean isNotOptionsContract(Security security) {
        String tickerSymbol = security.getTickerSymbol();
        if (tickerSymbol != null && tickerSymbol.replaceAll("\\s+", "").matches(".*\\d{6}[CP]\\d{8}")) {
            return false; // This is an options contract, skip it
        }
        return true; // Not an options contract
    }
    
    /**
     * Helper class to hold the three performance snapshots we need
     */
    private static class SecurityPerformanceSnapshots {
        final SecurityPerformanceSnapshot allTime;
        final SecurityPerformanceSnapshot ytd;
        final SecurityPerformanceSnapshot daily;
        
        SecurityPerformanceSnapshots(SecurityPerformanceSnapshot allTime, 
                                    SecurityPerformanceSnapshot ytd, 
                                    SecurityPerformanceSnapshot daily) {
            this.allTime = allTime;
            this.ytd = ytd;
            this.daily = daily;
        }
    }
    
    /**
     * Get or create cached performance snapshots for a portfolio.
     * This method implements caching to avoid recreating snapshots on every API call.
     * Cache is invalidated when the portfolio is updated.
     */
    private SecurityPerformanceSnapshots getOrCreatePerformanceSnapshots(String portfolioId, Client client) {
        // Check if we have cached snapshots for this portfolio
        SecurityPerformanceSnapshots cachedSnapshots = SNAPSHOTS_CACHE.get(portfolioId);
        
        // Check if cache is still valid (use a simple time-based cache with 5 minute TTL)
        Long lastCacheTime = CACHE_TIMESTAMPS.get(portfolioId);
        long currentTime = System.currentTimeMillis();
        long cacheTTL = 5 * 60 * 1000; // 5 minutes in milliseconds
        
        if (cachedSnapshots != null && lastCacheTime != null && 
            (currentTime - lastCacheTime) < cacheTTL) {
            logger.debug("Using cached performance snapshots for portfolio: {}", portfolioId);
            return cachedSnapshots;
        }
        
        // Create new snapshots and cache them
        logger.info("Creating new performance snapshots for portfolio: {}", portfolioId);
        SecurityPerformanceSnapshots snapshots = createPerformanceSnapshots(client);
        
        // Cache the snapshots
        SNAPSHOTS_CACHE.put(portfolioId, snapshots);
        CACHE_TIMESTAMPS.put(portfolioId, currentTime);
        
        return snapshots;
    }
    
    /**
     * Clear the performance snapshots cache for a specific portfolio.
     * This should be called when the portfolio is updated.
     */
    public static void clearCache(String portfolioId) {
        SNAPSHOTS_CACHE.remove(portfolioId);
        CACHE_TIMESTAMPS.remove(portfolioId);
        logger.info("Cleared performance snapshots cache for portfolio: {}", portfolioId);
    }
    
    /**
     * Clear all performance snapshots cache.
     * This can be called during application shutdown or when memory is needed.
     */
    public static void clearAllCache() {
        SNAPSHOTS_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
        logger.info("Cleared all performance snapshots cache");
    }
    
    /**
     * Create performance snapshots for a client.
     * This is called when cache is invalid or doesn't exist.
     */
    private SecurityPerformanceSnapshots createPerformanceSnapshots(Client client) {
        try {
            // Create currency converter for the client
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            LocalDate today = LocalDate.now();
            
            // Create interval for all time (to get current holdings)
            LocalDate startDate = LocalDate.of(1900, 1, 1);
            Interval allTimeInterval = Interval.of(startDate, today);
            
            // Create interval for YTD
            LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
            Interval ytdInterval = Interval.of(yearStart.minusDays(1), today);
            
            // Create interval for daily
            LocalDate yesterday = today.minusDays(1);
            Interval dailyInterval = Interval.of(yesterday.minusDays(1), today);
            
            // Create all three snapshots once
            SecurityPerformanceSnapshot allTimeSnapshot = SecurityPerformanceSnapshot.create(
                client, converter, allTimeInterval);
            SecurityPerformanceSnapshot ytdSnapshot = SecurityPerformanceSnapshot.create(
                client, converter, ytdInterval);
            SecurityPerformanceSnapshot dailySnapshot = SecurityPerformanceSnapshot.create(
                client, converter, dailyInterval);
            
            // Return the snapshots wrapper
            return new SecurityPerformanceSnapshots(
                allTimeSnapshot, ytdSnapshot, dailySnapshot);
            
        } catch (Exception e) {
            logger.error("Failed to create performance snapshots: {}", e.getMessage(), e);
            // Return empty snapshots on error
            return new SecurityPerformanceSnapshots(null, null, null);
        }
    }
    
    /**
     * Helper method to convert Security to SecurityDto.
     * Aligned with PortfolioFileService.loadSecurities()
     */
    private SecurityDto convertSecurityToDto(Security security, Client client, 
                                            SecurityPerformanceSnapshots snapshots) {
        SecurityDto dto = new SecurityDto();
        dto.setUuid(security.getUUID());
        dto.setName(security.getName());
        dto.setCurrencyCode(security.getCurrencyCode());
        dto.setTargetCurrencyCode(security.getTargetCurrencyCode());
        dto.setIsin(security.getIsin());
        dto.setTickerSymbol(security.getTickerSymbol());
        dto.setWkn(security.getWkn());
        dto.setNote(security.getNote());
        dto.setRetired(security.isRetired());
        dto.setFeed(security.getFeed());
        dto.setFeedURL(security.getFeedURL());
        dto.setLatestFeed(security.getLatestFeed());
        dto.setLatestFeedURL(security.getLatestFeedURL());
        dto.setPricesCount(security.getPrices().size());
        dto.setUpdatedAt(security.getUpdatedAt());
        
        // Calculate holdings data using pre-created snapshots
        calculateHoldingsData(dto, security, client, snapshots);
        
        return dto;
    }
    
    /**
     * Helper method to calculate and populate holdings data for a security.
     * Uses pre-created performance snapshots for efficiency.
     * Calculates:
     * - Shares held
     * - Average price per share
     * - Total holding value in security currency and base currency
     * - Unrealized gains for YTD and daily
     * - Total earnings (dividends)
     */
    private void calculateHoldingsData(SecurityDto dto, Security security, Client client, 
                                      SecurityPerformanceSnapshots snapshots) {
        try {
            // Check if snapshots are available
            if (snapshots == null || snapshots.allTime == null) {
                logger.warn("No performance snapshots available for security: {}", security.getName());
                return;
            }
            
            // Create currency converter for the client
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            LocalDate today = LocalDate.now();
            
            // Get all-time performance record
            Optional<SecurityPerformanceRecord> recordOpt = snapshots.allTime.getRecord(security);
            
            if (recordOpt.isPresent()) {
                SecurityPerformanceRecord record = recordOpt.get();
                
                // Set shares held (convert from internal representation)
                long sharesHeld = record.getSharesHeld();
                dto.setSharesHeld(sharesHeld / Values.Share.factor());
                
                // Set average price per share (FIFO cost per share)
                Quote avgPrice = record.getFifoCostPerSharesHeld();
                if (avgPrice != null && sharesHeld > 0) {
                    // Convert from internal representation (Value.Quote) to actual price
                    dto.setAvgPricePerShare(avgPrice.getAmount() / (double) Values.Quote.factor());
                }
                
                // Set total holding value
                // Market value from record is in base currency
                Money marketValueInBase = record.getMarketValue();
                if (marketValueInBase != null) {
                    dto.setTotalHoldingValueBaseCurrency(marketValueInBase.getAmount() / Values.Money.factor());
                    
                    // Convert back to security currency
                    CurrencyConverter reverseConverter = new CurrencyConverterImpl(factory, security.getCurrencyCode());
                    Money marketValueInSecurityCurrency = reverseConverter.convert(today, marketValueInBase);
                    dto.setTotalHoldingValueSecurityCurrency(marketValueInSecurityCurrency.getAmount() / Values.Money.factor());
                }
                
                // Set total earnings (dividends)
                Money sumOfDividends = record.getSumOfDividends();
                if (sumOfDividends != null) {
                    // Convert to base currency
                    Money dividendsInBase = converter.convert(today, sumOfDividends);
                    dto.setTotalEarnings(dividendsInBase.getAmount() / Values.Money.factor());
                }
            }
            
            // Get YTD performance record
            Optional<SecurityPerformanceRecord> ytdRecordOpt = snapshots.ytd.getRecord(security);
            
            if (ytdRecordOpt.isPresent()) {
                SecurityPerformanceRecord ytdRecord = ytdRecordOpt.get();
                Money capitalGains = ytdRecord.getCapitalGainsOnHoldings();
                if (capitalGains != null) {
                    Money gainsInBase = converter.convert(today, capitalGains);
                    dto.setUnrealizedGainsYTD(gainsInBase.getAmount() / Values.Money.factor());
                }
            }
            
            // Get daily performance record
            Optional<SecurityPerformanceRecord> dailyRecordOpt = snapshots.daily.getRecord(security);
            
            if (dailyRecordOpt.isPresent()) {
                SecurityPerformanceRecord dailyRecord = dailyRecordOpt.get();
                Money capitalGains = dailyRecord.getCapitalGainsOnHoldings();
                if (capitalGains != null) {
                    Money gainsInBase = converter.convert(today, capitalGains);
                    dto.setUnrealizedGainsDaily(gainsInBase.getAmount() / Values.Money.factor());
                }
            }
            
        } catch (Exception e) {
            // Log error but don't fail the entire DTO conversion
            logger.warn("Failed to calculate holdings data for security {}: {}", 
                security.getName(), e.getMessage());
            // Leave holdings fields as null to indicate unavailable data
        }
    }
}

