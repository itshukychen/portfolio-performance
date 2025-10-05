package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDate;

/**
 * DTO for ReportingPeriod serialization.
 */
public class ReportingPeriodDto {
    
    private String code;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}

