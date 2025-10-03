package name.abuchen.portfolio.api.data;

/**
 * Configuration class for widget data operations.
 * 
 * This class provides configuration options for widgets without UI dependencies.
 */
public class WidgetConfig {
    
    private final String key;
    private final String value;
    private final String description;
    
    public WidgetConfig(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "WidgetConfig{key='" + key + "', value='" + value + "', description='" + description + "'}";
    }
}
