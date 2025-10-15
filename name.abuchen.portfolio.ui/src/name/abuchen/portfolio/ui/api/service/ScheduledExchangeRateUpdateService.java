package name.abuchen.portfolio.ui.api.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * Service that automatically updates exchange rates on a scheduled basis.
 * This service runs every 10 minutes and updates exchange rates from online sources
 * for all registered exchange rate providers.
 */
public class ScheduledExchangeRateUpdateService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledExchangeRateUpdateService.class);
    
    // Update interval in minutes
    private static final int UPDATE_INTERVAL_MINUTES = 10;
    
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    /**
     * Constructor that initializes the scheduler.
     */
    public ScheduledExchangeRateUpdateService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ScheduledExchangeRateUpdate");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Start the scheduled exchange rate update service.
     * The service will update exchange rates every 10 minutes.
     */
    public void start() {
        if (running) {
            logger.warn("Scheduled exchange rate update service is already running");
            return;
        }
        
        running = true;
        logger.info("🕐 Starting scheduled exchange rate update service (interval: {} minutes)", UPDATE_INTERVAL_MINUTES);
        
        // Schedule the update task to run every 10 minutes with an initial delay of 10 minutes
        scheduler.scheduleAtFixedRate(
            this::updateExchangeRates,
            UPDATE_INTERVAL_MINUTES,
            UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("✅ Scheduled exchange rate update service started successfully");
    }
    
    /**
     * Stop the scheduled exchange rate update service.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        logger.info("🛑 Stopping scheduled exchange rate update service...");
        running = false;
        
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate within 30 seconds, forcing shutdown");
                scheduler.shutdownNow();
            }
            logger.info("✅ Scheduled exchange rate update service stopped");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for scheduler to terminate", e);
            scheduler.shutdownNow();
        }
    }
    
    /**
     * Update exchange rates from online sources.
     * This method is called periodically by the scheduler.
     */
    private void updateExchangeRates() {
        try {
            logger.info("========================================");
            logger.info("💱 Starting scheduled exchange rate update");
            logger.info("========================================");
            
            List<ExchangeRateProvider> providers = ExchangeRateProviderFactory.getProviders();
            
            if (providers.isEmpty()) {
                logger.warn("No exchange rate providers found, skipping update");
                logger.info("========================================");
                return;
            }
            
            logger.info("Found {} exchange rate provider(s)", providers.size());
            
            NullProgressMonitor monitor = new NullProgressMonitor();
            int successCount = 0;
            int failureCount = 0;
            
            for (ExchangeRateProvider provider : providers) {
                try {
                    logger.info("🌐 Updating exchange rates from: {}", provider.getName());
                    provider.update(monitor);
                    logger.info("   ✅ Updated successfully");
                    
                    // Save the updated data to cache file
                    logger.info("   💾 Saving updated rates to cache...");
                    provider.save(monitor);
                    logger.info("   ✅ Saved to cache file");
                    
                    successCount++;
                } catch (IOException e) {
                    logger.error("   ⚠️  Failed to update {}: {}", provider.getName(), e.getMessage());
                    failureCount++;
                }
            }
            
            logger.info("========================================");
            logger.info("✅ Scheduled exchange rate update completed");
            logger.info("   Providers updated: {}", successCount);
            if (failureCount > 0) {
                logger.info("   Providers failed: {}", failureCount);
            }
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Error in scheduled exchange rate update task", e);
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

