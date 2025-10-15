package name.abuchen.portfolio.ui.api.dto;

import java.util.List;

/**
 * Data Transfer Object for Taxonomy serialization.
 * Contains taxonomy information for API responses.
 */
public class TaxonomyDto {
    
    private String id;
    private String name;
    private String source;
    private List<String> dimensions;
    private int classificationsCount;
    private int height;
    private ClassificationDto root;
    
    // Constructors
    public TaxonomyDto() {}
    
    public TaxonomyDto(String id, String name) {
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
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public List<String> getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }
    
    public int getClassificationsCount() {
        return classificationsCount;
    }
    
    public void setClassificationsCount(int classificationsCount) {
        this.classificationsCount = classificationsCount;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public ClassificationDto getRoot() {
        return root;
    }
    
    public void setRoot(ClassificationDto root) {
        this.root = root;
    }
}

