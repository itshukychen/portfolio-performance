package name.abuchen.portfolio.api.dto;

import java.util.List;

/**
 * Data Transfer Object for Dashboard Column serialization.
 * Represents a column in a dashboard layout.
 */
public class ColumnDto {
    
    private int weight;
    private List<WidgetDto> widgets;
    
    // Constructors
    public ColumnDto() {}
    
    public ColumnDto(int weight) {
        this.weight = weight;
    }
    
    // Getters and Setters
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public List<WidgetDto> getWidgets() {
        return widgets;
    }
    
    public void setWidgets(List<WidgetDto> widgets) {
        this.widgets = widgets;
    }
}
