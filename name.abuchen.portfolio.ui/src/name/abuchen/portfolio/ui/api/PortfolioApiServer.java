package name.abuchen.portfolio.ui.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PortfolioApiServer
{
    private static final int DEFAULT_PORT = 8080;
    private HttpServer server;
    private final Gson gson;

    public PortfolioApiServer()
    {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void start() throws Exception
    {
        start(DEFAULT_PORT);
    }

    public void start(int port) throws Exception
    {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Hello world endpoint
        server.createContext("/api/hello", new HelloWorldHandler());
        
        // Mock Portfolio Controller endpoints
        MockPortfolioController mockController = new MockPortfolioController();
        server.createContext("/api/v1/portfolios", new PortfolioRouterHandler(mockController));
        
        // Catch-all handler for unmatched routes
        server.createContext("/", new NotFoundHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        
        System.out.println("Portfolio API Server started on port " + port);
        System.out.println("Available endpoints:");
        System.out.println("  GET /api/hello - Hello world endpoint");
        System.out.println("  GET /api/v1/portfolios - List all portfolios");
        System.out.println("  GET /api/v1/portfolios/health - Health check");
        System.out.println("  GET /api/v1/portfolios/{portfolioId} - Get portfolio by ID");
        System.out.println("  GET /api/v1/portfolios/{portfolioId}/widgetData - Get widget data");
    }

    public void stop()
    {
        if (server != null)
        {
            server.stop(0);
            System.out.println("Portfolio API Server stopped");
        }
    }

    public boolean isRunning()
    {
        return server != null;
    }

    private class HelloWorldHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            // Set CORS headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // Handle preflight requests
            if ("OPTIONS".equals(exchange.getRequestMethod()))
            {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
                return;
            }

            // Only handle GET requests
            if (!"GET".equals(exchange.getRequestMethod()))
            {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
                return;
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Hello from Portfolio Performance API!");
            responseData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            responseData.put("version", "1.0.0");
            responseData.put("status", "running");

            String jsonResponse = gson.toJson(responseData);
            byte[] responseBytes = jsonResponse.getBytes("UTF-8");

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(responseBytes);
            }
        }
    }

    private class PortfolioRouterHandler implements HttpHandler
    {
        private final MockPortfolioController mockController;
        
        public PortfolioRouterHandler(MockPortfolioController mockController)
        {
            this.mockController = mockController;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            String path = exchange.getRequestURI().getPath();
            
            // Route to appropriate handler based on path
            if ("/api/v1/portfolios".equals(path) || "/api/v1/portfolios/".equals(path))
            {
                // List portfolios endpoint
                mockController.new ListPortfoliosHandler().handle(exchange);
            }
            else if ("/api/v1/portfolios/health".equals(path))
            {
                // Health check endpoint
                mockController.new HealthHandler().handle(exchange);
            }
            else if (path.startsWith("/api/v1/portfolios/") && path.endsWith("/widgetData"))
            {
                // Widget data endpoint
                mockController.new GetWidgetDataHandler().handle(exchange);
            }
            else if (path.startsWith("/api/v1/portfolios/") && path.split("/").length == 5)
            {
                // Get portfolio by ID endpoint
                mockController.new GetPortfolioHandler().handle(exchange);
            }
            else
            {
                // Not found
                new NotFoundHandler().handle(exchange);
            }
        }
    }

    private class NotFoundHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            // Set CORS headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // Handle preflight requests
            if ("OPTIONS".equals(exchange.getRequestMethod()))
            {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
                return;
            }

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Not Found");
            errorData.put("message", "The requested endpoint was not found");
            errorData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String jsonResponse = gson.toJson(errorData);
            byte[] responseBytes = jsonResponse.getBytes("UTF-8");

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(404, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(responseBytes);
            }
        }
    }
}
