package name.abuchen.portfolio.ui.api.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.ui.api.data.Widget;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

/**
 * Data-only implementation of IndicatorWidget for API usage.
 * 
 * This class provides the same builder pattern as IndicatorWidget but
 * generates only data output without UI components.
 */
public class IndicatorWidgetData<N> {
    
    private static final Logger logger = LoggerFactory.getLogger(IndicatorWidgetData.class);
    
    public static class Builder<N> {
        private String widgetId;
        private DashboardData dashboardData;
        private Values<N> formatter;
        private BiFunction<DataSeries, Interval, N> provider;
        private BiFunction<DataSeries, Interval, String> tooltip;
        private boolean supportsBenchmarks = true;
        private Predicate<DataSeries> predicate;
        private boolean isValueColored = true;
                // Removed additionalConfig functionality to simplify API implementation
        private Map<String, String> config;

        public Builder(Widget widget, DashboardData dashboardData) {
            this.widgetId = widget.getId();
            this.dashboardData = dashboardData;
            this.config = widget.getConfiguration(); // Extract config from widget
        }

        public Builder<N> with(Values<N> formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder<N> with(Predicate<DataSeries> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder<N> with(BiFunction<DataSeries, Interval, N> provider) {
            this.provider = provider;
            return this;
        }

        public Builder<N> withTooltip(BiFunction<DataSeries, Interval, String> tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder<N> withBenchmarkDataSeries(boolean supportsBenchmarks) {
            this.supportsBenchmarks = supportsBenchmarks;
            return this;
        }

        public Builder<N> withColoredValues(boolean isValueColored) {
            this.isValueColored = isValueColored;
            return this;
        }

        // Removed withConfig method to simplify API implementation

        public Builder<N> withConfig(Map<String, String> config) {
            this.config = config;
            return this;
        }

                public IndicatorWidgetData<N> build() {
                    Objects.requireNonNull(formatter);
                    Objects.requireNonNull(provider);

                    return new IndicatorWidgetData<>(widgetId, dashboardData, formatter, provider, 
                            tooltip, supportsBenchmarks, predicate, isValueColored, config);
                }
    }

    private final String widgetId;
    private final DashboardData dashboardData;
    private final Values<N> formatter;
    private final BiFunction<DataSeries, Interval, N> provider;
    private final BiFunction<DataSeries, Interval, String> tooltip;
    private final boolean supportsBenchmarks;
    private final Predicate<DataSeries> predicate;
    private final boolean isValueColored;
    // Removed additionalConfig field to simplify API implementation
    private final Map<String, String> config;

    private IndicatorWidgetData(String widgetId, DashboardData dashboardData,
            Values<N> formatter, BiFunction<DataSeries, Interval, N> provider,
            BiFunction<DataSeries, Interval, String> tooltip, boolean supportsBenchmarks,
            Predicate<DataSeries> predicate, boolean isValueColored,
            Map<String, String> config) {
        this.widgetId = widgetId;
        this.dashboardData = dashboardData;
        this.formatter = formatter;
        this.provider = provider;
        this.tooltip = tooltip;
        this.supportsBenchmarks = supportsBenchmarks;
        this.predicate = predicate;
        this.isValueColored = isValueColored;
        this.config = config;
    }

    public static <N> Builder<N> create(Widget widget, DashboardData dashboardData) {
        return new IndicatorWidgetData.Builder<>(widget, dashboardData);
    }

    /**
     * Generate widget data based on the configuration.
     * 
     * @return Map containing the widget data
     */
    public Map<String, Object> generateData() {
        try {
            logger.debug("Generating data for indicator widget: {}", widgetId);
            
            // Get data series from configuration
            DataSeries dataSeries = getDataSeriesFromConfig();
            if (dataSeries == null) {
                logger.warn("No data series found for widget: {}", widgetId);
                return createErrorResponse("No data series configured");
            }
            
            // Get reporting period from configuration (will fall back to defaults)
            Interval reportingPeriod = getReportingPeriodFromConfig();
            
            // Calculate the value using the provider function
            N value = provider.apply(dataSeries, reportingPeriod);
            
            // Format the value
            String formattedValue = formatValue(value);
            
            // Create response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("widgetId", widgetId);
            response.put("type", "indicator");
            response.put("value", formattedValue);
            response.put("rawValue", value);
            response.put("isNegative", isNegative(value));
            response.put("supportsBenchmarks", supportsBenchmarks);
            
            // Add tooltip if available
            if (tooltip != null) {
                response.put("tooltip", tooltip.apply(dataSeries, reportingPeriod));
            }
            
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating data for widget: {}", widgetId, e);
            return createErrorResponse("Error generating widget data: " + e.getMessage());
        }
    }
    
    private DataSeries getDataSeriesFromConfig() {
        // Duplicate the DataSeriesConfig constructor logic:
        // 1. Get UUID from configuration using DATA_SERIES key
        String uuid = (config != null) ? config.get("DATA_SERIES") : null;
        
        DataSeries dataSeries = null;
        
        // 2. Look up DataSeries by UUID if configured
        if (uuid != null && !uuid.isEmpty()) {
            dataSeries = dashboardData.getDataSeriesSet().lookup(uuid);
        }
        
        // 3. If no DataSeries found and supportsEmptyDataSeries is false (as per requirement),
        // fall back to the first available CLIENT type DataSeries
        if (dataSeries == null) {
            dataSeries = dashboardData.getDataSeriesSet().getAvailableSeries().stream()
                    .filter(ds -> ds.getType().equals(DataSeries.Type.CLIENT))
                    .findFirst()
                    .orElse(null);
        }
        
        return dataSeries;
    }
    
    private Interval getReportingPeriodFromConfig() {
        // Try to get reporting period code from config
        String reportingPeriodCode = (config != null) ? config.get("REPORTING_PERIOD") : null;
        
        // If a reporting period code is configured, try to parse it
        if (reportingPeriodCode != null && !reportingPeriodCode.isEmpty()) {
            try {
                name.abuchen.portfolio.snapshot.ReportingPeriod reportingPeriod = 
                    name.abuchen.portfolio.snapshot.ReportingPeriod.from(reportingPeriodCode);
                return reportingPeriod.toInterval(LocalDate.now());
            } catch (Exception e) {
                logger.warn("Failed to parse reporting period code: {}", reportingPeriodCode, e);
            }
        }
        
        // Fall back to default reporting period if available
        if (dashboardData.getDefaultReportingPeriod() != null) {
            return dashboardData.getDefaultReportingPeriod().toInterval(LocalDate.now());
        }
        
        // Last resort: use a 1-year period ending today
        logger.warn("No reporting period configured, using default 1-year period");
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(1);
        return Interval.of(start, end);
    }
    
    private String formatValue(N value) {
        if (value instanceof Money money) {
            return Values.Money.format(money, dashboardData.getClient().getBaseCurrency());
        } else if (value instanceof Number number) {
            return formatter.format(value);
        } else {
            return formatter.format(value);
        }
    }
    
    private boolean isNegative(N value) {
        if (value instanceof Money money) {
            return money.isNegative();
        } else if (value instanceof Number number) {
            return number.doubleValue() < 0;
        }
        return false;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("widgetId", widgetId);
        response.put("type", "indicator");
        response.put("error", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}
