package name.abuchen.portfolio.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.api.controller.HelloWorldController;
import name.abuchen.portfolio.api.controller.PortfolioController;

/**
 * Standalone Portfolio Performance API Server
 * 
 * This server runs the Portfolio Performance API as a standalone HTTP service
 * using Jetty and JAX-RS. It provides REST endpoints for portfolio data access.
 */
public class StandaloneApiServer {
    
    private static final Logger logger = LoggerFactory.getLogger(StandaloneApiServer.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String API_BASE_PATH = "/api/v1";
    
    private Server server;
    private int port;
    
    public StandaloneApiServer() {
        this(DEFAULT_PORT);
    }
    
    public StandaloneApiServer(int port) {
        this.port = port;
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Usage: java StandaloneApiServer [port]");
                System.exit(1);
            }
        }
        
        StandaloneApiServer apiServer = new StandaloneApiServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down API server...");
            apiServer.stop();
        }));
        
        try {
            apiServer.start();
        } catch (Exception e) {
            System.err.println("❌ Failed to start API server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void start() throws Exception {
        logger.info("🚀 Starting Portfolio Performance API Server...");
        logger.info("🌐 Port: {}", port);
        logger.info("📡 Base path: {}", API_BASE_PATH);
        
        // Create Jetty server
        server = new Server(port);
        
        // Create servlet context
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configure JAX-RS servlet with Jersey configuration
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(new name.abuchen.portfolio.api.config.JerseyConfig()));
        jerseyServlet.setInitParameter("jersey.config.server.wadl.disable", "true");
        
        context.addServlet(jerseyServlet, "/*");
        
        // Add root redirect
        context.addServlet(new ServletHolder(new RootRedirectServlet()), "/");
        
        server.setHandler(context);
        
        // Start server
        server.start();
        
        logger.info("✅ API Server started successfully!");
        logger.info("📋 Available endpoints:");
        logger.info("   GET {}/hello", API_BASE_PATH);
        logger.info("   GET {}/health", API_BASE_PATH);
        logger.info("   GET {}/info", API_BASE_PATH);
        logger.info("   GET {}/portfolio/info", API_BASE_PATH);
        logger.info("   GET {}/portfolio/statistics", API_BASE_PATH);
        logger.info("   GET {}/portfolios", API_BASE_PATH);
        logger.info("   POST {}/portfolios/open", API_BASE_PATH);
        logger.info("   GET {}/portfolios/info", API_BASE_PATH);
        logger.info("   GET {}/portfolios/cache/stats", API_BASE_PATH);
        logger.info("   POST {}/portfolios/cache/clear", API_BASE_PATH);
        logger.info("   GET {}/portfolios/health", API_BASE_PATH);
        logger.info("   GET {}/portfolios/{{portfolioId}}", API_BASE_PATH);
        logger.info("   GET {}/portfolios/{{portfolioId}}/widgetData", API_BASE_PATH);
        logger.info("");
        logger.info("🌐 Server running at: http://localhost:{}", port);
        logger.info("🛑 Press Ctrl+C to stop the server");
        
        // Keep server running
        server.join();
    }
    
    public void stop() {
        if (server != null && server.isRunning()) {
            try {
                server.stop();
                logger.info("✅ API Server stopped");
            } catch (Exception e) {
                logger.error("❌ Error stopping server", e);
            }
        }
    }
    
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
    
    public int getPort() {
        return port;
    }
}
