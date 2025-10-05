package name.abuchen.portfolio.server;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.impl.ECBExchangeRateProvider;
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
        
        // Initialize exchange rates (normally done by StartupAddon in UI mode)
        initializeExchangeRates();
        
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
        
        // Save exchange rates before shutdown
        saveExchangeRates();
        
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
    
    /**
     * Save exchange rates to cache files on shutdown.
     */
    private void saveExchangeRates()
    {
        PortfolioLog.info("💾 Saving exchange rates to cache...");
        NullProgressMonitor monitor = new NullProgressMonitor();
        
        for (ExchangeRateProvider provider : ExchangeRateProviderFactory.getProviders())
        {
            try
            {
                provider.save(monitor);
                PortfolioLog.info("   ✅ Saved: " + provider.getName());
            }
            catch (IOException e)
            {
                PortfolioLog.error("   ⚠️  Failed to save: " + provider.getName() + " - " + e.getMessage());
            }
        }
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
    
    /**
     * Initialize exchange rate providers by loading their cached data from files.
     * This replicates what StartupAddon.updateExchangeRates() does in UI mode.
     */
    private void initializeExchangeRates()
    {
        PortfolioLog.info("========================================");
        PortfolioLog.info("Initializing exchange rate providers...");
        PortfolioLog.info("========================================");
        
        // Check where ECB stores its cache files
        checkECBStorageLocation();
        
        List<ExchangeRateProvider> providers = ExchangeRateProviderFactory.getProviders();
        PortfolioLog.info("Found " + providers.size() + " exchange rate providers");
        
        if (providers.isEmpty())
        {
            PortfolioLog.error("⚠️  WARNING: No exchange rate providers found! This is a problem.");
            return;
        }
        
        NullProgressMonitor monitor = new NullProgressMonitor();
        
        for (ExchangeRateProvider provider : providers)
        {
            try
            {
                PortfolioLog.info("📥 Loading exchange rates for: " + provider.getName());
                provider.load(monitor);
                PortfolioLog.info("   ✅ Loaded successfully: " + provider.getName());
                
                // Try to update from online if load was successful
                if (provider instanceof ECBExchangeRateProvider)
                {
                    PortfolioLog.info("   🌐 Attempting to update from ECB online...");
                    try
                    {
                        provider.update(monitor);
                        PortfolioLog.info("   ✅ Updated from ECB online successfully");
                        
                        // Save the updated data to cache file
                        PortfolioLog.info("   💾 Saving updated rates to cache...");
                        provider.save(monitor);
                        PortfolioLog.info("   ✅ Saved to cache file");
                    }
                    catch (IOException e)
                    {
                        PortfolioLog.error("   ⚠️  Could not update from online (using cached/default data): " + e.getMessage());
                    }
                }
            }
            catch (Exception e)
            {
                PortfolioLog.error("❌ Failed to load exchange rates for: " + provider.getName());
                PortfolioLog.error("   Error: " + e.getMessage());
                PortfolioLog.error(e);
            }
        }
        
        PortfolioLog.info("========================================");
        PortfolioLog.info("Exchange rate initialization complete");
        PortfolioLog.info("========================================");
    }
    
    /**
     * Check where ECB exchange rate provider stores its cache files.
     */
    private void checkECBStorageLocation()
    {
        try
        {
            Bundle bundle = FrameworkUtil.getBundle(ECBExchangeRateProvider.class);
            if (bundle == null)
            {
                PortfolioLog.error("⚠️  ECB bundle is null - OSGi not initialized properly!");
                return;
            }
            
            PortfolioLog.info("📁 Checking ECB storage location...");
            PortfolioLog.info("   Bundle: " + bundle.getSymbolicName() + " [" + bundle.getBundleId() + "]");
            PortfolioLog.info("   Bundle location: " + bundle.getLocation());
            
            File dataFile = bundle.getDataFile("ecb_exchange_rates.pb");
            if (dataFile != null)
            {
                PortfolioLog.info("   Cache file path: " + dataFile.getAbsolutePath());
                PortfolioLog.info("   Cache file exists: " + dataFile.exists());
                if (dataFile.exists())
                {
                    PortfolioLog.info("   Cache file size: " + dataFile.length() + " bytes");
                    PortfolioLog.info("   Cache file last modified: " + new java.util.Date(dataFile.lastModified()));
                }
                else
                {
                    PortfolioLog.info("   ⚠️  Cache file does not exist - will use defaults until updated from internet");
                }
            }
            else
            {
                PortfolioLog.error("   ⚠️  Could not get data file path from bundle");
            }
        }
        catch (Exception e)
        {
            PortfolioLog.error("Error checking ECB storage location: " + e.getMessage());
            PortfolioLog.error(e);
        }
    }
}

