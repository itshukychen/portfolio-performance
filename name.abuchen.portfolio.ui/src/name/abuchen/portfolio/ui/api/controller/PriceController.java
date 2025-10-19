package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDate;
import java.time.ZoneId;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.api.dto.SecurityPriceDto;
import name.abuchen.portfolio.ui.api.service.PortfolioFileService;

/**
 * REST Controller for security price operations.
 * 
 * This controller provides endpoints to manage security prices including:
 * - Viewing historical prices
 * - Adding new prices
 * - Updating existing prices
 * - Deleting prices
 */
@Path("/api/v1/portfolios")
public class PriceController {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);
    
    // Use static singleton to ensure cache is shared across all API calls
    private static final PortfolioFileService portfolioFileService = PortfolioFileService.getInstance();
    
    /**
     * Helper method to create error responses with consistent structure.
     * 
     * @param status HTTP status code
     * @param error Error type/category
     * @param message Detailed error message
     * @return Response with error details
     */
    private Response createErrorResponse(Response.Status status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return Response.status(status).entity(errorResponse).build();
    }
    
    /**
     * Helper method to create 428 Precondition Required responses.
     * Used when a portfolio must be opened first before accessing a resource.
     * 
     * @param error Error type/category
     * @param message Detailed error message
     * @return Response with 428 status and error details
     */
    private Response createPreconditionRequiredResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return Response.status(428).entity(errorResponse).build();
    }
    
    /**
     * Get historical prices for a specific security.
     * 
     * This endpoint returns all historical prices for a security identified by its UUID.
     * The prices are returned in chronological order with dates and values.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @return Response containing the security's historical prices
     */
    @GET
    @Path("/{portfolioId}/securities/{securityUuid}/prices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecurityPrices(@PathParam("portfolioId") String portfolioId,
                                     @PathParam("securityUuid") String securityUuid) {
        try {
            logger.info("Getting prices for security {} in portfolio {}", securityUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing security prices");
            }
            
            // Find the security by UUID
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found in portfolio");
            }
            
            // Convert SecurityPrice list to DTO list
            List<SecurityPriceDto> priceDtos = security.getPrices().stream()
                .map(price -> new SecurityPriceDto(
                    price.getDate(),
                    price.getValue() / Values.Quote.divider()
                ))
                .collect(Collectors.toList());
            
            // Create response with security info and prices
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("securityUuid", securityUuid);
            response.put("securityName", security.getName());
            response.put("currencyCode", security.getCurrencyCode());
            response.put("isin", security.getIsin());
            response.put("tickerSymbol", security.getTickerSymbol());
            response.put("pricesCount", priceDtos.size());
            response.put("prices", priceDtos);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} prices for security {} ({})", priceDtos.size(), security.getName(), securityUuid);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting prices for security {} in portfolio {}: {}", 
                securityUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Add a new price to a security.
     * 
     * This endpoint adds a new historical price for a security. If a price already exists
     * for the given date, it will be overwritten.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @param priceDto The price data to add (date and value)
     * @return Response indicating success or failure
     */
    @POST
    @Path("/{portfolioId}/securities/{securityUuid}/prices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSecurityPrice(@PathParam("portfolioId") String portfolioId,
                                    @PathParam("securityUuid") String securityUuid,
                                    SecurityPriceDto priceDto) {
        try {
            logger.info("Adding price for security {} in portfolio {}: date={}, value={}", 
                securityUuid, portfolioId, priceDto.getDate(), priceDto.getValue());
            
            // Validate input
            if (priceDto.getDate() == null) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid request", 
                    "Date is required");
            }
            
            if (priceDto.getValue() < 0) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid request", 
                    "Price value must be non-negative");
            }
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before adding security prices");
            }
            
            // Find the security by UUID
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found in portfolio");
            }
            
            // Create SecurityPrice object
            // Note: SecurityPrice uses long value which must be multiplied by Values.Quote.divider()
            long priceValue = Math.round(priceDto.getValue() * Values.Quote.divider());
            SecurityPrice newPrice = new SecurityPrice(priceDto.getDate(), priceValue);
            
            // Add the price (will overwrite if price exists for this date)
            boolean wasAdded = security.addPrice(newPrice, true);
            
            // Save the portfolio file after the update
            portfolioFileService.saveFile(portfolioId);
            
            logger.info("Price {} for security {} ({}) on date {}", 
                wasAdded ? "added/updated" : "unchanged", 
                security.getName(), securityUuid, priceDto.getDate());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("portfolioId", portfolioId);
            response.put("securityUuid", securityUuid);
            response.put("securityName", security.getName());
            response.put("price", priceDto);
            response.put("wasModified", wasAdded);
            response.put("message", wasAdded ? "Price added/updated successfully" : "Price unchanged (identical price already exists)");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error adding price for security {} in portfolio {}: {}", 
                securityUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Update an existing price for a security.
     * 
     * This endpoint updates the price value for a specific date. If no price exists for
     * the given date, a 404 error is returned.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @param date The date of the price to update (YYYY-MM-DD format)
     * @param priceDto The new price data (only value is used, date is taken from path)
     * @return Response indicating success or failure
     */
    @PUT
    @Path("/{portfolioId}/securities/{securityUuid}/prices/{date}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecurityPrice(@PathParam("portfolioId") String portfolioId,
                                       @PathParam("securityUuid") String securityUuid,
                                       @PathParam("date") String date,
                                       SecurityPriceDto priceDto) {
        try {
            logger.info("Updating price for security {} in portfolio {} on date {}: new value={}", 
                securityUuid, portfolioId, date, priceDto.getValue());
            
            // Parse the date from path parameter
            LocalDate priceDate;
            try {
                priceDate = LocalDate.parse(date);
            } catch (Exception e) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date format", 
                    "Date must be in YYYY-MM-DD format");
            }
            
            // Validate input
            if (priceDto.getValue() < 0) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid request", 
                    "Price value must be non-negative");
            }
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before updating security prices");
            }
            
            // Find the security by UUID
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found in portfolio");
            }
            
            // Find the existing price for this date
            SecurityPrice existingPrice = security.getPrices().stream()
                .filter(p -> p.getDate().equals(priceDate))
                .findFirst()
                .orElse(null);
            
            if (existingPrice == null) {
                logger.warn("Price not found for security {} on date {}", securityUuid, priceDate);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Price not found", 
                    "No price found for date " + priceDate);
            }
            
            // Create new SecurityPrice object with updated value
            long priceValue = Math.round(priceDto.getValue() * Values.Quote.divider());
            SecurityPrice updatedPrice = new SecurityPrice(priceDate, priceValue);
            
            // Remove old price and add new one
            security.removePrice(existingPrice);
            security.addPrice(updatedPrice, true);
            
            // Save the portfolio file after the update
            portfolioFileService.saveFile(portfolioId);
            
            logger.info("Price updated for security {} ({}) on date {} from {} to {}", 
                security.getName(), securityUuid, priceDate, 
                existingPrice.getValue() / Values.Quote.divider(), priceDto.getValue());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("portfolioId", portfolioId);
            response.put("securityUuid", securityUuid);
            response.put("securityName", security.getName());
            response.put("date", priceDate);
            response.put("oldValue", existingPrice.getValue() / Values.Quote.divider());
            response.put("newValue", priceDto.getValue());
            response.put("message", "Price updated successfully");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error updating price for security {} in portfolio {} on date {}: {}", 
                securityUuid, portfolioId, date, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Delete a price from a security.
     * 
     * This endpoint removes the price for a specific date from a security's historical prices.
     * 
     * @param portfolioId The portfolio ID
     * @param securityUuid The security UUID
     * @param date The date of the price to delete (YYYY-MM-DD format)
     * @return Response indicating success or failure
     */
    @DELETE
    @Path("/{portfolioId}/securities/{securityUuid}/prices/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSecurityPrice(@PathParam("portfolioId") String portfolioId,
                                       @PathParam("securityUuid") String securityUuid,
                                       @PathParam("date") String date) {
        try {
            logger.info("Deleting price for security {} in portfolio {} on date {}", 
                securityUuid, portfolioId, date);
            
            // Parse the date from path parameter
            LocalDate priceDate;
            try {
                priceDate = LocalDate.parse(date);
            } catch (Exception e) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date format", 
                    "Date must be in YYYY-MM-DD format");
            }
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: " + portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before deleting security prices");
            }
            
            // Find the security by UUID
            Security security = client.getSecurities().stream()
                .filter(s -> securityUuid.equals(s.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (security == null) {
                logger.warn("Security not found: {} in portfolio: {}", securityUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Security not found", 
                    "Security with UUID " + securityUuid + " not found in portfolio");
            }
            
            // Find the price for this date
            SecurityPrice priceToDelete = security.getPrices().stream()
                .filter(p -> p.getDate().equals(priceDate))
                .findFirst()
                .orElse(null);
            
            if (priceToDelete == null) {
                logger.warn("Price not found for security {} on date {}", securityUuid, priceDate);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Price not found", 
                    "No price found for date " + priceDate);
            }
            
            // Store the value for response
            double deletedValue = priceToDelete.getValue() / Values.Quote.divider();
            
            // Remove the price
            security.removePrice(priceToDelete);
            
            // Save the portfolio file after the update
            portfolioFileService.saveFile(portfolioId);
            
            logger.info("Price deleted for security {} ({}) on date {} (value was {})", 
                security.getName(), securityUuid, priceDate, deletedValue);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("portfolioId", portfolioId);
            response.put("securityUuid", securityUuid);
            response.put("securityName", security.getName());
            response.put("date", priceDate);
            response.put("deletedValue", deletedValue);
            response.put("message", "Price deleted successfully");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error deleting price for security {} in portfolio {} on date {}: {}", 
                securityUuid, portfolioId, date, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
}

