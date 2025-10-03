package name.abuchen.portfolio.ui.api.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.ui.api.dto.ColumnDto;
import name.abuchen.portfolio.ui.api.dto.DashboardDto;
import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.dto.WidgetDto;

/**
 * Mock Portfolio Controller that provides the same endpoints as the real PortfolioController
 * but returns mock data instead of real portfolio data.
 * 
 * This controller uses JAX-RS annotations for a more robust HTTP server implementation.
 */
@Path("/api/v1/portfolios")
public class MockPortfolioController {
    
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
            List<PortfolioFileInfo> portfolios = createMockPortfolioList();
            return Response.ok(portfolios).build();
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to list portfolios");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
        health.put("service", "MockPortfolioFileService");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("cachedClients", 3);
        cacheStats.put("cacheHits", 42);
        cacheStats.put("cacheMisses", 8);
        health.put("cacheStats", cacheStats);
        
        return Response.ok(health).build();
    }
    
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
            // Check if portfolio exists (mock validation)
            if ("nonexistent".equals(portfolioId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Portfolio not found");
                errorResponse.put("message", "Portfolio with ID '" + portfolioId + "' not found");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }
            
            PortfolioFileInfo portfolio = createMockPortfolio(portfolioId);
            return Response.ok(portfolio).build();
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
            // Validate required parameters
            if (dashboardId == null || dashboardId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "dashboardId query parameter is required");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            if (columnIndex == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "columnIndex query parameter is required");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            if (widgetIndex == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required parameter");
                errorResponse.put("message", "widgetIndex query parameter is required");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            // Mock validation
            if ("nonexistent".equals(portfolioId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Portfolio not loaded");
                errorResponse.put("message", "Portfolio must be opened first before accessing widgets");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }
            
            if ("nonexistent".equals(dashboardId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Dashboard not found");
                errorResponse.put("message", "Dashboard with ID '" + dashboardId + "' not found");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
            }
            
            if (columnIndex < 0 || columnIndex >= 3) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Column index out of bounds");
                errorResponse.put("message", "Column index " + columnIndex + " is out of bounds (0-2)");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            if (widgetIndex < 0 || widgetIndex >= 2) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Widget index out of bounds");
                errorResponse.put("message", "Widget index " + widgetIndex + " is out of bounds (0-1)");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            // Create mock widget data
            Map<String, Object> widgetData = createMockWidgetData(portfolioId, dashboardId, columnIndex, widgetIndex);
            return Response.ok(widgetData).build();
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }
    
    // Helper methods for creating mock data
    
    private List<PortfolioFileInfo> createMockPortfolioList() {
        List<PortfolioFileInfo> portfolios = new ArrayList<>();
        
        // Mock portfolio 1
        PortfolioFileInfo portfolio1 = new PortfolioFileInfo();
        portfolio1.setId("portfolio-1");
        portfolio1.setName("My Investment Portfolio");
        portfolio1.setBaseCurrency("EUR");
        portfolio1.setVersion(1);
        portfolio1.setSaveFlags(Set.of("AUTO_SAVE", "BACKUP"));
        portfolio1.setLastModified(LocalDateTime.now().minusDays(1));
        portfolio1.setEncrypted(false);
        portfolio1.setSecuritiesCount(25);
        portfolio1.setAccountsCount(3);
        portfolio1.setPortfoliosCount(1);
        portfolio1.setTransactionsCount(156);
        portfolio1.setClientLoaded(true);
        portfolio1.setClientInfo("Portfolio Performance v0.78.2");
        portfolio1.setDashboards(createMockDashboards());
        portfolios.add(portfolio1);
        
        // Mock portfolio 2
        PortfolioFileInfo portfolio2 = new PortfolioFileInfo();
        portfolio2.setId("portfolio-2");
        portfolio2.setName("Retirement Fund");
        portfolio2.setBaseCurrency("USD");
        portfolio2.setVersion(1);
        portfolio2.setSaveFlags(Set.of("AUTO_SAVE"));
        portfolio2.setLastModified(LocalDateTime.now().minusHours(6));
        portfolio2.setEncrypted(true);
        portfolio2.setSecuritiesCount(15);
        portfolio2.setAccountsCount(2);
        portfolio2.setPortfoliosCount(1);
        portfolio2.setTransactionsCount(89);
        portfolio2.setClientLoaded(false);
        portfolio2.setClientInfo("Portfolio Performance v0.78.2");
        portfolio2.setDashboards(new ArrayList<>());
        portfolios.add(portfolio2);
        
        return portfolios;
    }
    
    private PortfolioFileInfo createMockPortfolio(String portfolioId) {
        PortfolioFileInfo portfolio = new PortfolioFileInfo();
        portfolio.setId(portfolioId);
        portfolio.setName("Mock Portfolio " + portfolioId);
        portfolio.setBaseCurrency("EUR");
        portfolio.setVersion(1);
        portfolio.setSaveFlags(Set.of("AUTO_SAVE", "BACKUP"));
        portfolio.setLastModified(LocalDateTime.now().minusHours(2));
        portfolio.setEncrypted(false);
        portfolio.setSecuritiesCount(30);
        portfolio.setAccountsCount(4);
        portfolio.setPortfoliosCount(1);
        portfolio.setTransactionsCount(200);
        portfolio.setClientLoaded(true);
        portfolio.setClientInfo("Portfolio Performance v0.78.2");
        portfolio.setDashboards(createMockDashboards());
        return portfolio;
    }
    
    private List<DashboardDto> createMockDashboards() {
        List<DashboardDto> dashboards = new ArrayList<>();
        
        // Dashboard 1
        DashboardDto dashboard1 = new DashboardDto("dashboard-1", "Overview");
        dashboard1.setColumns(createMockColumns());
        Map<String, String> config1 = new HashMap<>();
        config1.put("theme", "light");
        config1.put("refreshInterval", "30");
        dashboard1.setConfiguration(config1);
        dashboards.add(dashboard1);
        
        // Dashboard 2
        DashboardDto dashboard2 = new DashboardDto("dashboard-2", "Performance");
        dashboard2.setColumns(createMockColumns());
        Map<String, String> config2 = new HashMap<>();
        config2.put("theme", "dark");
        config2.put("refreshInterval", "60");
        dashboard2.setConfiguration(config2);
        dashboards.add(dashboard2);
        
        return dashboards;
    }
    
    private List<ColumnDto> createMockColumns() {
        List<ColumnDto> columns = new ArrayList<>();
        
        // Column 1
        ColumnDto column1 = new ColumnDto(1);
        column1.setWidgets(createMockWidgets());
        columns.add(column1);
        
        // Column 2
        ColumnDto column2 = new ColumnDto(1);
        column2.setWidgets(createMockWidgets());
        columns.add(column2);
        
        return columns;
    }
    
    private List<WidgetDto> createMockWidgets() {
        List<WidgetDto> widgets = new ArrayList<>();
        
        // Widget 1
        WidgetDto widget1 = new WidgetDto("PERFORMANCE_CHART", "Performance Chart");
        Map<String, String> config1 = new HashMap<>();
        config1.put("period", "1Y");
        config1.put("benchmark", "MSCI World");
        widget1.setConfiguration(config1);
        widgets.add(widget1);
        
        // Widget 2
        WidgetDto widget2 = new WidgetDto("ASSET_ALLOCATION", "Asset Allocation");
        Map<String, String> config2 = new HashMap<>();
        config2.put("view", "pie");
        config2.put("grouping", "asset_class");
        widget2.setConfiguration(config2);
        widgets.add(widget2);
        
        return widgets;
    }
    
    private Map<String, Object> createMockWidgetData(String portfolioId, String dashboardId, int columnIndex, int widgetIndex) {
        Map<String, Object> widgetData = new HashMap<>();
        widgetData.put("portfolioId", portfolioId);
        widgetData.put("dashboardId", dashboardId);
        widgetData.put("columnIndex", columnIndex);
        widgetData.put("widgetIndex", widgetIndex);
        
        // Mock widget data based on widget type
        String widgetType = "PERFORMANCE_CHART"; // Default
        if (widgetIndex == 1) {
            widgetType = "ASSET_ALLOCATION";
        }
        
        widgetData.put("widgetType", widgetType);
        widgetData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if ("PERFORMANCE_CHART".equals(widgetType)) {
            // Mock performance data
            Map<String, Object> performanceData = new HashMap<>();
            performanceData.put("totalReturn", 12.5);
            performanceData.put("annualizedReturn", 8.3);
            performanceData.put("volatility", 15.2);
            performanceData.put("sharpeRatio", 0.55);
            
            List<Map<String, Object>> timeSeries = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", LocalDateTime.now().minusMonths(11 - i).format(DateTimeFormatter.ISO_LOCAL_DATE));
                point.put("value", 1000 + (i * 50) + (Math.random() * 100 - 50));
                timeSeries.add(point);
            }
            performanceData.put("timeSeries", timeSeries);
            
            widgetData.put("data", performanceData);
        } else if ("ASSET_ALLOCATION".equals(widgetType)) {
            // Mock asset allocation data
            Map<String, Object> allocationData = new HashMap<>();
            List<Map<String, Object>> allocations = new ArrayList<>();
            
            Map<String, Object> stocks = new HashMap<>();
            stocks.put("name", "Stocks");
            stocks.put("percentage", 60.0);
            stocks.put("value", 60000.0);
            allocations.add(stocks);
            
            Map<String, Object> bonds = new HashMap<>();
            bonds.put("name", "Bonds");
            bonds.put("percentage", 30.0);
            bonds.put("value", 30000.0);
            allocations.add(bonds);
            
            Map<String, Object> cash = new HashMap<>();
            cash.put("name", "Cash");
            cash.put("percentage", 10.0);
            cash.put("value", 10000.0);
            allocations.add(cash);
            
            allocationData.put("allocations", allocations);
            allocationData.put("totalValue", 100000.0);
            
            widgetData.put("data", allocationData);
        }
        
        return widgetData;
    }
}
