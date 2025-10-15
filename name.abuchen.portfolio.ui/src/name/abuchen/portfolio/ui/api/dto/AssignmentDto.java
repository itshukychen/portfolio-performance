package name.abuchen.portfolio.ui.api.dto;

/**
 * Data Transfer Object for Classification Assignment serialization.
 * Contains assignment information for API responses.
 */
public class AssignmentDto {
    
    private String investmentVehicleUuid;
    private String investmentVehicleName;
    private int weight;
    private int rank;
    
    // Constructors
    public AssignmentDto() {}
    
    public AssignmentDto(String investmentVehicleUuid, String investmentVehicleName, int weight) {
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
}

