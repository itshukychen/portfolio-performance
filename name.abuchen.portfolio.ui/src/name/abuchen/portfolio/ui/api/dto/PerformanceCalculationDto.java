package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for portfolio performance calculation.
 * 
 * This DTO represents the breakdown of portfolio performance over a period,
 * showing initial value, various gains/losses, fees, taxes, transfers, and final value.
 */
public class PerformanceCalculationDto {
    
    @JsonProperty("initialValue")
    private MoneyValueDto initialValue;
    
    @JsonProperty("initialValueDate")
    private LocalDate initialValueDate;
    
    @JsonProperty("capitalGains")
    private MoneyValueDto capitalGains;
    
    @JsonProperty("forexCapitalGains")
    private MoneyValueDto forexCapitalGains;
    
    @JsonProperty("realizedCapitalGains")
    private MoneyValueDto realizedCapitalGains;
    
    @JsonProperty("earnings")
    private MoneyValueDto earnings;
    
    @JsonProperty("fees")
    private MoneyValueDto fees;
    
    @JsonProperty("taxes")
    private MoneyValueDto taxes;
    
    @JsonProperty("cashCurrencyGains")
    private MoneyValueDto cashCurrencyGains;
    
    @JsonProperty("performanceNeutralTransfers")
    private MoneyValueDto performanceNeutralTransfers;
    
    @JsonProperty("finalValue")
    private MoneyValueDto finalValue;
    
    @JsonProperty("finalValueDate")
    private LocalDate finalValueDate;
    
    @JsonProperty("currencyCode")
    private String currencyCode;
    
    @JsonProperty("costMethod")
    private String costMethod; // "FIFO" or "MOVING_AVERAGE"
    
    @JsonProperty("accumulatedPerformance")
    private AccumulatedPerformanceDto accumulatedPerformance;
    
    // Constructors
    public PerformanceCalculationDto() {}
    
    // Getters and Setters
    public MoneyValueDto getInitialValue() {
        return initialValue;
    }
    
    public void setInitialValue(MoneyValueDto initialValue) {
        this.initialValue = initialValue;
    }
    
    public LocalDate getInitialValueDate() {
        return initialValueDate;
    }
    
    public void setInitialValueDate(LocalDate initialValueDate) {
        this.initialValueDate = initialValueDate;
    }
    
    public MoneyValueDto getCapitalGains() {
        return capitalGains;
    }
    
    public void setCapitalGains(MoneyValueDto capitalGains) {
        this.capitalGains = capitalGains;
    }
    
    public MoneyValueDto getForexCapitalGains() {
        return forexCapitalGains;
    }
    
    public void setForexCapitalGains(MoneyValueDto forexCapitalGains) {
        this.forexCapitalGains = forexCapitalGains;
    }
    
    public MoneyValueDto getRealizedCapitalGains() {
        return realizedCapitalGains;
    }
    
    public void setRealizedCapitalGains(MoneyValueDto realizedCapitalGains) {
        this.realizedCapitalGains = realizedCapitalGains;
    }
    
    public MoneyValueDto getEarnings() {
        return earnings;
    }
    
    public void setEarnings(MoneyValueDto earnings) {
        this.earnings = earnings;
    }
    
    public MoneyValueDto getFees() {
        return fees;
    }
    
    public void setFees(MoneyValueDto fees) {
        this.fees = fees;
    }
    
    public MoneyValueDto getTaxes() {
        return taxes;
    }
    
    public void setTaxes(MoneyValueDto taxes) {
        this.taxes = taxes;
    }
    
    public MoneyValueDto getCashCurrencyGains() {
        return cashCurrencyGains;
    }
    
    public void setCashCurrencyGains(MoneyValueDto cashCurrencyGains) {
        this.cashCurrencyGains = cashCurrencyGains;
    }
    
    public MoneyValueDto getPerformanceNeutralTransfers() {
        return performanceNeutralTransfers;
    }
    
    public void setPerformanceNeutralTransfers(MoneyValueDto performanceNeutralTransfers) {
        this.performanceNeutralTransfers = performanceNeutralTransfers;
    }
    
    public MoneyValueDto getFinalValue() {
        return finalValue;
    }
    
    public void setFinalValue(MoneyValueDto finalValue) {
        this.finalValue = finalValue;
    }
    
    public LocalDate getFinalValueDate() {
        return finalValueDate;
    }
    
