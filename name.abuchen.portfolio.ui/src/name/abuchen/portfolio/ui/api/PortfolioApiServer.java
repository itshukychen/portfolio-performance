package name.abuchen.portfolio.ui.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import name.abuchen.portfolio.PortfolioLog;

public class PortfolioApiServer
{
    private static final int DEFAULT_PORT = 8080;
    private Server server;

    public void start() throws Exception
    {
        start(DEFAULT_PORT);
    }

    public void start(int port) throws Exception
    {
        PortfolioLog.info("🚀 Starting Portfolio Performance API Server...");
        PortfolioLog.info("🌐 Port: " + port);
        
        // Create Jetty server
        server = new Server(port);
        
        // Create servlet context with context path in constructor
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configure JAX-RS servlet with Jersey configuration
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(new name.abuchen.portfolio.ui.api.config.JerseyConfig()));
        jerseyServlet.setInitParameter("jersey.config.server.wadl.disable", "true");
        
        context.addServlet(jerseyServlet, "/*");
        
        server.setHandler(context);
        
        // Start server
        server.start();
        
        PortfolioLog.info("✅ Portfolio API Server started successfully!");
        PortfolioLog.info("📋 Available endpoints:");
        PortfolioLog.info("  GET /api/hello - Hello world endpoint");
        PortfolioLog.info("  GET /api/v1/portfolios - List all portfolios");
        PortfolioLog.info("  GET /api/v1/portfolios/health - Health check");
        PortfolioLog.info("  GET /api/v1/portfolios/{portfolioId} - Get portfolio by ID");
        PortfolioLog.info("  GET /api/v1/portfolios/{portfolioId}/widgetData - Get widget data");
        PortfolioLog.info("");
        PortfolioLog.info("🌐 Server running at: http://localhost:" + port);
    }

    public void stop()
    {
        if (server != null && server.isRunning())
        {
            try
            {
                server.stop();
                PortfolioLog.info("✅ Portfolio API Server stopped");
            }
            catch (Exception e)
            {
                PortfolioLog.error("❌ Error stopping server: " + e.getMessage());
            }
        }
    }

    public boolean isRunning()
    {
        return server != null && server.isRunning();
    }

}
