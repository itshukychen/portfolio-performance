package name.abuchen.portfolio.ui.api.service;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.EODHistoricalDataQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.LeewayQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataSearchProvider;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Service to initialize API keys for quote feeds from Eclipse preferences.
 * 
 * This is needed when running UpdateQuotesJob from REST API context where
 * the E4 dependency injection (Preference2EnvAddon) may not have been triggered.
 */
public class QuoteFeedApiKeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuoteFeedApiKeyService.class);
    
    /**
     * Initialize all quote feed API keys from preferences.
     * This method reads API keys from Eclipse preferences and sets them
     * on the corresponding quote feed providers.
     */
    public static void initializeApiKeys() {
        IPreferencesService preferencesService = Platform.getPreferencesService();
        
        // Log available feeds for debugging
        logger.info("Initializing API keys for quote feeds");
        logger.info("Available quote feeds: {}", 
            Factory.getQuoteFeedProvider().stream()
                .map(QuoteFeed::getId)
                .collect(java.util.stream.Collectors.joining(", ")));
        
        // TwelveData
        try {
            String twelvedataApiKey = preferencesService.getString(
                PortfolioPlugin.PLUGIN_ID, 
                UIConstants.Preferences.TWELVEDATA_API_KEY, 
                "", 
                null
            );
            Factory.getQuoteFeed(TwelveDataQuoteFeed.class).setApiKey(twelvedataApiKey);;
            Factory.getSearchProvider(TwelveDataSearchProvider.class).setApiKey(twelvedataApiKey);
            logger.info("Initialized API keys for TwelveData: {}", twelvedataApiKey);
        } catch (Exception e) {
            logger.error("Failed to initialize TwelveData feed. Available feeds: {}", 
                Factory.getQuoteFeedProvider().stream()
                    .map(f -> f.getClass().getSimpleName())
                    .collect(java.util.stream.Collectors.joining(", ")), 
                e);
            throw e; // Re-throw to surface the issue
        }
    }
}

