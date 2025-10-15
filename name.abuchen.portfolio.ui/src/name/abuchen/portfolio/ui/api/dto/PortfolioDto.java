package name.abuchen.portfolio.ui.api.dto;

import java.time.Instant;

/**
 * Data Transfer Object for Portfolio serialization.
 * Contains portfolio information for API responses.
 */
public class PortfolioDto {
    
    private String uuid;
    private String name;
    private String note;
    private boolean isRetired;
    private String referenceAccountUuid;
    private String referenceAccountName;
    private int transactionsCount;
    private double currentValue;
    private Instant updatedAt;
    
    // Constructors
    public PortfolioDto() {}
    
    public PortfolioDto(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
    
    // Getters and Setters
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public boolean isRetired() {
        return isRetired;
    }
    
    public void setRetired(boolean retired) {
        isRetired = retired;
    }
    
    public String getReferenceAccountUuid() {
        return referenceAccountUuid;
    }
    
    public void setReferenceAccountUuid(String referenceAccountUuid) {
        this.referenceAccountUuid = referenceAccountUuid;
    }
    
    public String getReferenceAccountName() {
        return referenceAccountName;
    }
    
    public void setReferenceAccountName(String referenceAccountName) {
        this.referenceAccountName = referenceAccountName;
    }
    
    public int getTransactionsCount() {
        return transactionsCount;
    }
    
    public void setTransactionsCount(int transactionsCount) {
        this.transactionsCount = transactionsCount;
    }
    
    public double getCurrentValue() {
        return currentValue;
    }
    
    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

