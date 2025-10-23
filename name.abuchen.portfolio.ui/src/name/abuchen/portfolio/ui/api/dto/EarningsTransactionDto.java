package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Earnings Transaction serialization.
 * Represents earnings transactions (dividends, interest) with tax and fee details.
 */
public class EarningsTransactionDto {
    
    private String uuid;
    private LocalDateTime dateTime;
    private String type; // DIVIDENDS, INTEREST, INTEREST_CHARGE
    private String currencyCode;
    private double amount; // Net amount after taxes and fees
    private double grossValue; // Gross amount before taxes and fees
    private double taxes;
    private double fees;
    
    // Converted amounts in client base currency
    private String baseCurrency;
    private double amountInBaseCurrency;
    private double grossValueInBaseCurrency;
    private double taxesInBaseCurrency;
    private double feesInBaseCurrency;
    
    private String securityUuid;
    private String securityName;
    private String securityIsin;
    private String accountUuid;
    private String accountName;
    private String note;
    private String source;
    
    // Constructors
    public EarningsTransactionDto() {}
    
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
    
    public double getGrossValue() {
        return grossValue;
    }
    
    public void setGrossValue(double grossValue) {
        this.grossValue = grossValue;
    }
    
    public double getTaxes() {
        return taxes;
    }
    
    public void setTaxes(double taxes) {
        this.taxes = taxes;
    }
    
    public double getFees() {
        return fees;
    }
    
    public void setFees(double fees) {
        this.fees = fees;
    }
    
    public String getBaseCurrency() {
        return baseCurrency;
    }
    
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
    
    public double getAmountInBaseCurrency() {
        return amountInBaseCurrency;
    }
    
    public void setAmountInBaseCurrency(double amountInBaseCurrency) {
        this.amountInBaseCurrency = amountInBaseCurrency;
    }
    
    public double getGrossValueInBaseCurrency() {
        return grossValueInBaseCurrency;
    }
    
    public void setGrossValueInBaseCurrency(double grossValueInBaseCurrency) {
        this.grossValueInBaseCurrency = grossValueInBaseCurrency;
    }
    
    public double getTaxesInBaseCurrency() {
        return taxesInBaseCurrency;
    }
    
    public void setTaxesInBaseCurrency(double taxesInBaseCurrency) {
        this.taxesInBaseCurrency = taxesInBaseCurrency;
    }
    
    public double getFeesInBaseCurrency() {
        return feesInBaseCurrency;
    }
    
    public void setFeesInBaseCurrency(double feesInBaseCurrency) {
        this.feesInBaseCurrency = feesInBaseCurrency;
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
    
    public String getSecurityIsin() {
        return securityIsin;
    }
    
    public void setSecurityIsin(String securityIsin) {
        this.securityIsin = securityIsin;
    }
    
    public String getAccountUuid() {
        return accountUuid;
    }
    
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
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
}

