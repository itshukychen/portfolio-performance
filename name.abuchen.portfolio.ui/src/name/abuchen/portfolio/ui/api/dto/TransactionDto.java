package name.abuchen.portfolio.ui.api.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Transaction serialization.
 * Represents both Account and Portfolio transactions for API responses.
 */
public class TransactionDto {
    
    private String uuid;
    private LocalDateTime dateTime;
    private String type;
    private String transactionType; // "ACCOUNT" or "PORTFOLIO"
    private String currencyCode;
    private double amount;
    private String securityUuid;
    private String securityName;
    private double shares;
    private String note;
    private String source;
    private String ownerUuid;
    private String ownerName;
    private Instant updatedAt;
    
    // Constructors
    public TransactionDto() {}
    
    // Getters and Setters
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getSecurityUuid() {
        return securityUuid;
    }
    
    public void setSecurityUuid(String securityUuid) {
        this.securityUuid = securityUuid;
    }
    
    public String getSecurityName() {
        return securityName;
    }
    
    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }
    
    public double getShares() {
        return shares;
    }
    
    public void setShares(double shares) {
        this.shares = shares;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getOwnerUuid() {
        return ownerUuid;
    }
    
    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

