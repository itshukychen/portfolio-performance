package name.abuchen.portfolio.api.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for hello endpoint.
 */
public class HelloResponse {
    
    private String message;
    private LocalDateTime timestamp;
    private String version;
    private String status;
    
    // Constructors
    public HelloResponse() {}
    
    public HelloResponse(String message, LocalDateTime timestamp, String version, String status) {
        this.message = message;
        this.timestamp = timestamp;
        this.version = version;
        this.status = status;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
