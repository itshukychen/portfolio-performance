package name.abuchen.portfolio.ui.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
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

import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.service.PortfolioFileService;
import name.abuchen.portfolio.ui.api.service.QuoteFeedApiKeyService;
import name.abuchen.portfolio.ui.api.service.WidgetDataService;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Security;

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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to list portfolios");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Portfolio not found");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
                
        } catch (IOException e) {
            logger.error("Failed to get portfolio info: " + portfolioId + " - " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get portfolio info");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                
        } catch (Exception e) {
            logger.error("Unexpected error getting portfolio info: " + portfolioId + " - " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
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
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Portfolio not loaded", 
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
     * Update prices for all active (non-retired) securities in the portfolio.
     * 
     * This endpoint triggers a price update job that fetches both latest and historic quotes
     * for all securities in the portfolio that are not marked as retired.
     * 
     * @param portfolioId The portfolio ID
     * @return Response indicating the price update job was scheduled
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
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Portfolio not loaded", 
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
            
            logger.info("Price update job completed. Saving portfolio file...");
            
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
    
    
}
