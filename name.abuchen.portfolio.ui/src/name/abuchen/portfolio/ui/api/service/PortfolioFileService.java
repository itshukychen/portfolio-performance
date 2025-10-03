package name.abuchen.portfolio.ui.api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.ui.api.dto.DashboardDto;
import name.abuchen.portfolio.ui.api.dto.PortfolioFileInfo;
import name.abuchen.portfolio.ui.api.util.DashboardConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Dashboard;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Service for handling portfolio file operations.
 * 
 * This service provides functionality to open, load, and manage portfolio files
 * using the Portfolio Performance core modules.
 */
public class PortfolioFileService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioFileService.class);
    
    // Portfolio directory configuration
    private final Path portfolioDirectory;
    
    // Cache for loaded clients to avoid reloading the same file
    private final Map<String, Client> clientCache = new ConcurrentHashMap<>();
    
    /**
     * Constructor that initializes the portfolio directory from environment variable or system property.
     */
    public PortfolioFileService() {
        String portfolioDir = System.getenv("PORTFOLIO_DIR");
        if (portfolioDir == null) {
            portfolioDir = System.getProperty("portfolio.dir");
        }
        if (portfolioDir == null) {
            // Default to current working directory
            portfolioDir = System.getProperty("user.dir");
        }
        
        this.portfolioDirectory = Paths.get(portfolioDir).toAbsolutePath();
        logger.info("Portfolio directory set to: {}", this.portfolioDirectory);
        
        // Ensure directory exists
        if (!Files.exists(this.portfolioDirectory)) {
            try {
                Files.createDirectories(this.portfolioDirectory);
                logger.info("Created portfolio directory: {}", this.portfolioDirectory);
            } catch (IOException e) {
                logger.error("Failed to create portfolio directory: {}", this.portfolioDirectory, e);
            }
        }
    }
    
    /**
     * Constructor with explicit portfolio directory.
     */
    public PortfolioFileService(String portfolioDir) {
        this.portfolioDirectory = Paths.get(portfolioDir).toAbsolutePath();
        logger.info("Portfolio directory set to: {}", this.portfolioDirectory);
        
        // Ensure directory exists
        if (!Files.exists(this.portfolioDirectory)) {
            try {
                Files.createDirectories(this.portfolioDirectory);
                logger.info("Created portfolio directory: {}", this.portfolioDirectory);
            } catch (IOException e) {
                logger.error("Failed to create portfolio directory: {}", this.portfolioDirectory, e);
            }
        }
    }
    
    /**
     * Open and load a portfolio file from the given relative path.
     * The path is resolved against the portfolio directory.
     * 
     * @param relativePath The relative path to the portfolio file (relative to portfolio directory)
     * @param password Optional password for encrypted files
     * @return PortfolioFileInfo containing file information
     * @throws IOException if the file cannot be read
     * @throws FileNotFoundException if the file does not exist
     */
    public PortfolioFileInfo openFile(String relativePath, char[] password) throws IOException {
        logger.info("Opening portfolio file: {} (relative to: {})", relativePath, portfolioDirectory);
        
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        // Resolve the relative path against the portfolio directory
        Path path = portfolioDirectory.resolve(relativePath).normalize();
        
        // Security check: ensure the resolved path is within the portfolio directory
        if (!path.startsWith(portfolioDirectory)) {
            throw new SecurityException("Access denied: path outside portfolio directory");
        }
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Portfolio file not found: " + relativePath + " (resolved to: " + path + ")");
        }
        
        File file = path.toFile();
        
        // Check if file is encrypted and password is required
        if (ClientFactory.isEncrypted(file) && password == null) {
            throw new IOException("Password required for encrypted file: " + relativePath);
        }
        
        // Check cache first using file ID as key
        String fileId = generateFileId(relativePath);
        Client client = clientCache.get(fileId);
        
        try {
            // Load the actual portfolio file using ClientFactory
            logger.info("Loading portfolio file: {} with ClientFactory", path);
            
            // Create a minimal progress monitor
            MinimalProgressMonitor monitor = new MinimalProgressMonitor();
            
            // Load the client using ClientFactory with the provided password
            client = ClientFactory.load(file, password, monitor);
            
            clientCache.put(fileId, client);
            logger.info("Successfully loaded portfolio file: {}", relativePath);
            
        } catch (Exception e) {
            logger.error("Failed to load portfolio file: {}", relativePath, e);
            throw new IOException("Failed to process portfolio file: " + e.getMessage(), e);
        }
        
        // Create file info
        PortfolioFileInfo fileInfo = new PortfolioFileInfo();
        fileInfo.setId(generateFileId(relativePath));
        fileInfo.setName(extractName(relativePath));
        fileInfo.setLastModified(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(file.lastModified()),
            ZoneId.systemDefault()
        ));
        fileInfo.setEncrypted(ClientFactory.isEncrypted(file));
        
        // Add client info if available
        if (client != null) {
            fileInfo.setBaseCurrency(client.getBaseCurrency());
            fileInfo.setVersion(client.getFileVersionAfterRead());
            
            // Count entities
            fileInfo.setSecuritiesCount(client.getSecurities().size());
            fileInfo.setAccountsCount(client.getAccounts().size());
            fileInfo.setPortfoliosCount(client.getPortfolios().size());
            fileInfo.setTransactionsCount(client.getAllTransactions().size());
            
            fileInfo.setClientLoaded(true);
            fileInfo.setClientInfo("Client loaded successfully");
            
            // Add dashboard information
            List<Dashboard> dashboards = client.getDashboards().collect(Collectors.toList());
            logger.info("Found {} dashboards in portfolio", dashboards.size());
            for (Dashboard dashboard : dashboards) {
                logger.info("Dashboard: id={}, name={}, columns={}", 
                    dashboard.getId(), dashboard.getName(), dashboard.getColumns().size());
            }
            fileInfo.setDashboards(DashboardConverter.toDtoList(dashboards));
            logger.info("Set {} dashboards in fileInfo", fileInfo.getDashboards() != null ? fileInfo.getDashboards().size() : 0);
            
            // Force a simple test dashboard to see if serialization works
            if (fileInfo.getDashboards() == null || fileInfo.getDashboards().isEmpty()) {
                logger.info("No dashboards found, creating test dashboard");
                List<DashboardDto> testDashboards = new ArrayList<>();
                DashboardDto testDashboard = new DashboardDto();
                testDashboard.setId("test-id");
                testDashboard.setName("Test Dashboard");
                testDashboards.add(testDashboard);
                fileInfo.setDashboards(testDashboards);
                logger.info("Set test dashboard in fileInfo");
            }
        } else {
            // Set default values when client is not available
            fileInfo.setBaseCurrency("EUR"); // Default currency
            fileInfo.setVersion(0); // Unknown version
            fileInfo.setSecuritiesCount(0);
            fileInfo.setAccountsCount(0);
            fileInfo.setPortfoliosCount(0);
            fileInfo.setTransactionsCount(0);
            
            fileInfo.setClientLoaded(false);
            fileInfo.setClientInfo("Client creation failed - OSGi dependencies not available");
        }
        
        return fileInfo;
    }
    
    /**
     * Get basic information about a portfolio file without loading it.
     * 
     * @param relativePath The relative path to the portfolio file
     * @return PortfolioFileInfo with basic file information
     * @throws IOException if the file cannot be accessed
     */
    public PortfolioFileInfo getFileInfo(String relativePath) throws IOException {
        logger.info("Getting file info for: {} (relative to: {})", relativePath, portfolioDirectory);
        
        // Resolve the relative path against the portfolio directory
        Path path = portfolioDirectory.resolve(relativePath).normalize();
        
        // Security check: ensure the resolved path is within the portfolio directory
        if (!path.startsWith(portfolioDirectory)) {
            throw new SecurityException("Access denied: path outside portfolio directory");
        }
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Portfolio file not found: " + relativePath + " (resolved to: " + path + ")");
        }
        
        File file = path.toFile();
        
        PortfolioFileInfo fileInfo = new PortfolioFileInfo();
        fileInfo.setId(generateFileId(relativePath));
        fileInfo.setName(extractName(relativePath));
        fileInfo.setLastModified(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(file.lastModified()),
            ZoneId.systemDefault()
        ));
        fileInfo.setEncrypted(ClientFactory.isEncrypted(file));
        
        return fileInfo;
    }
    
    /**
     * List all portfolio files in the portfolio directory.
     * 
     * @return List of PortfolioFileInfo for all portfolio files found
     * @throws IOException if the directory cannot be read
     */
    public List<PortfolioFileInfo> listPortfolioFiles() throws IOException {
        logger.info("Listing portfolio files in directory: {}", portfolioDirectory);
        
        List<PortfolioFileInfo> files = new ArrayList<>();
        
        if (!Files.exists(portfolioDirectory)) {
            logger.warn("Portfolio directory does not exist: {}", portfolioDirectory);
            return files;
        }
        
        try (Stream<Path> paths = Files.walk(portfolioDirectory)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isPortfolioFile)
                 .forEach(path -> {
                     try {
                         PortfolioFileInfo fileInfo = new PortfolioFileInfo();
                         File file = path.toFile();
                         
                         // Calculate relative path
                         String relativePath = portfolioDirectory.relativize(path).toString();
                         
                         // Set file ID and name
                         fileInfo.setId(generateFileId(relativePath));
                         fileInfo.setName(extractName(relativePath));
                         fileInfo.setLastModified(LocalDateTime.ofInstant(
                             java.time.Instant.ofEpochMilli(file.lastModified()),
                             ZoneId.systemDefault()
                         ));
                         fileInfo.setEncrypted(ClientFactory.isEncrypted(file));
                         
                         files.add(fileInfo);
                         
                     } catch (Exception e) {
                         logger.warn("Error processing file: {}", path, e);
                     }
                 });
        }
        
        logger.info("Found {} portfolio files", files.size());
        return files;
    }
    
    /**
     * Check if a file is a portfolio file based on its extension.
     * 
     * @param path The file path to check
     * @return true if the file appears to be a portfolio file
     */
    private boolean isPortfolioFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".xml") || fileName.endsWith(".portfolio");
    }
    
    /**
     * Get the portfolio directory path.
     * 
     * @return The portfolio directory path
     */
    public Path getPortfolioDirectory() {
        return portfolioDirectory;
    }
    
    /**
     * Generate a unique file ID by hashing the full relative path.
     * 
     * @param relativePath The relative path to hash
     * @return A unique file ID (SHA-256 hash)
     */
    private String generateFileId(String relativePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(relativePath.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            logger.error("Failed to generate file ID for: {}", relativePath, e);
            // Fallback to a simple hash
            return String.valueOf(relativePath.hashCode());
        }
    }
    
    /**
     * Extract the name from a file path (filename without extension).
     * 
     * @param filePath The file path
     * @return The name without extension
     */
    private String extractName(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    /**
     * Find a file by its ID by scanning the portfolio directory.
     * 
     * @param fileId The file ID to search for
     * @return The relative path of the file if found
     * @throws FileNotFoundException if no file with the given ID is found
     */
    public String findFileById(String fileId) throws FileNotFoundException {
        logger.info("Searching for file with ID: {}", fileId);
        
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("File ID cannot be null or empty");
        }
        
        if (!Files.exists(portfolioDirectory)) {
            throw new FileNotFoundException("Portfolio directory does not exist: " + portfolioDirectory);
        }
        
        try (Stream<Path> paths = Files.walk(portfolioDirectory)) {
            return paths.filter(Files::isRegularFile)
                       .filter(this::isPortfolioFile)
                       .map(path -> portfolioDirectory.relativize(path).toString())
                       .filter(relativePath -> {
                           String computedId = generateFileId(relativePath);
                           return fileId.equals(computedId);
                       })
                       .findFirst()
                       .orElseThrow(() -> new FileNotFoundException("No file found with ID: " + fileId));
                       
        } catch (IOException e) {
            logger.error("Error scanning portfolio directory for file ID: {}", fileId, e);
            throw new FileNotFoundException("Error scanning portfolio directory: " + e.getMessage());
        }
    }
    
    /**
     * Open and load a portfolio file by its ID.
     * 
     * @param fileId The file ID to open
     * @param password Optional password for encrypted files
     * @return PortfolioFileInfo containing file information
     * @throws IOException if the file cannot be read
     * @throws FileNotFoundException if the file does not exist
     */
    public PortfolioFileInfo openFileById(String fileId, char[] password) throws IOException {
        logger.info("Opening portfolio file by ID: {}", fileId);
        
        // First find the file by ID
        String relativePath = findFileById(fileId);
        
        // Then open it using the existing method
        return openFile(relativePath, password);
    }
    
    /**
     * Get a cached Client by portfolio ID.
     * This method only returns clients that are already in the cache.
     * 
     * @param portfolioId The portfolio ID (file ID)
     * @return The cached Client or null if not found in cache
     */
    public Client getPortfolio(String portfolioId) {
        logger.info("Getting cached portfolio for ID: {}", portfolioId);
        
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            logger.warn("Portfolio ID cannot be null or empty");
            return null;
        }
        
        // Use the portfolio ID directly as the cache key (without password for now)
        Client client = clientCache.get(portfolioId);
        
        if (client != null) {
            logger.info("Found cached client for portfolio ID: {}", portfolioId);
        } else {
            logger.info("No cached client found for portfolio ID: {}", portfolioId);
        }
        
        return client;
    }
    
    /**
     * Clear the client cache.
     */
    public void clearCache() {
        logger.info("Clearing client cache");
        clientCache.clear();
    }
    
    
    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedClients", clientCache.size());
        stats.put("cacheKeys", clientCache.keySet());
        return stats;
    }
    
    
    /**
     * Cleanup method called when the application shuts down.
     * This ensures proper cleanup of resources.
     */
    public void shutdown() {
        logger.info("Shutting down PortfolioFileService - clearing client cache");
        clientCache.clear();
        logger.info("PortfolioFileService shutdown complete");
    }
    
    /**
     * Minimal progress monitor implementation that implements Eclipse's IProgressMonitor interface.
     */
    private static class MinimalProgressMonitor implements IProgressMonitor {
        private boolean cancelled = false;
        
        @Override
        public void beginTask(String name, int totalWork) {
            logger.debug("Starting task: {} (total work: {})", name, totalWork);
        }
        
        @Override
        public void done() {
            logger.debug("Task completed");
        }
        
        @Override
        public void internalWorked(double work) {
            // No-op for now
        }
        
        @Override
        public boolean isCanceled() {
            return cancelled;
        }
        
        @Override
        public void setCanceled(boolean value) {
            this.cancelled = value;
        }
        
        @Override
        public void setTaskName(String name) {
            logger.debug("Task: {}", name);
        }
        
        @Override
        public void subTask(String name) {
            logger.debug("Subtask: {}", name);
        }
        
        @Override
        public void worked(int work) {
            // No-op for now
        }
    }
}

