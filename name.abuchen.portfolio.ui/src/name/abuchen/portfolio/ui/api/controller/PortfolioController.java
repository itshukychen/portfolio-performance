package name.abuchen.portfolio.ui.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.service.QuoteFeedApiKeyService;

/**
 * REST Controller for portfolio file operations.
 * 
 * This controller provides endpoints to list, open, and manage portfolio files.
 * Specialized operations for securities, accounts, dashboards, widgets, earnings, 
 * and options have been moved to their respective controllers.
 */
@Path("/api/v1/portfolios")
public class PortfolioController extends BaseController {
    
    /**
     * List all portfolios in the portfolio directory.
     * 
     * @return List of portfolios
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPortfolios() {
        try {
            logger.info("Listing portfolios");
            
            List<name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo> files = portfolioFileService.listPortfolioFiles();
            
            return Response.ok(files).build();
            
        } catch (IOException e) {
            logger.error("Failed to list portfolios", e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "INTERNAL_ERROR", 
                "Failed to list portfolios: " + e.getMessage());
        }
    }
    
    
    
    
    
    /**
     * Health check endpoint for file operations.
     * 
     * @return Health status
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "PortfolioFileService");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        health.put("timezone", ZoneId.systemDefault().getId());
        health.put("cacheStats", portfolioFileService.getCacheStats());
        
        return Response.ok(health).build();
    }
    
    // ===== RESTful ID-based endpoints =====
    
    /**
     * Open a portfolio by its ID.
     * 
     * @param portfolioId The portfolio ID
     * @param password Optional password for encrypted portfolios
     * @param allowCache Whether to allow using cached version (default: true)
     * @return Portfolio information
     */
    @GET
    @Path("/{portfolioId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortfolioById(@PathParam("portfolioId") String portfolioId, 
                                   @QueryParam("password") String password,
                                   @QueryParam("allowCache") Boolean allowCache) {
        try {
            // Default allowCache to true if not specified
            boolean useCache = allowCache == null || allowCache;
            
            logger.info("Opening portfolio by ID: " + portfolioId + " (allowCache: " + useCache + ", cache size: " + portfolioFileService.getCacheStats().get("cachedClients") + ")");
            
            PortfolioFileInfo fileInfo;
            
            // If cache is allowed, try to get from cache first
            if (useCache) {
                Client client = portfolioFileService.getPortfolio(portfolioId);
                if (client != null) {
                    logger.info("Using cached portfolio for ID: " + portfolioId);
                    fileInfo = portfolioFileService.getFullPortfolioInfo(portfolioId);
                    return Response.ok(fileInfo).build();
                }
                logger.info("Portfolio not in cache, loading from disk");
            } else {
                logger.info("Cache disabled, forcing reload from disk");
            }
            
            // Cache miss or cache disabled - load from disk
            char[] passwordChars = null;
            if (password != null && !password.trim().isEmpty()) {
                passwordChars = password.toCharArray();
            }
            
            fileInfo = portfolioFileService.openFileById(
                portfolioId,
                passwordChars
            );
            
            logger.info("Portfolio loaded successfully (cache size after: " + portfolioFileService.getCacheStats().get("cachedClients") + ")");
            
            return Response.ok(fileInfo).build();
            
        } catch (FileNotFoundException e) {
            logger.error("Portfolio not found: " + portfolioId + " - " + e.getMessage());
            return createErrorResponse(Response.Status.NOT_FOUND, 
                "PORTFOLIO_NOT_FOUND", 
                e.getMessage());
                
        } catch (IOException e) {
            logger.error("Failed to get portfolio info: " + portfolioId + " - " + e.getMessage());
            return createErrorResponse(Response.Status.BAD_REQUEST, 
                "INVALID_REQUEST", 
                e.getMessage());
                
        } catch (Exception e) {
            logger.error("Unexpected error getting portfolio info: " + portfolioId + " - " + e.getMessage());
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "INTERNAL_ERROR", 
                e.getMessage());
        }
    }
    
    /**
     * Update prices and exchange rates for the portfolio.
     * 
     * This endpoint triggers a price update job that fetches both latest and historic quotes
     * for all securities in the portfolio that are not marked as retired. After updating 
     * security prices, it also updates exchange rates from all registered exchange rate 
     * providers, similar to the scheduled exchange rate update service.
     * 
     * @param portfolioId The portfolio ID
     * @return Response with updated portfolio information
     */
    @POST
    @Path("/{portfolioId}/updatePrices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePrices(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Manual price update requested for portfolio: " + portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before updating prices");
            }
            
            // Initialize API keys from preferences before running the update job
            // This is necessary because the E4 dependency injection (Preference2EnvAddon)
            // may not have been triggered in the REST API context
            logger.info("Initializing API keys from preferences");
            QuoteFeedApiKeyService.initializeApiKeys();
            
            // Use the shared update logic from ScheduledPriceUpdateService
            // This updates exchange rates, prices, and sets lastPriceUpdateTime
            priceUpdateService.updatePortfolioPricesAndExchangeRates(portfolioId);
            
            logger.info("Price and exchange rate update completed successfully");
            
            // Get the updated portfolio info with full client data from cache
            PortfolioFileInfo fileInfo = portfolioFileService.getFullPortfolioInfo(portfolioId);
            
            return Response.ok(fileInfo).build();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Price update job was interrupted for portfolio " + portfolioId + ": " + e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Price update interrupted", 
                e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating prices for portfolio " + portfolioId + ": " + e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get the last price update timestamp for a portfolio.
     * 
     * This endpoint returns the timestamp of the last time prices were updated,
     * either through the scheduled update service or manual update.
     * 
     * @param portfolioId The portfolio ID
     * @return Response containing the last price update timestamp
     */
    @GET
    @Path("/{portfolioId}/lastPriceUpdateTime")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastPriceUpdateTime(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.debug("Getting last price update time for portfolio: " + portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing last price update time");
            }
            
            // Get the lastPriceUpdateTime property
            String lastUpdateTime = client.getProperty("lastPriceUpdateTime");
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            
            if (lastUpdateTime != null && !lastUpdateTime.isEmpty()) {
                response.put("lastPriceUpdateTime", lastUpdateTime);
                response.put("hasBeenUpdated", true);
            } else {
                response.put("lastPriceUpdateTime", null);
                response.put("hasBeenUpdated", false);
                response.put("message", "Prices have not been updated yet");
            }
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting last price update time for portfolio " + portfolioId + ": " + e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get the total portfolio value (current valuation).
     * 
     * This endpoint returns the current total value of the portfolio,
     * calculated as the sum of all account balances and security positions.
     * 
     * @param portfolioId The portfolio ID
     * @return Response containing the total portfolio value
     */
    @GET
    @Path("/{portfolioId}/totalValue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTotalValue(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting total value for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing total value");
            }
            
            // Create currency converter
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            java.time.LocalDate today = java.time.LocalDate.now();
            
            // Create client snapshot to get total value
            ClientSnapshot snapshot = ClientSnapshot.create(client, converter, today);
            Money totalValue = snapshot.getMonetaryAssets();
            
            // Convert from internal representation (multiplied by 100) to decimal
            double totalValueDecimal = totalValue.getAmount() / Values.Money.factor();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("totalValue", totalValueDecimal);
            response.put("currencyCode", totalValue.getCurrencyCode());
            response.put("date", today.toString());
            
            logger.info("Total value for portfolio {}: {} {}", portfolioId, totalValueDecimal, totalValue.getCurrencyCode());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting total value for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
}
