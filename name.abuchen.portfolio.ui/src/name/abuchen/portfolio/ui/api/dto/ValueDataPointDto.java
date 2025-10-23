package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for value data points over time.
 * Used for representing account or portfolio values at specific dates.
 */
public class ValueDataPointDto {
    
    private LocalDate date;
    private double value;
    
    // Constructors
    public ValueDataPointDto() {}
    
    public ValueDataPointDto(LocalDate date, double value) {
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

