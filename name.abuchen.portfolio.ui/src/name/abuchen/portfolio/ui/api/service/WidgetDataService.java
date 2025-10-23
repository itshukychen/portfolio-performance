package name.abuchen.portfolio.ui.api.service;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.ui.api.data.Widget;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import name.abuchen.portfolio.ui.util.UI;
import name.abuchen.portfolio.ui.api.factory.WidgetDataFactory;
import name.abuchen.portfolio.ui.api.data.Widget;

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
     * @param reportingPeriodCode Optional reporting period code to override widget configuration
     * @return Widget data as a Map
     */
    public Map<String, Object> getWidgetData(Dashboard.Widget dashboardWidget, Client client, String reportingPeriodCode) {
        // Marshal all SWT work to the UI thread since this is called from REST handlers
        return UI.sync(() -> getWidgetDataInternal(dashboardWidget, client, reportingPeriodCode));
    }
    
    /**
     * Helper method to create error data with consistent structure.
     * 
     * @param dashboardWidget The widget (optional, can be null)
     * @param message Error message
     * @return Error data map
     */
    private Map<String, Object> createErrorData(Dashboard.Widget dashboardWidget, String message) {
        Map<String, Object> errorData = new HashMap<>();
        if (dashboardWidget != null) {
            errorData.put("widgetId", dashboardWidget.getLabel());
            errorData.put("type", dashboardWidget.getType());
            errorData.put("config", dashboardWidget.getConfiguration());
        }
        errorData.put("message", message);
        errorData.put("timestamp", java.time.LocalDateTime.now().toString());
        return errorData;
    }
    
    /**
     * Internal implementation of getWidgetData that runs on the UI thread.
     * This method and everything it calls may touch SWT resources (Colors, Fonts, etc.)
     */
    private Map<String, Object> getWidgetDataInternal(Dashboard.Widget dashboardWidget, Client client, String reportingPeriodCode) {
        logger.info("Getting widget data for Dashboard.Widget: {} with client", dashboardWidget);
        
        if (dashboardWidget == null) {
            logger.warn("Dashboard.Widget is null, returning empty widget data");
            return createErrorData(null, "Dashboard.Widget is null");
        }
        
        if (client == null) {
            logger.warn("Client is null, returning empty widget data");
            return createErrorData(dashboardWidget, "Client is null");
        }
        
        try {
            // Create DashboardData using the provided Client
            DashboardData dashboardData = createDashboardData(client);
            logger.info("Default Reporting period: {}", dashboardData.getDefaultReportingPeriod());
            logger.info("List Reporting period: {}", dashboardData.getDefaultReportingPeriods());
            
            if (dashboardData == null) {
                logger.warn("DashboardData is null for client, returning empty widget data");
                return createErrorData(dashboardWidget, "DashboardData not available - requires UI dependencies");
            }
            
            // Get widget configuration, potentially overriding the reporting period
            Map<String, String> widgetConfig = getWidgetConfiguration(dashboardWidget, reportingPeriodCode);
            
            // Create Widget wrapper from Dashboard.Widget with potentially modified configuration
            Widget widget = new Widget(
                dashboardWidget.getLabel(),
                dashboardWidget.getType(),
                widgetConfig
            );
            
            // Get the widget type from the Dashboard.Widget
            String widgetType = dashboardWidget.getType();
            
            // Look up the WidgetDataFactory for this widget type
            WidgetDataFactory factory = WidgetDataFactory.valueOf(widgetType);
            
            if (factory == null) {
                logger.warn("No WidgetDataFactory found for type: {}", widgetType);
                return createErrorData(dashboardWidget, "Unknown widget type: " + widgetType);
            }
            
            // Generate widget data using the factory
            logger.info("Generating widget data for type: {}", widgetType);
            Map<String, Object> widgetData = factory.generateData(widget, dashboardData);
            
            logger.info("Successfully generated widget data for type: {}", widgetType);
            return widgetData;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid widget type", e);
            return createErrorData(dashboardWidget, "Invalid widget type: " + dashboardWidget.getType());
        } catch (Exception e) {
            logger.error("Failed to get widget data", e);
            return createErrorData(dashboardWidget, "Error getting widget data: " + e.getMessage());
        }
    }
    
    /**
     * Get widget configuration, optionally setting the reporting period if not already present.
     * 
     * @param dashboardWidget The Dashboard.Widget object
     * @param reportingPeriodCode Optional reporting period code to use if widget doesn't have one
     * @return Widget configuration map
     */
    private Map<String, String> getWidgetConfiguration(Dashboard.Widget dashboardWidget, String reportingPeriodCode) {
        // Get the original configuration from the widget
        Map<String, String> config = new HashMap<>(dashboardWidget.getConfiguration());
        
        // Only set REPORTING_PERIOD if a code is provided AND it doesn't already exist in the config
        if (reportingPeriodCode != null && !reportingPeriodCode.trim().isEmpty()) {
            String existingPeriod = config.get(Dashboard.Config.REPORTING_PERIOD.name());
            if (existingPeriod == null || existingPeriod.trim().isEmpty()) {
                logger.info("Setting REPORTING_PERIOD config with code: {}", reportingPeriodCode);
                config.put(Dashboard.Config.REPORTING_PERIOD.name(), reportingPeriodCode);
            } else {
                logger.debug("Widget already has REPORTING_PERIOD config ({}), not overriding", existingPeriod);
            }
        }
        
        return config;
    }
    
    /**
     * Create a DashboardData instance with the provided Client using Eclipse dependency injection.
     * Populates an Eclipse context with all required dependencies and uses ContextInjectionFactory.
     * 
     * @param client The portfolio Client instance
     * @return DashboardData instance or null if creation fails
     */
    private DashboardData createDashboardData(Client client) {
        try {
            if (client == null) {
                logger.warn("Client is null, cannot create DashboardData");
                return null;
            }
            
            logger.info("Creating DashboardData with {} securities, {} accounts, {} portfolios", 
                client.getSecurities().size(), 
                client.getAccounts().size(), 
                client.getPortfolios().size());
            
            // Create Eclipse context and populate it with all required dependencies
            // This follows the pattern used in PortfolioPart.createView() and AbstractFinanceView.make()
            IEclipseContext context = EclipseContextFactory.create();
            
            // 1. Add Client to context
            context.set(Client.class, client);
            
            // 2. Create and add ExchangeRateProviderFactory (it only needs Client)
            ExchangeRateProviderFactory exchangeRateFactory = new ExchangeRateProviderFactory(client);
            context.set(ExchangeRateProviderFactory.class, exchangeRateFactory);
            
            // 3. Create and add DataSeriesCache using ContextInjectionFactory
            // It will automatically inject Client and ExchangeRateProviderFactory from context
            DataSeriesCache dataSeriesCache = ContextInjectionFactory.make(DataSeriesCache.class, context);
            context.set(DataSeriesCache.class, dataSeriesCache);
            
            // 4. Add IPreferenceStore - use a simple in-memory preference store
            // In a server context, we don't need persistent preferences
            IPreferenceStore preferenceStore = new PreferenceStore();
            context.set(IPreferenceStore.class, preferenceStore);
            
            // 5. IStylingEngine - set to null since it's only used for UI styling
            // In a server/API context, we don't have UI components to style
            context.set(IStylingEngine.class, null);
            
            logger.info("Creating DashboardData using ContextInjectionFactory with populated context");
            
            // Use ContextInjectionFactory to create DashboardData with dependency injection
            return ContextInjectionFactory.make(DashboardData.class, context);
            
        } catch (Exception e) {
            logger.error("Failed to create DashboardData for client", e);
            return null;
        }
    }
}

