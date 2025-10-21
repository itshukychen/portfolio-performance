package name.abuchen.portfolio.ui.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.api.dto.SecurityDto;

/**
 * REST Controller for security operations.
 * 
 * This controller provides endpoints to manage securities within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/securities")
public class SecurityController extends BaseController {
    
    /**
     * Get all active securities in a portfolio.
     * 
     * This endpoint filters out:
     * - Retired securities
     * - Options contracts (ticker symbols matching pattern: 6 digits + C/P + 8 digits)
     * 
     * @param portfolioId The portfolio ID
     * @return List of active securities (excluding retired and options contracts)
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSecurities(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting all securities for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing securities");
            }
            
            // Get all securities (filter out retired securities and options contracts)
            List<SecurityDto> securities = client.getSecurities().stream()
                .filter(security -> !security.isRetired()) // Skip retired securities
                .filter(this::isNotOptionsContract) // Skip options contracts
                .map(this::convertSecurityToDto)
                .collect(Collectors.toList());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("count", securities.size());
            response.put("securities", securities);
            
            logger.info("Returning {} active securities for portfolio {} (filtered out retired and options contracts)", 
                securities.size(), portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting securities for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get a specific security by UUID.
     * 
     * This endpoint filters out retired securities and options contracts.
     * If the security is retired or an options contract, a 404 will be returned.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Security details (if not retired and not an options contract)
     */
    @GET
    @Path("/{securityUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecurityById(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid) {
        try {
            logger.info("Getting security {} for portfolio {}", securityUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing securities");
            }
            
            // Find the security by UUID (excluding retired securities and options contracts)
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .filter(s -> !s.isRetired()) // Skip retired securities
                .filter(this::isNotOptionsContract) // Skip options contracts
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found or filtered out: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found (or is retired/options contract)");
            }
            
            // Convert to DTO
            SecurityDto securityDto = convertSecurityToDto(security);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("security", securityDto);
            
            logger.info("Returning security {} ({}) for portfolio {}", 
                security.getName(), securityUuid, portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting security {} for portfolio {}: {}", 
                securityUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new security.
     * TODO: Implement security creation
     * 
     * @param portfolioId The portfolio ID
     * @param securityData Security data
     * @return Created security
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSecurity(@PathParam("portfolioId") String portfolioId,
                                   Map<String, Object> securityData) {
        // TODO: Implement security creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security creation not yet implemented");
    }
    
    /**
     * Update an existing security.
     * TODO: Implement security update
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @param securityData Updated security data
     * @return Updated security
     */
    @PUT
    @Path("/{securityUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecurity(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid,
                                   Map<String, Object> securityData) {
        // TODO: Implement security update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security update not yet implemented");
    }
    
    /**
     * Delete a security.
     * TODO: Implement security deletion
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{securityUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSecurity(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("securityUuid") String securityUuid) {
        // TODO: Implement security deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Security deletion not yet implemented");
    }
    
    /**
     * Helper method to check if a security is NOT an options contract.
     * Options contracts have ticker symbols matching pattern: 6 digits + C/P + 8 digits
     * Aligned with PortfolioFileService.loadSecurities()
     */
    private boolean isNotOptionsContract(Security security) {
        String tickerSymbol = security.getTickerSymbol();
        if (tickerSymbol != null && tickerSymbol.replaceAll("\\s+", "").matches(".*\\d{6}[CP]\\d{8}")) {
            return false; // This is an options contract, skip it
        }
        return true; // Not an options contract
    }
    
    /**
     * Helper method to convert Security to SecurityDto.
     * Aligned with PortfolioFileService.loadSecurities()
     */
    private SecurityDto convertSecurityToDto(Security security) {
        SecurityDto dto = new SecurityDto();
        dto.setUuid(security.getUUID());
        dto.setName(security.getName());
        dto.setCurrencyCode(security.getCurrencyCode());
        dto.setTargetCurrencyCode(security.getTargetCurrencyCode());
        dto.setIsin(security.getIsin());
        dto.setTickerSymbol(security.getTickerSymbol());
        dto.setWkn(security.getWkn());
        dto.setNote(security.getNote());
        dto.setRetired(security.isRetired());
        dto.setFeed(security.getFeed());
        dto.setFeedURL(security.getFeedURL());
        dto.setLatestFeed(security.getLatestFeed());
        dto.setLatestFeedURL(security.getLatestFeedURL());
        dto.setPricesCount(security.getPrices().size());
        dto.setUpdatedAt(security.getUpdatedAt());
        
        return dto;
    }
}

