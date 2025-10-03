package name.abuchen.portfolio.api.data;

/**
 * Simplified DataSeries for API usage.
 * 
 * This class provides the minimal interface needed for widget data generation
 * without the full UI dependencies.
 */
public class DataSeries {
    
    private final String id;
    private final String label;
    private final String type;
    
    public DataSeries(String id, String label, String type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "DataSeries{id='" + id + "', label='" + label + "', type='" + type + "'}";
    }
}
