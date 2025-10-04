package name.abuchen.portfolio.ui.api.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode;

/**
 * Data-only implementation of RebalancingChartWidget for API usage.
 * 
 * This class provides the same data calculation logic as RebalancingChartWidget but
 * generates only data output without UI components.
 */
public class RebalancingChartWidgetData {
    
    private static final Logger logger = LoggerFactory.getLogger(RebalancingChartWidgetData.class);
    
    private final String widgetId;
    private final DashboardData dashboardData;
    private final Map<String, String> config;

    public RebalancingChartWidgetData(Widget widget, DashboardData dashboardData) {
        this.widgetId = widget.getId();
        this.dashboardData = dashboardData;
        this.config = widget.getConfiguration();
    }

    /**
     * Generate widget data based on the configuration.
     * 
     * @return Map containing the widget data
     */
    public Map<String, Object> generateData() {
        try {
            logger.debug("Generating data for rebalancing chart widget: {}", widgetId);
            
            // Get taxonomy from configuration
            Taxonomy taxonomy = getTaxonomyFromConfig();
            if (taxonomy == null) {
                logger.warn("No taxonomy found for widget: {}", widgetId);
                return createErrorResponse("No taxonomy configured or available");
            }
            
            // Create taxonomy model
            TaxonomyModel model = new TaxonomyModel(
                    dashboardData.getExchangeRateProviderFactory(),
                    dashboardData.getClient(),
                    taxonomy);
            
            // Apply client filter if applicable
            ClientFilter clientFilter = getClientFilterFromConfig();
            Client filteredClient = clientFilter.filter(dashboardData.getClient());
            if (filteredClient != dashboardData.getClient()) {
                model.updateClientSnapshot(filteredClient);
            }
            
            // Apply configuration flags
            boolean includeUnassigned = getIncludeUnassignedFromConfig();
            model.setExcludeUnassignedCategoryInCharts(!includeUnassigned);
            
            // Extract rebalancing data from taxonomy model
            TaxonomyNode root = model.getClassificationRootNode();
            List<TaxonomyNode> nodes = root.getChildren();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("widgetId", widgetId);
            response.put("type", "rebalancingChart");
            response.put("taxonomyName", taxonomy.getName());
            response.put("taxonomyId", taxonomy.getId());
            response.put("includeUnassigned", includeUnassigned);
            response.put("clientFilter", getClientFilterLabel(clientFilter));
            
            // Convert nodes to data format
            List<Map<String, Object>> categories = new ArrayList<>();
            
            for (TaxonomyNode node : nodes) {
                Map<String, Object> category = new HashMap<>();
                
                Money actual = node.getActual();
                Money target = node.getTarget();
                Money diff = target.subtract(actual);
                
                category.put("name", node.getName());
                category.put("id", node.getId());
                
                // Actual value
                category.put("actualValue", actual.getAmount() / Values.Amount.divider());
                category.put("actualFormatted", Values.Money.format(actual));
                
                // Target value
                category.put("targetValue", target.getAmount() / Values.Amount.divider());
                category.put("targetFormatted", Values.Money.format(target));
                
                // Difference value
                category.put("diffValue", diff.getAmount() / Values.Amount.divider());
                category.put("diffFormatted", Values.Money.format(diff));
                
                // Currency
                category.put("currency", actual.getCurrencyCode());
                
                // Color
                String colorHex = node.getParent().getColor();
                if (colorHex != null) {
                    category.put("color", colorHex);
                }
                
                categories.add(category);
            }
            
            response.put("categories", categories);
            response.put("categoryCount", categories.size());
            
            // Add series metadata
            List<Map<String, Object>> series = new ArrayList<>();
            
            Map<String, Object> actualSeries = new HashMap<>();
            actualSeries.put("id", "actual");
            actualSeries.put("label", "Actual Value");
            actualSeries.put("color", "#3C97DA"); // RGB(60, 151, 218)
            actualSeries.put("visible", true);
            series.add(actualSeries);
            
            Map<String, Object> targetSeries = new HashMap<>();
            targetSeries.put("id", "target");
            targetSeries.put("label", "Target Value");
            targetSeries.put("color", "#71AD46"); // RGB(113, 173, 70)
            targetSeries.put("visible", true);
            series.add(targetSeries);
            
            Map<String, Object> diffSeries = new HashMap<>();
            diffSeries.put("id", "diff");
            diffSeries.put("label", "Delta Value");
            diffSeries.put("color", null); // Use theme red foreground
            diffSeries.put("visible", false);
            series.add(diffSeries);
            
            response.put("series", series);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating data for widget: {}", widgetId, e);
            return createErrorResponse("Error generating widget data: " + e.getMessage());
        }
    }
    
    private Taxonomy getTaxonomyFromConfig() {
        String uuid = (config != null) ? config.get("TAXONOMY") : null;
        
        Taxonomy taxonomy = null;
        
        if (uuid != null) {
            taxonomy = dashboardData.getClient().getTaxonomies().stream()
                    .filter(t -> uuid.equals(t.getId()))
                    .findFirst()
                    .orElse(null);
        }
        
        // Default to first taxonomy if none configured
        if (taxonomy == null && !dashboardData.getClient().getTaxonomies().isEmpty()) {
            taxonomy = dashboardData.getClient().getTaxonomies().get(0);
        }
        
        return taxonomy;
    }
    
    private ClientFilter getClientFilterFromConfig() {
        String storedIdent = (config != null) ? config.get("CLIENT_FILTER") : null;
        
        ClientFilterMenu menu = new ClientFilterMenu(
                dashboardData.getClient(),
                dashboardData.getPreferences(),
                f -> {});
        
        // Select stored filter if available
        if (storedIdent != null) {
            menu.getAllItems()
                    .filter(item -> item.getId().equals(storedIdent))
                    .findAny()
                    .ifPresent(item -> menu.select(item));
        }
        
        return menu.getSelectedFilter();
    }
    
    private String getClientFilterLabel(ClientFilter filter) {
        String storedIdent = (config != null) ? config.get("CLIENT_FILTER") : null;
        
        ClientFilterMenu menu = new ClientFilterMenu(
                dashboardData.getClient(),
                dashboardData.getPreferences(),
                f -> {});
        
        // Select stored filter if available
        if (storedIdent != null) {
            menu.getAllItems()
                    .filter(item -> item.getId().equals(storedIdent))
                    .findAny()
                    .ifPresent(item -> menu.select(item));
        }
        
        return menu.getSelectedItem().getLabel();
    }
    
    private boolean getIncludeUnassignedFromConfig() {
        String code = (config != null) ? config.get(Dashboard.Config.FLAG_INCLUDE_UNASSIGNED.name()) : null;
        // Default is true as per the widget constructor
        return code != null ? Boolean.parseBoolean(code) : true;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("widgetId", widgetId);
        response.put("type", "rebalancingChart");
        response.put("error", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}

