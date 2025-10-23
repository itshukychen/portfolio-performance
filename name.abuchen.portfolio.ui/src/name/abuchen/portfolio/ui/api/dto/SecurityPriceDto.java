package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for Security Price serialization.
 * Contains historical price information for API responses.
 */
public class SecurityPriceDto {
    
    private LocalDate date;
    private double value;
    
    // Constructors
    public SecurityPriceDto() {}
    
    public SecurityPriceDto(LocalDate date, double value) {
        this.date = date;
        this.value = value;
    }
    
    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
}

