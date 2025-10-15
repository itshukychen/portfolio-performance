package name.abuchen.portfolio.ui.api.dto;

import java.util.List;

/**
 * Data Transfer Object for Classification serialization.
 * Contains classification node information for API responses.
 */
public class ClassificationDto {
    
    private String id;
    private String name;
    private String description;
    private String color;
    private int weight;
    private int rank;
    private String key;
    private List<ClassificationDto> children;
    private List<AssignmentDto> assignments;
    
    // Constructors
    public ClassificationDto() {}
    
    public ClassificationDto(String id, String name) {
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public List<ClassificationDto> getChildren() {
        return children;
    }
    
    public void setChildren(List<ClassificationDto> children) {
        this.children = children;
    }
    
    public List<AssignmentDto> getAssignments() {
        return assignments;
    }
    
    public void setAssignments(List<AssignmentDto> assignments) {
        this.assignments = assignments;
    }
}