    public void setFinalValueDate(LocalDate finalValueDate) {
        this.finalValueDate = finalValueDate;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public String getCostMethod() {
        return costMethod;
    }
    
    public void setCostMethod(String costMethod) {
        this.costMethod = costMethod;
    }
    
    public AccumulatedPerformanceDto getAccumulatedPerformance() {
        return accumulatedPerformance;
    }
    
    public void setAccumulatedPerformance(AccumulatedPerformanceDto accumulatedPerformance) {
        this.accumulatedPerformance = accumulatedPerformance;
    }
    
    /**
     * Inner class for representing accumulated performance percentages.
     */
    public static class AccumulatedPerformanceDto {
        @JsonProperty("total")
        private double total;
        
        @JsonProperty("totalAnnualized")
        private double totalAnnualized;
        
        @JsonProperty("unrealizedCapitalGains")
        private double unrealizedCapitalGains;
        
        @JsonProperty("unrealizedCapitalGainsAnnualized")
        private double unrealizedCapitalGainsAnnualized;
        
        @JsonProperty("realizedCapitalGains")
        private double realizedCapitalGains;
        
        @JsonProperty("realizedCapitalGainsAnnualized")
        private double realizedCapitalGainsAnnualized;
        
        @JsonProperty("forexGains")
        private double forexGains;
        
        @JsonProperty("forexGainsAnnualized")
        private double forexGainsAnnualized;
        
        @JsonProperty("earnings")
        private double earnings;
        
        @JsonProperty("earningsAnnualized")
        private double earningsAnnualized;
        
        public AccumulatedPerformanceDto() {}
        
        public double getTotal() {
            return total;
        }
        
        public void setTotal(double total) {
            this.total = total;
        }
        
        public double getTotalAnnualized() {
            return totalAnnualized;
        }
        
        public void setTotalAnnualized(double totalAnnualized) {
            this.totalAnnualized = totalAnnualized;
        }
        
        public double getUnrealizedCapitalGains() {
            return unrealizedCapitalGains;
        }
        
        public void setUnrealizedCapitalGains(double unrealizedCapitalGains) {
            this.unrealizedCapitalGains = unrealizedCapitalGains;
        }
        
        public double getUnrealizedCapitalGainsAnnualized() {
            return unrealizedCapitalGainsAnnualized;
        }
        
        public void setUnrealizedCapitalGainsAnnualized(double unrealizedCapitalGainsAnnualized) {
            this.unrealizedCapitalGainsAnnualized = unrealizedCapitalGainsAnnualized;
        }
        
        public double getRealizedCapitalGains() {
            return realizedCapitalGains;
        }
        
        public void setRealizedCapitalGains(double realizedCapitalGains) {
            this.realizedCapitalGains = realizedCapitalGains;
        }
        
        public double getRealizedCapitalGainsAnnualized() {
            return realizedCapitalGainsAnnualized;
        }
        
        public void setRealizedCapitalGainsAnnualized(double realizedCapitalGainsAnnualized) {
            this.realizedCapitalGainsAnnualized = realizedCapitalGainsAnnualized;
        }
        
        public double getEarnings() {
            return earnings;
        }
        
        public void setEarnings(double earnings) {
            this.earnings = earnings;
        }
        
        public double getEarningsAnnualized() {
            return earningsAnnualized;
        }
        
        public void setEarningsAnnualized(double earningsAnnualized) {
            this.earningsAnnualized = earningsAnnualized;
        }
        
        public double getForexGains() {
            return forexGains;
        }
        
        public void setForexGains(double forexGains) {
            this.forexGains = forexGains;
        }
        
        public double getForexGainsAnnualized() {
            return forexGainsAnnualized;
        }
        
        public void setForexGainsAnnualized(double forexGainsAnnualized) {
            this.forexGainsAnnualized = forexGainsAnnualized;
        }
    }
    
    /**
     * Inner class for representing money values with formatted string and raw amount.
     */
    public static class MoneyValueDto {
        @JsonProperty("formatted")
        private String formatted;
        
        @JsonProperty("rawValue")
        private double rawValue;
        
        public MoneyValueDto() {}
        
        public MoneyValueDto(String formatted, double rawValue) {
            this.formatted = formatted;
            this.rawValue = rawValue;
        }
        
        public String getFormatted() {
            return formatted;
        }
        
        public void setFormatted(String formatted) {
            this.formatted = formatted;
        }
        
        public double getRawValue() {
            return rawValue;
        }
        
        public void setRawValue(double rawValue) {
            this.rawValue = rawValue;
        }
    }
}

