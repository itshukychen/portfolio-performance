package name.abuchen.portfolio.ui.api.data;

import java.util.Map;

/**
 * Simplified Widget class for API usage.
 * 
 * This class provides the minimal interface needed for widget data generation
 * without the full UI dependencies of the original Widget class.
 */
public class Widget {
    
    private final String id;
    private final String type;
    private final Map<String, String> configuration;
    
    public Widget(String id, String type, Map<String, String> configuration) {
        this.id = id;
        this.type = type;
        this.configuration = configuration;
    }
    
    public String getId() {
        return id;
    }
    
    public String getType() {
        return type;
    }
    
    public Map<String, String> getConfiguration() {
        return configuration;
    }
    
    @Override
    public String toString() {
        return "Widget{id='" + id + "', type='" + type + "', configuration=" + configuration + "}";
    }
}

