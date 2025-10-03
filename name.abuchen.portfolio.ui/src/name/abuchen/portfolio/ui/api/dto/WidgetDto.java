package name.abuchen.portfolio.ui.api.dto;

import java.util.Map;

/**
 * Data Transfer Object for Dashboard Widget serialization.
 * Represents a widget within a dashboard column.
 */
public class WidgetDto {
    
    private String type;
    private String label;
    private Map<String, String> configuration;
    
    // Constructors
    public WidgetDto() {}
    
    public WidgetDto(String type, String label) {
        this.type = type;
        this.label = label;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Map<String, String> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }
}
