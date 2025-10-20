package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for currency conversion rates.
 * Contains the exchange rate information for converting a currency to the base currency.
 */
public class CurrencyConversionDto {
    
    private String fromCurrency;
    private String toCurrency;
    private double rate;
    private LocalDate date;
    
    // Constructors
    public CurrencyConversionDto() {}
    
    public CurrencyConversionDto(String fromCurrency, String toCurrency, double rate, LocalDate date) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.date = date;
    }
    
    // Getters and Setters
    public String getFromCurrency() {
        return fromCurrency;
    }
    
    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }
    
    public String getToCurrency() {
        return toCurrency;
    }
    
    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }
    
    public double getRate() {
        return rate;
    }
    
    public void setRate(double rate) {
        this.rate = rate;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
}

