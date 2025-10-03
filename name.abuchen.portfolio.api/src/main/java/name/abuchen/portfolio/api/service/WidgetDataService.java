package name.abuchen.portfolio.api.service;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.api.data.Widget;
import name.abuchen.portfolio.api.factory.WidgetDataFactory;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing widget data operations.
 * 
 * This service provides methods to retrieve and manage widget data
 * for portfolios.
 */
public class WidgetDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(WidgetDataService.class);
    
    /**
     * Constructor for WidgetDataService.
     */
    public WidgetDataService() {
        // No dependencies needed - Client is passed directly to methods
    }
    
    /**
     * Get widget data by Dashboard.Widget object and Client.
     * 
     * @param dashboardWidget The Dashboard.Widget object
     * @param client The portfolio Client instance
     * @return Widget data as a Map
     */
    public Map<String, Object> getWidgetData(Dashboard.Widget dashboardWidget, Client client) {
        logger.info("Getting widget data for Dashboard.Widget: {} with client", dashboardWidget);
        
        if (dashboardWidget == null) {
            logger.warn("Dashboard.Widget is null, returning empty widget data");
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("message", "Dashboard.Widget is null");
            emptyData.put("timestamp", java.time.LocalDateTime.now().toString());
            return emptyData;
        }
        
        if (client == null) {
            logger.warn("Client is null, returning empty widget data");
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("widgetId", dashboardWidget.getLabel());
            emptyData.put("type", dashboardWidget.getType());
            emptyData.put("message", "Client is null");
            emptyData.put("timestamp", java.time.LocalDateTime.now().toString());
            return emptyData;
        }
        
        try {
            // Get the widget factory by widget type using valueOf
            WidgetDataFactory factory = WidgetDataFactory.valueOf(dashboardWidget.getType());
            logger.debug("Found widget factory for type: {}", factory.name());
            
            // Create DashboardData using the provided Client
            DashboardData dashboardData = createDashboardData(client);
            
            if (dashboardData == null) {
                logger.warn("DashboardData is null for client, returning empty widget data");
                
                // Return empty widget data when DashboardData is not available
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("widgetId", dashboardWidget.getLabel());
                emptyData.put("type", dashboardWidget.getType());
                emptyData.put("config", dashboardWidget.getConfiguration());
                emptyData.put("message", "DashboardData not available - requires UI dependencies");
                emptyData.put("timestamp", java.time.LocalDateTime.now().toString());
                return emptyData;
            }
            
            // Create a Widget object from Dashboard.Widget
            Widget widget = new Widget(dashboardWidget.getLabel(), dashboardWidget.getType(), dashboardWidget.getConfiguration());
            return factory.generateData(widget, dashboardData);
            
        } catch (IllegalArgumentException e) {
            logger.warn("No widget factory found for widget type: {}", dashboardWidget.getType());
            
            // Return generic response for unknown widget types
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("widgetId", dashboardWidget.getLabel());
            widgetData.put("type", dashboardWidget.getType());
            widgetData.put("config", dashboardWidget.getConfiguration());
            widgetData.put("message", "Unknown widget type - no factory found");
            widgetData.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return widgetData;
        }
    }
    
    /**
     * Create a DashboardData instance with the provided Client using ContextInjectionFactory.
     * 
     * @param client The portfolio Client instance
     * @return DashboardData instance or null if creation fails
     */
    private DashboardData createDashboardData(Client client) {
        try {
            logger.debug("Creating DashboardData for client using ContextInjectionFactory");
            
            if (client == null) {
                logger.warn("Client is null, cannot create DashboardData");
                return null;
            }
            
            logger.info("Creating DashboardData with {} securities, {} accounts, {} portfolios", 
                client.getSecurities().size(), 
                client.getAccounts().size(), 
                client.getPortfolios().size());
            
            // Create Eclipse context and populate it with required dependencies
            IEclipseContext context = EclipseContextFactory.create();
            
            // Add required dependencies to context
            context.set(Client.class.getName(), client);
            
            logger.info("Creating DashboardData using ContextInjectionFactory");
            
            // Use ContextInjectionFactory to create DashboardData, just like in DashboardView
            return ContextInjectionFactory.make(DashboardData.class, context);
            
        } catch (Exception e) {
            logger.error("Failed to create DashboardData for client", e);
            return null;
        }
    }
}
