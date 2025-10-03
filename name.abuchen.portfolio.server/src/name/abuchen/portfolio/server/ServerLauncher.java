package name.abuchen.portfolio.server;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.ui.api.PortfolioApiServer;

/**
 * Server launcher that starts the HTTP server from the UI project without
 * opening any windows.
 * <p>
 * This launcher creates an SWT Display (required for UI-bound code to run) but
 * does not open any Shell or Workbench windows. The HTTP server is started as
 * the main entry point.
 */
public class ServerLauncher implements IApplication
{
    private static final int DEFAULT_PORT = 8080;
    private static final String PORT_PROPERTY = "portfolio.server.port";
    
    private PortfolioApiServer apiServer;
    private volatile boolean running = true;

    @Override
    public Object start(IApplicationContext context) throws Exception
    {
        PortfolioLog.info("🚀 Starting Portfolio Performance Server...");
        
        // Get port from system property or use default
        int port = getPort();
        
        // Create Display for SWT/JFace components (required for UI runtime)
        // but don't create any Shell or Window
        Display display = Display.getDefault();
        
        // Start the HTTP server from the UI project on the UI thread
        display.asyncExec(() -> {
            try
            {
                apiServer = new PortfolioApiServer();
                apiServer.start(port);
                PortfolioLog.info("✅ Server started successfully on port " + port);
                PortfolioLog.info("📋 Press Ctrl+C to stop the server");
            }
            catch (Exception e)
            {
                PortfolioLog.error("❌ Failed to start server: " + e.getMessage());
                PortfolioLog.error(e);
                running = false;
            }
        });
        
        // Keep the server running until shutdown is requested
        // Process UI events to keep Display alive
        while (running)
        {
            try
            {
                if (!display.readAndDispatch())
                {
                    display.sleep();
                }
            }
            catch (Exception e)
            {
                PortfolioLog.error("Error in event loop: " + e.getMessage());
                PortfolioLog.error(e);
            }
        }
        
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop()
    {
        PortfolioLog.info("🛑 Stopping Portfolio Performance Server...");
        running = false;
        
        if (apiServer != null)
        {
            apiServer.stop();
        }
        
        Display display = Display.getCurrent();
        if (display != null && !display.isDisposed())
        {
            display.wake();
        }
        
        PortfolioLog.info("✅ Server stopped");
    }
    
    private int getPort()
    {
        String portStr = System.getProperty(PORT_PROPERTY);
        if (portStr != null && !portStr.isEmpty())
        {
            try
            {
                return Integer.parseInt(portStr);
            }
            catch (NumberFormatException e)
            {
                PortfolioLog.error("Invalid port specified: " + portStr + ", using default: " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
}

