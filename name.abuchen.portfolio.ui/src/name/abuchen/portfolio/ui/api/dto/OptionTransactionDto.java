package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Options Transaction serialization.
 * Represents options trading transactions with parsed option details.
 */
public class OptionTransactionDto {
    
    private String uuid;
    private LocalDateTime transactionTime;
    private String type; // OPTIONS_SELL, EXPOSURE_FEE
    private String currencyCode;
    
    // Option contract details
    private int numberOfContracts; // shares / 100
    private double pricePerContract; // quote price per contract
    private double fees;
    private double totalAmount; // Net amount received
    
    // Parsed option information from security name (e.g., "SPXW 13OCT25 6025 P")
    private String underlyingSecurityName; // e.g., "SPX" (without W suffix)
    private String expirationDate; // e.g., "13OCT25"
    private LocalDate parsedExpirationDate; // Parsed date object
    private double strikePrice; // e.g., 6025
    private String optionType; // "P" (Put) or "C" (Call)
    
    // Underlying security price at transaction time
    private Double underlyingSecurityPrice; // SPX price at transaction time
    private String underlyingSecurityCurrency;
    
    // Original security information
    private String securityUuid;
    private String originalSecurityName; // Full name as stored (e.g., "SPXW 13OCT25 6025 P")
    private String securityIsin;
    
    // Account information
    private String accountUuid;
    private String accountName;
    
    // Additional information
    private String note;
    private String source;
    
    // Base currency conversions
    private String baseCurrency;
    private double totalAmountInBaseCurrency;
    private double feesInBaseCurrency;
    private double pricePerContractInBaseCurrency;
    
    // Constructors
    public OptionTransactionDto() {}
    
    // Getters and Setters
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }
    
    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
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
    
    public int getNumberOfContracts() {
        return numberOfContracts;
    }
    
    public void setNumberOfContracts(int numberOfContracts) {
        this.numberOfContracts = numberOfContracts;
    }
    
    public double getPricePerContract() {
        return pricePerContract;
    }
    
    public void setPricePerContract(double pricePerContract) {
        this.pricePerContract = pricePerContract;
    }
    
    public double getFees() {
        return fees;
    }
    
    public void setFees(double fees) {
        this.fees = fees;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getUnderlyingSecurityName() {
        return underlyingSecurityName;
    }
    
    public void setUnderlyingSecurityName(String underlyingSecurityName) {
        this.underlyingSecurityName = underlyingSecurityName;
    }
    
    public String getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public LocalDate getParsedExpirationDate() {
        return parsedExpirationDate;
    }
    
    public void setParsedExpirationDate(LocalDate parsedExpirationDate) {
        this.parsedExpirationDate = parsedExpirationDate;
    }
    
    public double getStrikePrice() {
        return strikePrice;
    }
    
    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }
    
    public String getOptionType() {
        return optionType;
    }
    
    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }
    
    public Double getUnderlyingSecurityPrice() {
        return underlyingSecurityPrice;
    }
    
    public void setUnderlyingSecurityPrice(Double underlyingSecurityPrice) {
        this.underlyingSecurityPrice = underlyingSecurityPrice;
    }
    
    public String getUnderlyingSecurityCurrency() {
        return underlyingSecurityCurrency;
    }
    
    public void setUnderlyingSecurityCurrency(String underlyingSecurityCurrency) {
        this.underlyingSecurityCurrency = underlyingSecurityCurrency;
    }
    
    public String getSecurityUuid() {
        return securityUuid;
    }
    
    public void setSecurityUuid(String securityUuid) {
        this.securityUuid = securityUuid;
    }
    
    public String getOriginalSecurityName() {
        return originalSecurityName;
    }
    
    public void setOriginalSecurityName(String originalSecurityName) {
        this.originalSecurityName = originalSecurityName;
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
    
    public String getBaseCurrency() {
        return baseCurrency;
    }
    
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
    
    public double getTotalAmountInBaseCurrency() {
        return totalAmountInBaseCurrency;
    }
    
    public void setTotalAmountInBaseCurrency(double totalAmountInBaseCurrency) {
        this.totalAmountInBaseCurrency = totalAmountInBaseCurrency;
    }
    
    public double getFeesInBaseCurrency() {
        return feesInBaseCurrency;
    }
    
    public void setFeesInBaseCurrency(double feesInBaseCurrency) {
        this.feesInBaseCurrency = feesInBaseCurrency;
    }
    
    public double getPricePerContractInBaseCurrency() {
        return pricePerContractInBaseCurrency;
    }
    
    public void setPricePerContractInBaseCurrency(double pricePerContractInBaseCurrency) {
        this.pricePerContractInBaseCurrency = pricePerContractInBaseCurrency;
    }
}

