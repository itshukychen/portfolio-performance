package name.abuchen.portfolio.ui.api.service;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;

/**
 * Service that automatically updates prices and exchange rates on a scheduled basis.
 * This service runs every 10 minutes and performs the following operations:
 * 1. Updates exchange rates from online sources
 * 2. Updates both latest and historic quotes for all active (non-retired) securities in cached portfolios
 * 3. Sets the lastPriceUpdateTime property on all updated portfolios
 */
public class ScheduledPriceUpdateService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledPriceUpdateService.class);
    
    // Update interval in minutes
    private static final int UPDATE_INTERVAL_MINUTES = 10;
    
    private final PortfolioFileService portfolioFileService;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    /**
     * Constructor with portfolio file service dependency.
     * 
     * @param portfolioFileService The portfolio file service to use for accessing cached portfolios
     */
    public ScheduledPriceUpdateService(PortfolioFileService portfolioFileService) {
        this.portfolioFileService = portfolioFileService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ScheduledPriceUpdate");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Start the scheduled price and exchange rate update service.
     * The service will update exchange rates and prices every 10 minutes for all cached portfolios.
     */
    public void start() {
        if (running) {
            logger.warn("Scheduled price update service is already running");
            return;
        }
        
        running = true;
        logger.info("🕐 Starting scheduled price and exchange rate update service (interval: {} minutes)", UPDATE_INTERVAL_MINUTES);
        
        // Load exchange rate providers from cache first (one-time operation)
        ExchangeRateProviderLoader.ensureLoaded();
        
        // Schedule the update task to run every 10 minutes with an initial delay of 10 minutes
        scheduler.scheduleAtFixedRate(
            this::updateAllPortfolioPrices,
            UPDATE_INTERVAL_MINUTES,
            UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("✅ Scheduled price and exchange rate update service started successfully");
    }
    
    /**
     * Stop the scheduled price update service.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        logger.info("🛑 Stopping scheduled price update service...");
        running = false;
        
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate within 30 seconds, forcing shutdown");
                scheduler.shutdownNow();
            }
            logger.info("✅ Scheduled price update service stopped");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for scheduler to terminate", e);
            scheduler.shutdownNow();
        }
    }
    
    /**
     * Update exchange rates and prices for all portfolios currently in the cache.
     * This method is called periodically by the scheduler.
     */
    private void updateAllPortfolioPrices() {
        try {
            logger.info("========================================");
            logger.info("📊 Starting scheduled update (exchange rates + prices)");
            logger.info("========================================");
            
            // Step 1: Update exchange rates first (global operation)
            logger.info("Step 1/2: Updating exchange rates...");
            updateExchangeRates();
            
            // Step 2: Update prices for all cached portfolios
            logger.info("Step 2/2: Updating security prices...");
            
            // Get the IDs of all cached portfolios
            Set<String> portfolioIds = portfolioFileService.getCachedPortfolioIds();
            
            if (portfolioIds.isEmpty()) {
                logger.info("No portfolios currently loaded in cache, skipping price update");
                logger.info("========================================");
                return;
            }
            
            logger.info("Found {} portfolio(s) in cache", portfolioIds.size());
            
            // Update each portfolio
            int successCount = 0;
            int failureCount = 0;
            
            for (String portfolioId : portfolioIds) {
                try {
                    updatePortfolioPrices(portfolioId);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to update prices for portfolio {}: {}", portfolioId, e.getMessage(), e);
                    failureCount++;
                }
            }
            
            logger.info("========================================");
            logger.info("✅ Scheduled update completed");
            logger.info("   Portfolios updated: {}", successCount);
            if (failureCount > 0) {
                logger.info("   Portfolios failed: {}", failureCount);
            }
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Error in scheduled update task", e);
        }
    }
    
    /**
     * Update exchange rates and prices for a specific portfolio.
     * This is a public method that can be called by the REST API or scheduled tasks.
     * 
     * @param portfolioId The portfolio ID
     * @throws Exception if the update fails
     */
    public void updatePortfolioPricesAndExchangeRates(String portfolioId) throws Exception {
        logger.info("Updating exchange rates and prices for portfolio: {}", portfolioId);
        
        // Step 1: Update exchange rates first (global operation)
        updateExchangeRates();
        
        // Step 2: Update portfolio prices
        updatePortfolioPrices(portfolioId);
    }
    
    /**
     * Update prices for a specific portfolio.
     * 
     * @param portfolioId The portfolio ID
     * @throws Exception if the update fails
     */
    private void updatePortfolioPrices(String portfolioId) throws Exception {
        logger.info("📈 Updating prices for portfolio: {}", portfolioId);
        
        // Get the cached Client for this portfolio
        Client client = portfolioFileService.getPortfolio(portfolioId);
        
        if (client == null) {
            logger.warn("Portfolio {} is no longer in cache, skipping", portfolioId);
            return;
        }
        
        // Create predicate to filter only active (non-retired) securities
        // Same logic as in PortfolioController.updatePrices()
        Predicate<Security> onlyActive = s -> !s.isRetired()
                 && (s.getTickerSymbol() == null || !s.getTickerSymbol().replaceAll("\\s+", "").matches(".*\\d{6}[CP]\\d{8}"));
        
        // Count active securities
        long activeSecurities = client.getSecurities().stream()
                .filter(onlyActive)
                .count();
        
        logger.info("   Found {} active securities to update", activeSecurities);
        
        if (activeSecurities == 0) {
            logger.info("   No active securities to update, skipping");
            return;
        }
        
        // Create and schedule the update quotes job with both LATEST and HISTORIC targets
        Job updateJob = new UpdatePricesJob(client, onlyActive,
                        EnumSet.of(UpdatePricesJob.Target.LATEST, UpdatePricesJob.Target.HISTORIC));
        updateJob.schedule();
        
        // Wait for the job to complete
        updateJob.join();
        
        logger.info("   Price update job completed. Saving portfolio file...");
        
        // Set the lastPriceUpdateTime property on the client
        String updateTimestamp = Instant.now().toString();
        client.setProperty("lastPriceUpdateTime", updateTimestamp);
        logger.info("   Set lastPriceUpdateTime to: {}", updateTimestamp);
        
        // Save the portfolio file after the update
        portfolioFileService.saveFile(portfolioId);
        
        logger.info("   ✅ Portfolio file saved successfully");
    }
    
    /**
     * Update exchange rates from online sources.
     * This updates exchange rates globally before updating portfolio prices.
     */
    private void updateExchangeRates() {
        try {
            logger.info("💱 Updating exchange rates from online sources...");
            
            List<ExchangeRateProvider> providers = ExchangeRateProviderFactory.getProviders();
            
            if (providers.isEmpty()) {
                logger.warn("No exchange rate providers found, skipping exchange rate update");
                return;
            }
            
            logger.info("Found {} exchange rate provider(s)", providers.size());
            
            NullProgressMonitor monitor = new NullProgressMonitor();
            int successCount = 0;
            int failureCount = 0;
            
            for (ExchangeRateProvider provider : providers) {
                try {
                    logger.info("   🌐 Updating from: {} [@{}]", 
                                provider.getName(),
                                Integer.toHexString(System.identityHashCode(provider)));
                    provider.update(monitor);
                    
                    // Save the updated data to cache file
                    provider.save(monitor);
                    logger.info("   ✅ Updated and saved: {} [@{}]", 
                                provider.getName(),
                                Integer.toHexString(System.identityHashCode(provider)));
                    
                    successCount++;
                } catch (IOException e) {
                    logger.error("   ⚠️  Failed to update {} [@{}]: {}", 
                                 provider.getName(),
                                 Integer.toHexString(System.identityHashCode(provider)),
                                 e.getMessage());
                    failureCount++;
                }
            }
            
            logger.info("Exchange rate update completed: {} succeeded, {} failed", successCount, failureCount);
            
        } catch (Exception e) {
            logger.error("Error updating exchange rates: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if the service is running.
     * 
     * @return true if the service is running
     */
    public boolean isRunning() {
        return running;
    }
}

