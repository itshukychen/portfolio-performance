package name.abuchen.portfolio.api.data;

import name.abuchen.portfolio.api.service.DashboardDataService;

/**
 * Delegate interface for widget data operations.
 * 
 * This interface provides the minimal contract needed for widget data generation
 * without UI dependencies.
 */
public interface WidgetDataDelegate<T> {
    
    /**
     * Get the widget ID.
     * 
     * @return The widget identifier
     */
    String getWidgetId();
    
    /**
     * Get the dashboard data service.
     * 
     * @return The dashboard data service
     */
    DashboardDataService getDashboardDataService();
}
