package name.abuchen.portfolio.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for health endpoint.
 */
public class HealthResponse {
    
    private String status;
    private LocalDateTime timestamp;
    private String service;
    
    // Constructors
    public HealthResponse() {}
    
    public HealthResponse(String status, LocalDateTime timestamp, String service) {
        this.status = status;
        this.timestamp = timestamp;
        this.service = service;
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
}
