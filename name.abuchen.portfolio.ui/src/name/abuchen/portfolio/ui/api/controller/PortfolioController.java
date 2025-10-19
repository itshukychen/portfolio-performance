package name.abuchen.portfolio.ui.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.ui.api.dto.EarningsTransactionDto;
import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.dto.SecurityPriceDto;
import name.abuchen.portfolio.ui.api.dto.ValueDataPointDto;
import name.abuchen.portfolio.ui.api.service.PortfolioFileService;
import name.abuchen.portfolio.ui.api.service.QuoteFeedApiKeyService;
import name.abuchen.portfolio.ui.api.service.ScheduledExchangeRateUpdateService;
import name.abuchen.portfolio.ui.api.service.WidgetDataService;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * REST Controller for portfolio operations.
 * 
 * This controller provides endpoints to open, load, and manage portfolios
 * using the Portfolio Performance core modules.
 */
@Path("/api/v1/portfolios")
public class PortfolioController {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);
    
    // Use static singleton to ensure cache is shared across all API calls
    private static final PortfolioFileService portfolioFileService = PortfolioFileService.getInstance();
    private static final WidgetDataService widgetDataService = new WidgetDataService();
    private static final ScheduledExchangeRateUpdateService exchangeRateUpdateService = new ScheduledExchangeRateUpdateService();
    
    /**
     * Helper method to create error responses with consistent structure.
     * 
     * @param status HTTP status code
     * @param error Error type/category
     * @param message Detailed error message
     * @return Response with error details
     */
    private Response createErrorResponse(Response.Status status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return Response.status(status).entity(errorResponse).build();
    }
    
    /**
     * Helper method to create 428 Precondition Required responses.
     * Used when a portfolio must be opened first before accessing a resource.
     * 
     * @param error Error type/category
     * @param message Detailed error message
     * @return Response with 428 status and error details
     */
    private Response createPreconditionRequiredResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return Response.status(428).entity(errorResponse).build();
    }
    
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
     * @return Portfolio information
     */
    @GET
    @Path("/{portfolioId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortfolioById(@PathParam("portfolioId") String portfolioId, 
                                   @QueryParam("password") String password) {
        try {
            logger.info("Opening portfolio by ID: " + portfolioId + " (cache size before: " + portfolioFileService.getCacheStats().get("cachedClients") + ")");
            
            // Convert password string to char array if provided
            char[] passwordChars = null;
            if (password != null && !password.trim().isEmpty()) {
                passwordChars = password.toCharArray();
            }
            
            PortfolioFileInfo fileInfo = portfolioFileService.openFileById(
                portfolioId,
                passwordChars
            );
            
            logger.info("Portfolio opened successfully (cache size after: " + portfolioFileService.getCacheStats().get("cachedClients") + ")");
            
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
     * Get widget data for a specific portfolio, dashboard, column, and widget.
     * 
     * @param portfolioId The portfolio ID
     * @param dashboardId The dashboard ID
     * @param columnIndex The column index
     * @param widgetIndex The widget index within the column
     * @param reportingPeriodCode Optional reporting period code to override widget configuration
     * @return Widget data
     */
    @GET
    @Path("/{portfolioId}/widgetData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWidget(@PathParam("portfolioId") String portfolioId,
                            @QueryParam("dashboardId") String dashboardId,
                            @QueryParam("columnIndex") Integer columnIndex,
                            @QueryParam("widgetIndex") Integer widgetIndex,
                            @QueryParam("reportingPeriodCode") String reportingPeriodCode) {
        try {
            logger.info("Getting widget for portfolio " + portfolioId + ", dashboard " + dashboardId + ", column " + columnIndex + ", widget " + widgetIndex);
            
            // Validate required query parameters
            if (dashboardId == null || dashboardId.trim().isEmpty()) {
                logger.warn("Missing required parameter: dashboardId");
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Missing required parameter", 
                    "dashboardId query parameter is required");
            }
            
            if (columnIndex == null) {
                logger.warn("Missing required parameter: columnIndex");
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Missing required parameter", 
                    "columnIndex query parameter is required");
            }
            
            if (widgetIndex == null) {
                logger.warn("Missing required parameter: widgetIndex");
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Missing required parameter", 
                    "widgetIndex query parameter is required");
            }
            
            // Get the cached Client for this portfolio
            logger.info("Getting cached client for portfolio: " + portfolioId + " (cache size: " + portfolioFileService.getCacheStats().get("cachedClients") + ")");
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing widgets");
            }
            
            // Find the dashboard by ID
            Dashboard dashboard = client.getDashboards()
                .filter(d -> dashboardId.equals(d.getId()))
                .findFirst()
                .orElse(null);
            
            if (dashboard == null) {
                logger.warn("Dashboard not found: " + dashboardId + " in portfolio: " + portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Dashboard not found", 
                    "Dashboard with ID " + dashboardId + " not found");
            }
            
            // Get the columns from the dashboard
            List<Dashboard.Column> columns = dashboard.getColumns();
            
            if (columnIndex < 0 || columnIndex >= columns.size()) {
                logger.warn("Column index out of bounds: " + columnIndex + " (dashboard has " + columns.size() + " columns)");
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Column index out of bounds", 
                    "Column index " + columnIndex + " is out of bounds (0-" + (columns.size() - 1) + ")");
            }
            
            Dashboard.Column column = columns.get(columnIndex);
            List<Dashboard.Widget> widgets = column.getWidgets();
            
            if (widgetIndex < 0 || widgetIndex >= widgets.size()) {
                logger.warn("Widget index out of bounds: " + widgetIndex + " (column has " + widgets.size() + " widgets)");
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Widget index out of bounds", 
                    "Widget index " + widgetIndex + " is out of bounds (0-" + (widgets.size() - 1) + ")");
            }
            
            Dashboard.Widget widget = widgets.get(widgetIndex);
            
            // Use WidgetDataService to get widget data with the Dashboard.Widget object
            Map<String, Object> widgetData = widgetDataService.getWidgetData(widget, client, reportingPeriodCode);
            
            // Add portfolio context to the response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("dashboardId", dashboardId);
            response.put("columnIndex", columnIndex);
            response.put("widgetIndex", widgetIndex);
            response.putAll(widgetData);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting widget for portfolio " + portfolioId + ", dashboard " + dashboardId + ", column " + columnIndex + ", widget " + widgetIndex + ": " + e.getMessage());
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
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
            logger.info("Updating prices for portfolio: " + portfolioId);
            
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
            
            // Create predicate to filter only active (non-retired) securities
            Predicate<Security> onlyActive = s -> !s.isRetired()
                     && (s.getTickerSymbol() == null || !s.getTickerSymbol().replaceAll("\\s+", "").matches(".*\\d{6}[CP]\\d{8}"));
            
            // Create and schedule the update quotes job with both LATEST and HISTORIC targets
            Job updateJob = new UpdatePricesJob(client, onlyActive,
                            EnumSet.of(UpdatePricesJob.Target.LATEST, UpdatePricesJob.Target.HISTORIC));
            updateJob.schedule();
            
            logger.info("Waiting for price update job to complete...");
            
            // Wait for the job to complete
            updateJob.join();
            
            logger.info("Price update job completed. Now updating exchange rates...");
            
            // Update exchange rates (like the scheduled job does)
            exchangeRateUpdateService.updateExchangeRates();
            
            logger.info("Exchange rates updated. Saving portfolio file...");
            
            // Save the portfolio file after the update
            portfolioFileService.saveFile(portfolioId);
            
            logger.info("Portfolio file saved successfully");
            
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
     * Get historical prices for a specific security.
     * 
     * This endpoint returns all historical prices for a security identified by its UUID.
     * The prices are returned in chronological order with dates and values.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Response containing the security's historical prices
     */
    @GET
    @Path("/{portfolioId}/securities/{securityUuid}/prices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecurityPrices(@PathParam("portfolioId") String portfolioId,
                                     @PathParam("securityUuid") String securityUuid) {
        try {
            logger.info("Getting prices for security {} in portfolio {}", securityUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing security prices");
            }
            
            // Find the security by UUID
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found in portfolio");
            }
            
            // Convert SecurityPrice list to DTO list
            List<SecurityPriceDto> priceDtos = security.getPrices().stream()
                .map(price -> new SecurityPriceDto(
                    price.getDate(),
                    price.getValue() / Values.Quote.divider()
                ))
                .collect(Collectors.toList());
            
            // Create response with security info and prices
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("securityUuid", securityUuid);
            response.put("securityName", security.getName());
            response.put("currencyCode", security.getCurrencyCode());
            response.put("isin", security.getIsin());
            response.put("tickerSymbol", security.getTickerSymbol());
            response.put("pricesCount", priceDtos.size());
            response.put("prices", priceDtos);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} prices for security {} ({})", priceDtos.size(), security.getName(), securityUuid);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting prices for security {} in portfolio {}: {}", 
                securityUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get account value over time.
     * 
     * This endpoint returns the account balance over a specified date range.
     * The values are calculated based on account transactions up to each date.
     * 
     * @param portfolioId The portfolio ID
     * @param accountUuid The account UUID
     * @param startDate Start date for the time series (optional, defaults to 1 year ago)
     * @param endDate End date for the time series (optional, defaults to today)
     * @return Response containing the account's values over time
     */
    @GET
    @Path("/{portfolioId}/accounts/{accountUuid}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountValues(@PathParam("portfolioId") String portfolioId,
                                     @PathParam("accountUuid") String accountUuid,
                                     @QueryParam("startDate") String startDate,
                                     @QueryParam("endDate") String endDate) {
        try {
            logger.info("Getting values for account {} in portfolio {}", accountUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing account values");
            }
            
            // Find the account by UUID
            Account account = client.getAccounts().stream()
                .filter(a -> accountUuid.equals(a.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (account == null) {
                logger.warn("Account not found: {} in portfolio: {}", accountUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Account not found", 
                    "Account with UUID " + accountUuid + " not found in portfolio");
            }
            
            // Parse dates with defaults
            LocalDate start = startDate != null && !startDate.isEmpty() 
                ? LocalDate.parse(startDate) 
                : LocalDate.now().minusYears(1);
            LocalDate end = endDate != null && !endDate.isEmpty() 
                ? LocalDate.parse(endDate) 
                : LocalDate.now();
            
            // Validate date range
            if (start.isAfter(end)) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date range", 
                    "Start date must be before or equal to end date");
            }
            
            // Create currency converter
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Calculate values for each date in range
            List<ValueDataPointDto> valuePoints = new ArrayList<>();
            LocalDate currentDate = start;
            
            while (!currentDate.isAfter(end)) {
                AccountSnapshot snapshot = AccountSnapshot.create(account, converter, currentDate);
                Money funds = snapshot.getFunds();
                
                // Convert to double value
                double value = funds.getAmount() / Values.Amount.divider();
                
                valuePoints.add(new ValueDataPointDto(currentDate, value));
                currentDate = currentDate.plusDays(1);
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("accountUuid", accountUuid);
            response.put("accountName", account.getName());
            response.put("currencyCode", account.getCurrencyCode());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("dataPointsCount", valuePoints.size());
            response.put("values", valuePoints);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} value data points for account {} ({})", 
                valuePoints.size(), account.getName(), accountUuid);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting values for account {} in portfolio {}: {}", 
                accountUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get portfolio value over time.
     * 
     * This endpoint returns the portfolio's market value over a specified date range.
     * The values are calculated based on holdings and security prices at each date.
     * 
     * @param portfolioId The portfolio ID (file)
     * @param securityAccountUuid The security account (portfolio) UUID
     * @param startDate Start date for the time series (optional, defaults to 1 year ago)
     * @param endDate End date for the time series (optional, defaults to today)
     * @return Response containing the portfolio's values over time
     */
    @GET
    @Path("/{portfolioId}/securityaccounts/{securityAccountUuid}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortfolioValues(@PathParam("portfolioId") String portfolioId,
                                      @PathParam("securityAccountUuid") String securityAccountUuid,
                                      @QueryParam("startDate") String startDate,
                                      @QueryParam("endDate") String endDate) {
        try {
            logger.info("Getting values for security account {} in portfolio {}", securityAccountUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing portfolio values");
            }
            
            // Find the portfolio by UUID
            Portfolio portfolio = client.getPortfolios().stream()
                .filter(p -> securityAccountUuid.equals(p.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (portfolio == null) {
                logger.warn("Portfolio not found: {} in client: {}", securityAccountUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Portfolio not found", 
                    "Portfolio with UUID " + securityAccountUuid + " not found");
            }
            
            // Parse dates with defaults
            LocalDate start = startDate != null && !startDate.isEmpty() 
                ? LocalDate.parse(startDate) 
                : LocalDate.now().minusYears(1);
            LocalDate end = endDate != null && !endDate.isEmpty() 
                ? LocalDate.parse(endDate) 
                : LocalDate.now();
            
            // Validate date range
            if (start.isAfter(end)) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date range", 
                    "Start date must be before or equal to end date");
            }
            
            // Create currency converter
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Calculate values for each date in range
            List<ValueDataPointDto> valuePoints = new ArrayList<>();
            LocalDate currentDate = start;
            
            while (!currentDate.isAfter(end)) {
                PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, currentDate);
                Money marketValue = snapshot.getValue();
                
                // Convert to double value
                double value = marketValue.getAmount() / Values.Amount.divider();
                
                valuePoints.add(new ValueDataPointDto(currentDate, value));
                currentDate = currentDate.plusDays(1);
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("securityAccountUuid", securityAccountUuid);
            response.put("securityAccountName", portfolio.getName());
            response.put("baseCurrency", client.getBaseCurrency());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("dataPointsCount", valuePoints.size());
            response.put("values", valuePoints);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} value data points for portfolio {} ({})", 
                valuePoints.size(), portfolio.getName(), securityAccountUuid);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting values for portfolio {} in client {}: {}", 
                securityAccountUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get earnings transactions for a given period.
     * 
     * This endpoint returns all earnings transactions (dividends, interest, interest charges)
     * within a specified date range.
     * 
     * @param portfolioId The portfolio ID (file)
     * @param startDate Start date for filtering transactions (optional, defaults to 1 year ago)
     * @param endDate End date for filtering transactions (optional, defaults to today)
     * @param type Filter by transaction type: DIVIDENDS, INTEREST, INTEREST_CHARGE, or ALL (optional, defaults to ALL)
     * @return Response containing the list of earnings transactions
     */
    @GET
    @Path("/{portfolioId}/earnings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEarnings(@PathParam("portfolioId") String portfolioId,
                               @QueryParam("startDate") String startDate,
                               @QueryParam("endDate") String endDate,
                               @QueryParam("type") String type) {
        try {
            logger.info("Getting earnings for portfolio {} from {} to {} (type: {})", 
                portfolioId, startDate, endDate, type);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing earnings");
            }
            
            // Parse dates with defaults
            LocalDate start = startDate != null && !startDate.isEmpty() 
                ? LocalDate.parse(startDate) 
                : LocalDate.now().minusYears(1);
            LocalDate end = endDate != null && !endDate.isEmpty() 
                ? LocalDate.parse(endDate) 
                : LocalDate.now();
            
            // Validate date range
            if (start.isAfter(end)) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date range", 
                    "Start date must be before or equal to end date");
            }
            
            // Determine which transaction types to include
            Predicate<AccountTransaction.Type> typeFilter;
            String filterType = type != null && !type.isEmpty() ? type.toUpperCase() : "ALL";
            
            switch (filterType) {
                case "DIVIDENDS":
                    typeFilter = t -> t == AccountTransaction.Type.DIVIDENDS;
                    break;
                case "INTEREST":
                    typeFilter = t -> t == AccountTransaction.Type.INTEREST 
                                   || t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
                case "INTEREST_CHARGE":
                    typeFilter = t -> t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
                case "ALL":
                default:
                    typeFilter = t -> t == AccountTransaction.Type.DIVIDENDS 
                                   || t == AccountTransaction.Type.INTEREST 
                                   || t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
            }
            
            // Create currency converter for base currency conversion
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Collect all earnings transactions from all accounts
            List<EarningsTransactionDto> earnings = new ArrayList<>();
            
            for (Account account : client.getAccounts()) {
                for (AccountTransaction tx : account.getTransactions()) {
                    // Filter by type
                    if (!typeFilter.test(tx.getType())) {
                        continue;
                    }
                    
                    // Filter by date range
                    LocalDate txDate = tx.getDateTime().toLocalDate();
                    if (txDate.isBefore(start) || txDate.isAfter(end)) {
                        continue;
                    }
                    
                    // Create DTO
                    EarningsTransactionDto dto = new EarningsTransactionDto();
                    dto.setUuid(tx.getUUID());
                    dto.setDateTime(tx.getDateTime());
                    dto.setType(tx.getType().name());
                    dto.setCurrencyCode(tx.getCurrencyCode());
                    
                    // Original currency amounts
                    double amount = tx.getAmount() / Values.Amount.divider();
                    double grossValue = tx.getGrossValueAmount() / Values.Amount.divider();
                    
                    Money taxesMoney = tx.getUnitSum(name.abuchen.portfolio.model.Transaction.Unit.Type.TAX);
                    Money feesMoney = tx.getUnitSum(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE);
                    
                    double taxes = taxesMoney.getAmount() / Values.Amount.divider();
                    double fees = feesMoney.getAmount() / Values.Amount.divider();
                    
                    dto.setAmount(amount);
                    dto.setGrossValue(grossValue);
                    dto.setTaxes(taxes);
                    dto.setFees(fees);
                    
                    // Converted amounts in base currency
                    dto.setBaseCurrency(client.getBaseCurrency());
                    
                    Money amountMoney = tx.getMonetaryAmount().with(converter.at(tx.getDateTime()));
                    dto.setAmountInBaseCurrency(amountMoney.getAmount() / Values.Amount.divider());
                    
                    Money grossValueMoney = tx.getGrossValue().with(converter.at(tx.getDateTime()));
                    dto.setGrossValueInBaseCurrency(grossValueMoney.getAmount() / Values.Amount.divider());
                    
                    Money taxesConverted = taxesMoney.with(converter.at(tx.getDateTime()));
                    dto.setTaxesInBaseCurrency(taxesConverted.getAmount() / Values.Amount.divider());
                    
                    Money feesConverted = feesMoney.with(converter.at(tx.getDateTime()));
                    dto.setFeesInBaseCurrency(feesConverted.getAmount() / Values.Amount.divider());
                    
                    // Add security information if available
                    if (tx.getSecurity() != null) {
                        dto.setSecurityUuid(tx.getSecurity().getUUID());
                        dto.setSecurityName(tx.getSecurity().getName());
                        dto.setSecurityIsin(tx.getSecurity().getIsin());
                    }
                    
                    // Add account information
                    dto.setAccountUuid(account.getUUID());
                    dto.setAccountName(account.getName());
                    dto.setNote(tx.getNote());
                    dto.setSource(tx.getSource());
                    
                    earnings.add(dto);
                }
            }
            
            // Sort by date (most recent first)
            earnings.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
            
            // Calculate totals in base currency (for consistent aggregation across currencies)
            double totalAmountInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getAmountInBaseCurrency)
                .sum();
            double totalGrossValueInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getGrossValueInBaseCurrency)
                .sum();
            double totalTaxesInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getTaxesInBaseCurrency)
                .sum();
            double totalFeesInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getFeesInBaseCurrency)
                .sum();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("filterType", filterType);
            response.put("count", earnings.size());
            response.put("baseCurrency", client.getBaseCurrency());
            response.put("totalAmountInBaseCurrency", totalAmountInBaseCurrency);
            response.put("totalGrossValueInBaseCurrency", totalGrossValueInBaseCurrency);
            response.put("totalTaxesInBaseCurrency", totalTaxesInBaseCurrency);
            response.put("totalFeesInBaseCurrency", totalFeesInBaseCurrency);
            response.put("earnings", earnings);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} earnings transactions for portfolio {} (total in base currency: {}, gross: {})", 
                earnings.size(), portfolioId, totalAmountInBaseCurrency, totalGrossValueInBaseCurrency);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting earnings for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    
}
