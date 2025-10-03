package name.abuchen.portfolio.api.dto;

/**
 * Request DTO for opening a portfolio file.
 */
public class OpenFileRequest {
    
    private String fileId;
    private char[] password;
    
    // Constructors
    public OpenFileRequest() {}
    
    public OpenFileRequest(String fileId) {
        this.fileId = fileId;
    }
    
    public OpenFileRequest(String fileId, char[] password) {
        this.fileId = fileId;
        this.password = password;
    }
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    // Legacy getter for backward compatibility
    public String getFilePath() {
        return fileId;
    }
    
    // Legacy setter for backward compatibility
    public void setFilePath(String filePath) {
        this.fileId = filePath;
    }
    
    public char[] getPassword() {
        return password;
    }
    
    public void setPassword(char[] password) {
        this.password = password;
    }
}
