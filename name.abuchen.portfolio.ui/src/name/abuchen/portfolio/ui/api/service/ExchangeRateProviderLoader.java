package name.abuchen.portfolio.ui.api.service;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * Utility class to ensure exchange rate providers are loaded from their cache files.
 * This is a singleton pattern that loads providers once on first access.
 * The providers themselves are global singletons (loaded via ServiceLoader),
 * so loading them once makes the data available everywhere in the application.
 */
public class ExchangeRateProviderLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateProviderLoader.class);
    
    private static volatile boolean loaded = false;
    private static final Object lock = new Object();
    
    // Private constructor to prevent instantiation
    private ExchangeRateProviderLoader() {}
    
    /**
     * Ensures that exchange rate providers are loaded from their cache files.
     * This method is thread-safe and will only load the providers once.
     * Subsequent calls will return immediately.
     * 
     * Call this method before using exchange rates to ensure cached data is loaded
     * instead of falling back to default values from 2015.
     */
    public static void ensureLoaded() {
        String threadName = Thread.currentThread().getName();
        
        if (loaded) {
            logger.debug("Exchange rate providers already loaded - skipping (thread: {})", threadName);
            return;
        }
        
        synchronized (lock) {
            if (loaded) {
                logger.debug("Exchange rate providers already loaded (double-check inside lock) - skipping (thread: {})", threadName);
                return;
            }
            
            try {
                logger.info("💾 Loading exchange rate providers from cache... (thread: {})", threadName);
                
                List<ExchangeRateProvider> providers = ExchangeRateProviderFactory.getProviders();
                
                if (providers.isEmpty()) {
                    logger.warn("No exchange rate providers found");
                    loaded = true;
                    return;
                }
                
                logger.info("Found {} exchange rate provider(s)", providers.size());
                
                NullProgressMonitor monitor = new NullProgressMonitor();
                int loadedCount = 0;
                
            for (ExchangeRateProvider provider : providers) {
                try {
                    logger.info("   📂 Loading cached data for: {} [@{}]", 
                                provider.getName(), 
                                Integer.toHexString(System.identityHashCode(provider)));
                    provider.load(monitor);
                    logger.info("   ✅ Loaded: {} [@{}]", 
                                provider.getName(),
                                Integer.toHexString(System.identityHashCode(provider)));
                    loadedCount++;
                } catch (IOException e) {
                    logger.warn("   ⚠️  Failed to load cached data for {} [@{}]: {}", 
                                provider.getName(),
                                Integer.toHexString(System.identityHashCode(provider)),
                                e.getMessage());
                    logger.info("   ℹ️  Provider {} [@{}] will use default fallback data until first update", 
                                provider.getName(),
                                Integer.toHexString(System.identityHashCode(provider)));
                }
            }
                
                logger.info("Exchange rate providers loaded: {} succeeded (thread: {})", loadedCount, threadName);
                loaded = true;
                logger.info("✅ Static 'loaded' flag set to true - visible to ALL threads in JVM");
                
            } catch (Exception e) {
                logger.error("Error loading exchange rate providers", e);
                loaded = true; // Set to true anyway to avoid retrying repeatedly
            }
        }
    }
    
    /**
     * Check if exchange rate providers have been loaded.
     * 
     * @return true if providers have been loaded, false otherwise
     */
    public static boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Reset the loaded state. Only use for testing purposes.
     */
    static void reset() {
        synchronized (lock) {
            loaded = false;
        }
    }
}

