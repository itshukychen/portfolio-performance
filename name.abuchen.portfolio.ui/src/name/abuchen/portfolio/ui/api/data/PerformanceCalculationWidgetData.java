package name.abuchen.portfolio.ui.api.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Category;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Position;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

/**
 * Data-only implementation of PerformanceCalculationWidget for API usage.
 * 
 * This class provides the same calculation logic as PerformanceCalculationWidget but
 * generates only data output without UI components.
 */
public class PerformanceCalculationWidgetData {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceCalculationWidgetData.class);
    
    public enum TableLayout {
        FULL, REDUCED, RELEVANT;
    }
    
    private final String widgetId;
    private final DashboardData dashboardData;
    private final Map<String, String> config;

    public PerformanceCalculationWidgetData(Widget widget, DashboardData dashboardData) {
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
            logger.debug("Generating data for performance calculation widget: {}", widgetId);
            
            // Get data series from configuration
            DataSeries dataSeries = getDataSeriesFromConfig();
            if (dataSeries == null) {
                logger.warn("No data series found for widget: {}", widgetId);
                return createErrorResponse("No data series configured");
            }
            
            // Get reporting period from configuration
            Interval reportingPeriod = getReportingPeriodFromConfig();
            
            // Get cost method from configuration
            boolean useFifo = getCostMethodFromConfig();
            
            // Get layout from configuration
            TableLayout layout = getLayoutFromConfig();
            
            // Calculate performance snapshot
            PerformanceIndex index = dashboardData.calculate(dataSeries, reportingPeriod);
            ClientPerformanceSnapshot snapshot = index.getClientPerformanceSnapshot(useFifo)
                    .orElseThrow(() -> new IllegalArgumentException("Unable to calculate performance snapshot"));
            
            // Create response based on layout
            Map<String, Object> response = new HashMap<>();
            response.put("widgetId", widgetId);
            response.put("type", "performanceCalculation");
            response.put("layout", layout.name());
            response.put("costMethod", useFifo ? "FIFO" : "MOVING_AVERAGE");
            response.put("reportingPeriod", formatReportingPeriod(reportingPeriod));
            
            // Add category data based on layout
            switch (layout) {
                case FULL:
                    response.put("categories", createFullCategories(snapshot));
                    break;
                case REDUCED:
                    response.put("categories", createReducedCategories(snapshot));
                    break;
                case RELEVANT:
                    response.put("categories", createRelevantCategories(snapshot));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported layout: " + layout);
            }
            
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating data for widget: {}", widgetId, e);
            return createErrorResponse("Error generating widget data: " + e.getMessage());
        }
    }
    
    private DataSeries getDataSeriesFromConfig() {
        String uuid = (config != null) ? config.get("DATA_SERIES") : null;
        
        DataSeries dataSeries = null;
        
        if (uuid != null && !uuid.isEmpty()) {
            dataSeries = dashboardData.getDataSeriesSet().lookup(uuid);
        }
        
        if (dataSeries == null) {
            dataSeries = dashboardData.getDataSeriesSet().getAvailableSeries().stream()
                    .filter(ds -> ds.getType().equals(DataSeries.Type.CLIENT))
                    .findFirst()
                    .orElse(null);
        }
        
        return dataSeries;
    }
    
    private Interval getReportingPeriodFromConfig() {
        String reportingPeriodCode = (config != null) ? config.get("REPORTING_PERIOD") : null;
        
        if (reportingPeriodCode != null && !reportingPeriodCode.isEmpty()) {
            try {
                name.abuchen.portfolio.snapshot.ReportingPeriod reportingPeriod = 
                    name.abuchen.portfolio.snapshot.ReportingPeriod.from(reportingPeriodCode);
                return reportingPeriod.toInterval(LocalDate.now());
            } catch (Exception e) {
                logger.warn("Failed to parse reporting period code: {}", reportingPeriodCode, e);
            }
        }
        
        if (dashboardData.getDefaultReportingPeriod() != null) {
            return dashboardData.getDefaultReportingPeriod().toInterval(LocalDate.now());
        }
        
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(1);
        return Interval.of(start, end);
    }
    
    private boolean getCostMethodFromConfig() {
        String costMethodStr = (config != null) ? config.get("COST_METHOD") : null;
        
        if (costMethodStr != null && !costMethodStr.isEmpty()) {
            try {
                CostMethod costMethod = CostMethod.valueOf(costMethodStr);
                return costMethod.useFifo();
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse cost method: {}", costMethodStr, e);
            }
        }
        
        // Default to FIFO
        return true;
    }
    
    private TableLayout getLayoutFromConfig() {
        String layoutStr = (config != null) ? config.get("LAYOUT") : null;
        
        if (layoutStr != null && !layoutStr.isEmpty()) {
            try {
                return TableLayout.valueOf(layoutStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse layout: {}", layoutStr, e);
            }
        }
        
        // Default to FULL
        return TableLayout.FULL;
    }
    
    private List<Map<String, Object>> createFullCategories(ClientPerformanceSnapshot snapshot) {
        List<Map<String, Object>> categories = new ArrayList<>();
        
        for (Category category : snapshot.getCategories()) {
            categories.add(createCategoryData(category, snapshot));
        }
        
        return categories;
    }
    
    private List<Map<String, Object>> createReducedCategories(ClientPerformanceSnapshot snapshot) {
        List<Map<String, Object>> categories = new ArrayList<>();
        List<Category> allCategories = snapshot.getCategories();
        
        int showFirstXItems = 3;
        int showLastXItems = 2;
        int count = CategoryType.values().length;
        
        // Add first 3 items
        for (int i = 0; i < showFirstXItems; i++) {
            categories.add(createCategoryData(allCategories.get(i), snapshot));
        }
        
        // Calculate and add "Other Movements" category
        Money misc = sumCategoryValuations(
                snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode(),
                allCategories.subList(showFirstXItems, count - showLastXItems));
        
        Map<String, Object> miscCategory = new HashMap<>();
        miscCategory.put("sign", "+");
        miscCategory.put("label", "Other Movements");
        miscCategory.put("value", Values.Money.format(misc, dashboardData.getClient().getBaseCurrency()));
        miscCategory.put("rawValue", misc.getAmount());
        miscCategory.put("currency", misc.getCurrencyCode());
        categories.add(miscCategory);
        
        // Add last 2 items
        for (int i = count - showLastXItems; i < count; i++) {
            categories.add(createCategoryData(allCategories.get(i), snapshot));
        }
        
        return categories;
    }
    
    private List<Map<String, Object>> createRelevantCategories(ClientPerformanceSnapshot snapshot) {
        List<Map<String, Object>> categories = new ArrayList<>();
        List<Category> allCategories = snapshot.getCategories();
        
        // Header
        LocalDate startDate = snapshot.getStartClientSnapshot().getTime();
        Map<String, Object> header = new HashMap<>();
        header.put("sign", "");
        header.put("label", "Transactions from " + Values.Date.format(startDate));
        header.put("value", "");
        header.put("isHeader", true);
        categories.add(header);
        
        // Categories 1-6 (relevant transactions)
        for (int i = 1; i < 7; i++) {
            categories.add(createCategoryData(allCategories.get(i), snapshot));
        }
        
        // Footer
        LocalDate endDate = snapshot.getEndClientSnapshot().getTime();
        Money totalRelevantTransactions = sumCategoryValuations(
                snapshot.getValue(CategoryType.INITIAL_VALUE).getCurrencyCode(),
                allCategories.subList(1, 7));
        
        Map<String, Object> footer = new HashMap<>();
        footer.put("sign", "=");
        footer.put("label", "Total relevant transactions until " + Values.Date.format(endDate));
        footer.put("value", Values.Money.format(totalRelevantTransactions, dashboardData.getClient().getBaseCurrency()));
        footer.put("rawValue", totalRelevantTransactions.getAmount());
        footer.put("currency", totalRelevantTransactions.getCurrencyCode());
        footer.put("isFooter", true);
        categories.add(footer);
        
        return categories;
    }
    
    private Map<String, Object> createCategoryData(Category category, ClientPerformanceSnapshot snapshot) {
        Map<String, Object> data = new HashMap<>();
        data.put("sign", category.getSign());
        data.put("label", category.getLabel());
        data.put("value", Values.Money.format(category.getValuation(), dashboardData.getClient().getBaseCurrency()));
        data.put("rawValue", category.getValuation().getAmount());
        data.put("currency", category.getValuation().getCurrencyCode());
        
        // Add positions if available
        if (!category.getPositions().isEmpty()) {
            data.put("positions", createPositionsData(category, snapshot));
        }
        
        return data;
    }
    
    private List<Map<String, Object>> createPositionsData(Category category, ClientPerformanceSnapshot snapshot) {
        List<Position> positions = new ArrayList<>(category.getPositions());
        Collections.sort(positions, (r, l) -> l.getValue().compareTo(r.getValue()));
        
        List<Map<String, Object>> positionsData = new ArrayList<>();
        
        for (Position position : positions) {
            Map<String, Object> posData = new HashMap<>();
            posData.put("label", position.getLabel());
            posData.put("value", Values.Money.format(position.getValue()));
            posData.put("rawValue", position.getValue().getAmount());
            posData.put("currency", position.getValue().getCurrencyCode());
            
            if (position.getSecurity() != null) {
                posData.put("security", position.getSecurity().getName());
                posData.put("securityUuid", position.getSecurity().getUUID());
                
                // Check if security still has holdings at end of period
                boolean hasHoldings = snapshot.getEndClientSnapshot()
                        .getPositionsByVehicle().get(position.getSecurity()) != null;
                posData.put("hasHoldings", hasHoldings);
            }
            
            positionsData.add(posData);
        }
        
        return positionsData;
    }
    
    private Money sumCategoryValuations(String currencyCode, List<Category> categories) {
        MutableMoney totalMoney = MutableMoney.of(currencyCode);
        
        for (Category category : categories) {
            String sign = category.getSign();
            switch (sign) {
                case "+":
                    totalMoney.add(category.getValuation());
                    break;
                case "-":
                    totalMoney.subtract(category.getValuation());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sign: " + sign);
            }
        }
        
        return totalMoney.toMoney();
    }
    
    private Map<String, String> formatReportingPeriod(Interval interval) {
        Map<String, String> period = new HashMap<>();
        period.put("start", interval.getStart().toString());
        period.put("end", interval.getEnd().toString());
        return period;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("widgetId", widgetId);
        response.put("type", "performanceCalculation");
        response.put("error", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}
