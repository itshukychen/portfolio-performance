package name.abuchen.portfolio.ui.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Dashboard serialization.
 * Contains dashboard information for API responses.
 */
public class DashboardDto {
    
    private String id;
    private String name;
    private List<ColumnDto> columns;
    private Map<String, String> configuration;
    
    // Constructors
    public DashboardDto() {}
    
    public DashboardDto(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<ColumnDto> getColumns() {
        return columns;
    }
    
    public void setColumns(List<ColumnDto> columns) {
        this.columns = columns;
    }
    
    public Map<String, String> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }
}
