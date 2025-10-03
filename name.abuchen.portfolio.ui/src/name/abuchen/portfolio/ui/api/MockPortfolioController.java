package name.abuchen.portfolio.ui.api;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import name.abuchen.portfolio.ui.api.dto.ColumnDto;
import name.abuchen.portfolio.ui.api.dto.DashboardDto;
import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.dto.WidgetDto;

/**
 * Mock Portfolio Controller that provides the same endpoints as the real PortfolioController
 * but returns mock data instead of real portfolio data.
 */
public class MockPortfolioController {
    
    private final Gson gson;
    
    public MockPortfolioController() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Handler for GET /api/v1/portfolios - List all portfolios
     */
    public class ListPortfoliosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            setCorsHeaders(exchange);
            
            try {
                List<PortfolioFileInfo> portfolios = createMockPortfolioList();
                String jsonResponse = gson.toJson(portfolios);
                sendJsonResponse(exchange, 200, jsonResponse);
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Internal server error", e.getMessage());
            }
        }
    }
    
    /**
     * Handler for GET /api/v1/portfolios/health - Health check
     */
    public class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            setCorsHeaders(exchange);
            
            try {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("service", "MockPortfolioFileService");
                health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("cachedClients", 3);
                cacheStats.put("cacheHits", 42);
                cacheStats.put("cacheMisses", 8);
                health.put("cacheStats", cacheStats);
                
                String jsonResponse = gson.toJson(health);
                sendJsonResponse(exchange, 200, jsonResponse);
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Internal server error", e.getMessage());
            }
        }
    }
    
    /**
     * Handler for GET /api/v1/portfolios/{portfolioId} - Get portfolio by ID
     */
    public class GetPortfolioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            setCorsHeaders(exchange);
            
            try {
                String path = exchange.getRequestURI().getPath();
                String portfolioId = extractPortfolioId(path);
                
                if (portfolioId == null) {
                    sendErrorResponse(exchange, 400, "Bad request", "Portfolio ID is required");
                    return;
                }
                
                // Check if portfolio exists (mock validation)
                if ("nonexistent".equals(portfolioId)) {
                    sendErrorResponse(exchange, 404, "Portfolio not found", "Portfolio with ID '" + portfolioId + "' not found");
                    return;
                }
                
                PortfolioFileInfo portfolio = createMockPortfolio(portfolioId);
                String jsonResponse = gson.toJson(portfolio);
                sendJsonResponse(exchange, 200, jsonResponse);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Internal server error", e.getMessage());
            }
        }
    }
    
    /**
     * Handler for GET /api/v1/portfolios/{portfolioId}/widgetData - Get widget data
     */
    public class GetWidgetDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            setCorsHeaders(exchange);
            
            try {
                String path = exchange.getRequestURI().getPath();
                String portfolioId = extractPortfolioId(path);
                
                if (portfolioId == null) {
                    sendErrorResponse(exchange, 400, "Bad request", "Portfolio ID is required");
                    return;
                }
                
                // Extract query parameters
                Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
                String dashboardId = queryParams.get("dashboardId");
                String columnIndexStr = queryParams.get("columnIndex");
                String widgetIndexStr = queryParams.get("widgetIndex");
                
                // Validate required parameters
                if (dashboardId == null || dashboardId.trim().isEmpty()) {
                    sendErrorResponse(exchange, 400, "Missing required parameter", "dashboardId query parameter is required");
                    return;
                }
                
                if (columnIndexStr == null) {
                    sendErrorResponse(exchange, 400, "Missing required parameter", "columnIndex query parameter is required");
                    return;
                }
                
                if (widgetIndexStr == null) {
                    sendErrorResponse(exchange, 400, "Missing required parameter", "widgetIndex query parameter is required");
                    return;
                }
                
                int columnIndex, widgetIndex;
                try {
                    columnIndex = Integer.parseInt(columnIndexStr);
                    widgetIndex = Integer.parseInt(widgetIndexStr);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid parameter", "columnIndex and widgetIndex must be valid integers");
                    return;
                }
                
                // Mock validation
                if ("nonexistent".equals(portfolioId)) {
                    sendErrorResponse(exchange, 404, "Portfolio not loaded", "Portfolio must be opened first before accessing widgets");
                    return;
                }
                
                if ("nonexistent".equals(dashboardId)) {
                    sendErrorResponse(exchange, 404, "Dashboard not found", "Dashboard with ID '" + dashboardId + "' not found");
                    return;
                }
                
                if (columnIndex < 0 || columnIndex >= 3) {
                    sendErrorResponse(exchange, 400, "Column index out of bounds", "Column index " + columnIndex + " is out of bounds (0-2)");
                    return;
                }
                
                if (widgetIndex < 0 || widgetIndex >= 2) {
                    sendErrorResponse(exchange, 400, "Widget index out of bounds", "Widget index " + widgetIndex + " is out of bounds (0-1)");
                    return;
                }
                
                // Create mock widget data
                Map<String, Object> widgetData = createMockWidgetData(portfolioId, dashboardId, columnIndex, widgetIndex);
                String jsonResponse = gson.toJson(widgetData);
                sendJsonResponse(exchange, 200, jsonResponse);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Internal server error", e.getMessage());
            }
        }
    }
    
    // Helper methods
    
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String error, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        String jsonResponse = gson.toJson(errorResponse);
        sendJsonResponse(exchange, statusCode, jsonResponse);
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendErrorResponse(exchange, 405, "Method not allowed", "Only GET method is supported");
    }
    
    private String extractPortfolioId(String path) {
        // Extract portfolio ID from path like /api/v1/portfolios/{portfolioId} or /api/v1/portfolios/{portfolioId}/widgetData
        String[] parts = path.split("/");
        if (parts.length >= 5 && "portfolios".equals(parts[3])) {
            return parts[4];
        }
        return null;
    }
    
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
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
