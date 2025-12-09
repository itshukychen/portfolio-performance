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
    private Double weight;
    private int rank;
    private String key;
    private Double proportion; // Actual % - proportion of parent classification
    private Double actualValue; // Actual value in base currency
    private Double targetValue; // Target value in base currency
    private Double actualProportion; // Actual proportion as percentage of total
    private Double targetProportion; // Target proportion as percentage of total
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
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
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
    
    public Double getProportion() {
        return proportion;
    }
    
    public void setProportion(Double proportion) {
        this.proportion = proportion;
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
    
    public Double getActualValue() {
        return actualValue;
    }
    
    public void setActualValue(Double actualValue) {
        this.actualValue = actualValue;
    }
    
    public Double getTargetValue() {
        return targetValue;
    }
    
    public void setTargetValue(Double targetValue) {
        this.targetValue = targetValue;
    }
    
    public Double getActualProportion() {
        return actualProportion;
    }
    
    public void setActualProportion(Double actualProportion) {
        this.actualProportion = actualProportion;
    }
    
    public Double getTargetProportion() {
        return targetProportion;
    }
    
    public void setTargetProportion(Double targetProportion) {
        this.targetProportion = targetProportion;
    }
}

