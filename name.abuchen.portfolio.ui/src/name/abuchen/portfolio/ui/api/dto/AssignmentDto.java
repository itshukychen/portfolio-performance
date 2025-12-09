package name.abuchen.portfolio.ui.api.dto;

/**
 * Data Transfer Object for Classification Assignment serialization.
 * Contains assignment information for API responses.
 */
public class AssignmentDto {
    
    private String investmentVehicleUuid;
    private String investmentVehicleName;
    private Double weight;
    private int rank;
    private Double proportion; // Actual % - proportion of parent classification
    private Double actualValue; // Actual value in base currency
    private Double targetValue; // Target value in base currency
    private Double actualProportion; // Actual proportion as percentage of total
    private Double targetProportion; // Target proportion as percentage of total
    
    // Constructors
    public AssignmentDto() {}
    
    public AssignmentDto(String investmentVehicleUuid, String investmentVehicleName, Double weight) {
        this.investmentVehicleUuid = investmentVehicleUuid;
        this.investmentVehicleName = investmentVehicleName;
        this.weight = weight;
    }
    
    // Getters and Setters
    public String getInvestmentVehicleUuid() {
        return investmentVehicleUuid;
    }
    
    public void setInvestmentVehicleUuid(String investmentVehicleUuid) {
        this.investmentVehicleUuid = investmentVehicleUuid;
    }
    
    public String getInvestmentVehicleName() {
        return investmentVehicleName;
    }
    
    public void setInvestmentVehicleName(String investmentVehicleName) {
        this.investmentVehicleName = investmentVehicleName;
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
    
    public Double getProportion() {
        return proportion;
    }
    
    public void setProportion(Double proportion) {
        this.proportion = proportion;
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

