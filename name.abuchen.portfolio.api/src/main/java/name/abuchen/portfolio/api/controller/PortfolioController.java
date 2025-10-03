package name.abuchen.portfolio.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import name.abuchen.portfolio.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.api.service.PortfolioFileService;
import name.abuchen.portfolio.api.service.WidgetDataService;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;

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
    private static final PortfolioFileService portfolioFileService = new PortfolioFileService();
    private static final WidgetDataService widgetDataService = new WidgetDataService();
    
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
            
            List<name.abuchen.portfolio.api.dto.PortfolioFileInfo> files = portfolioFileService.listPortfolioFiles();
            
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
            logger.info("Opening portfolio by ID: {} (cache size before: {})", portfolioId, portfolioFileService.getCacheStats().get("cachedClients"));
            
            // Convert password string to char array if provided
            char[] passwordChars = null;
            if (password != null && !password.trim().isEmpty()) {
                passwordChars = password.toCharArray();
            }
            
            PortfolioFileInfo fileInfo = portfolioFileService.openFileById(
                portfolioId,
                passwordChars
            );
            
            logger.info("Portfolio opened successfully (cache size after: {})", portfolioFileService.getCacheStats().get("cachedClients"));
            
            return Response.ok(fileInfo).build();
            
        } catch (FileNotFoundException e) {
            logger.error("Portfolio not found: {}", portfolioId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Portfolio not found");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
                
        } catch (IOException e) {
            logger.error("Failed to get portfolio info: {}", portfolioId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get portfolio info");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                
        } catch (Exception e) {
            logger.error("Unexpected error getting portfolio info: {}", portfolioId, e);
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
     * @return Widget data
     */
    @GET
    @Path("/{portfolioId}/widgetData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWidget(@PathParam("portfolioId") String portfolioId,
                            @QueryParam("dashboardId") String dashboardId,
                            @QueryParam("columnIndex") Integer columnIndex,
                            @QueryParam("widgetIndex") Integer widgetIndex) {
        try {
            logger.info("Getting widget for portfolio {}, dashboard {}, column {}, widget {}", 
                portfolioId, dashboardId, columnIndex, widgetIndex);
            
            // Add debug logging to see if the endpoint is being hit
            logger.info("Endpoint hit: /{}/widgetData with query params: dashboardId={}, columnIndex={}, widgetIndex={}", 
                portfolioId, dashboardId, columnIndex, widgetIndex);
            
            // Validate required query parameters
            if (dashboardId == null || dashboardId.trim().isEmpty()) {
                logger.warn("Missing required parameter: dashboardId");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "dashboardId query parameter is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            if (columnIndex == null) {
                logger.warn("Missing required parameter: columnIndex");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "columnIndex query parameter is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            if (widgetIndex == null) {
                logger.warn("Missing required parameter: widgetIndex");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "widgetIndex query parameter is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            // Get the cached Client for this portfolio
            logger.info("Getting cached client for portfolio: {} (cache size: {})", portfolioId, portfolioFileService.getCacheStats().get("cachedClients"));
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Portfolio not loaded");
                errorResponse.put("message", "Portfolio must be opened first before accessing widgets");
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }
            
            // Find the dashboard by ID
            Dashboard dashboard = client.getDashboards()
                .filter(d -> dashboardId.equals(d.getId()))
                .findFirst()
                .orElse(null);
            
            if (dashboard == null) {
                logger.warn("Dashboard not found: {} in portfolio: {}", dashboardId, portfolioId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Dashboard not found");
                errorResponse.put("message", "Dashboard with ID " + dashboardId + " not found");
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }
            
            // Get the columns from the dashboard
            List<Dashboard.Column> columns = dashboard.getColumns();
            
            if (columnIndex < 0 || columnIndex >= columns.size()) {
                logger.warn("Column index out of bounds: {} (dashboard has {} columns)", columnIndex, columns.size());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Column index out of bounds");
                errorResponse.put("message", "Column index " + columnIndex + " is out of bounds (0-" + (columns.size() - 1) + ")");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            Dashboard.Column column = columns.get(columnIndex);
            List<Dashboard.Widget> widgets = column.getWidgets();
            
            if (widgetIndex < 0 || widgetIndex >= widgets.size()) {
                logger.warn("Widget index out of bounds: {} (column has {} widgets)", widgetIndex, widgets.size());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Widget index out of bounds");
                errorResponse.put("message", "Widget index " + widgetIndex + " is out of bounds (0-" + (widgets.size() - 1) + ")");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            Dashboard.Widget widget = widgets.get(widgetIndex);
            
            // Use WidgetDataService to get widget data with the Dashboard.Widget object
            Map<String, Object> widgetData = widgetDataService.getWidgetData(widget, client);
            
            // Add portfolio context to the response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("dashboardId", dashboardId);
            response.put("columnIndex", columnIndex);
            response.put("widgetIndex", widgetIndex);
            response.putAll(widgetData);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting widget for portfolio {}, dashboard {}, column {}, widget {}", 
                portfolioId, dashboardId, columnIndex, widgetIndex, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }
    
    
    
}
