package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.api.dto.OptionTransactionDto;

/**
 * REST Controller for options operations.
 * 
 * This controller provides endpoints to manage options transactions within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/options")
public class OptionsController extends BaseController {
    
    /**
     * Get options earnings for a given period.
     * 
     * This endpoint returns all options-related transactions (security sells with options ticker patterns
     * and option-related fees) within a specified date range.
     * 
     * Options are identified by security names matching the pattern like "SPXW 13OCT25 6025 P"
     * where the security name contains expiration date, strike price, and option type (Put/Call).
     * 
     * Additionally, fee transactions with the comment "EXPOSURE FEE" are included as options-related fees.
     * 
     * @param portfolioId The portfolio ID (file)
     * @param startDate Start date for filtering transactions (optional, defaults to 1 year ago)
     * @param endDate End date for filtering transactions (optional, defaults to today)
     * @return Response containing the list of options earnings transactions
     */
    @GET
    @Path("/earnings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOptionsEarnings(@PathParam("portfolioId") String portfolioId,
                                      @QueryParam("startDate") String startDate,
                                      @QueryParam("endDate") String endDate) {
        try {
            logger.info("Getting options earnings for portfolio {} from {} to {}", 
                portfolioId, startDate, endDate);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing options earnings");
            }
            
            // Parse dates with defaults
            LocalDate start = startDate != null && !startDate.isEmpty() 
                ? LocalDate.parse(startDate) 
                : LocalDate.now().minusYears(1);
            LocalDate end = endDate != null && !endDate.isEmpty() 
                ? LocalDate.parse(endDate) 
                : LocalDate.now();
            
            // Validate date range
            if (start.isAfter(end)) {
                return createErrorResponse(Response.Status.BAD_REQUEST, 
                    "Invalid date range", 
                    "Start date must be before or equal to end date");
            }
            
            // Create currency converter for base currency conversion
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Collect all options earnings transactions
            List<OptionTransactionDto> optionsEarnings = new ArrayList<>();
            
            // 1. Collect security SELL and BUY transactions with options pattern
            for (Portfolio portfolio : client.getPortfolios()) {
                for (PortfolioTransaction tx : portfolio.getTransactions()) {
                    // Filter by type - only SELL and BUY transactions
                    if (tx.getType() != PortfolioTransaction.Type.SELL && tx.getType() != PortfolioTransaction.Type.BUY) {
                        continue;
                    }
                    
                    // Filter by date range
                    LocalDate txDate = tx.getDateTime().toLocalDate();
                    if (txDate.isBefore(start) || txDate.isAfter(end)) {
                        continue;
                    }
                    
                    // Check if security has options name pattern
                    Security security = tx.getSecurity();
                    if (security == null || security.getName() == null) {
                        continue;
                    }
                    
                    // Check if this looks like an option (has the pattern like "SPXW 13OCT25 6025 P")
                    String securityName = security.getName().trim();
                    String[] parts = securityName.split("\\s+");
                    
                    // Need at least 4 parts: underlying, expiration, strike, type
                    if (parts.length < 4) {
                        continue;
                    }
                    
                    // Check if third part is a number (strike price) and fourth is P or C
                    try {
                        Double.parseDouble(parts[2]);
                        if (!parts[3].equals("P") && !parts[3].equals("C")) {
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    
                    // This is an options transaction - create DTO
                    OptionTransactionDto dto = new OptionTransactionDto();
                    dto.setUuid(tx.getUUID());
                    dto.setTransactionTime(tx.getDateTime());
                    dto.setType(tx.getType().toString()); // "SELL" or "BUY"
                    dto.setCurrencyCode(tx.getCurrencyCode());
                    
                    // Calculate number of contracts (shares / 100)
                    long sharesRaw = tx.getShares();
                    double actualShares = sharesRaw / (double) Values.Share.factor();
                    int numberOfContracts = (int) Math.round(actualShares / 100.0);
                    dto.setNumberOfContracts(numberOfContracts);
                    
                    // Calculate price per contract with proper precision handling
                    double grossValue = tx.getGrossValueAmount() / Values.Amount.divider();
                    double sharesCount = sharesRaw / (double) Values.Share.factor();
                    double pricePerShare = sharesCount != 0 ? grossValue / sharesCount : 0;
                    double pricePerContract = pricePerShare * 100;
                    dto.setPricePerContract(pricePerContract);
                    
                    // Get fees
                    Money feesMoney = tx.getUnitSum(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE);
                    double fees = feesMoney.getAmount() / Values.Amount.divider();
                    dto.setFees(fees);
                    
                    // Calculate total amount (for SELL: received, for BUY: paid)
                    double totalAmount = tx.getAmount() / Values.Amount.divider();
                    // For BUY transactions, make the amount negative to indicate money spent
                    if (tx.getType() == PortfolioTransaction.Type.BUY) {
                        totalAmount = -totalAmount;
                    }
                    dto.setTotalAmount(totalAmount);
                    
                    // Parse option security name
                    dto.setOriginalSecurityName(securityName);
                    parseOptionSecurity(securityName, dto);
                    
                    // Find underlying security price at transaction time
                    if (dto.getUnderlyingSecurityName() != null) {
                        Double underlyingPrice = findUnderlyingSecurityPrice(client, dto.getUnderlyingSecurityName(), tx.getDateTime());
                        dto.setUnderlyingSecurityPrice(underlyingPrice);
                        dto.setUnderlyingSecurityCurrency(tx.getCurrencyCode());
                    }
                    
                    // Add security information
                    dto.setSecurityUuid(security.getUUID());
                    dto.setSecurityIsin(security.getIsin());
                    
                    // Add portfolio information
                    if (portfolio.getReferenceAccount() != null) {
                        dto.setAccountUuid(portfolio.getReferenceAccount().getUUID());
                        dto.setAccountName(portfolio.getReferenceAccount().getName());
                    }
                    dto.setNote(tx.getNote());
                    dto.setSource(tx.getSource());
                    
                    // Converted amounts in base currency
                    dto.setBaseCurrency(client.getBaseCurrency());
                    
                    Money totalAmountMoney = Money.of(tx.getCurrencyCode(), tx.getAmount()).with(converter.at(tx.getDateTime()));
                    double convertedTotal = totalAmountMoney.getAmount() / Values.Amount.divider();
                    // For BUY transactions, make negative
                    if (tx.getType() == PortfolioTransaction.Type.BUY) {
                        convertedTotal = -convertedTotal;
                    }
                    dto.setTotalAmountInBaseCurrency(convertedTotal);
                    
                    Money feesConverted = feesMoney.with(converter.at(tx.getDateTime()));
                    dto.setFeesInBaseCurrency(feesConverted.getAmount() / Values.Amount.divider());
                    
                    Money pricePerContractMoney = Money.of(tx.getCurrencyCode(), 
                        (long)(pricePerContract * Values.Amount.divider())).with(converter.at(tx.getDateTime()));
                    dto.setPricePerContractInBaseCurrency(pricePerContractMoney.getAmount() / Values.Amount.divider());
                    
                    optionsEarnings.add(dto);
                }
            }
            
            // 2. Collect fee transactions with comment "EXPOSURE FEE"
            for (Account account : client.getAccounts()) {
                for (AccountTransaction tx : account.getTransactions()) {
                    // Filter by type - only FEES transactions
                    if (tx.getType() != AccountTransaction.Type.FEES) {
                        continue;
                    }
                    
                    // Filter by date range
                    LocalDate txDate = tx.getDateTime().toLocalDate();
                    if (txDate.isBefore(start) || txDate.isAfter(end)) {
                        continue;
                    }
                    
                    // Filter by note/comment "EXPOSURE FEE"
                    if (tx.getNote() == null || !tx.getNote().contains("EXPOSURE FEE")) {
                        continue;
                    }
                    
                    // This is an options-related fee - create DTO
                    OptionTransactionDto dto = new OptionTransactionDto();
                    dto.setUuid(tx.getUUID());
                    dto.setTransactionTime(tx.getDateTime());
                    dto.setType("EXPOSURE_FEE");
                    dto.setCurrencyCode(tx.getCurrencyCode());
                    
                    // For fee transactions, the amount is negative (debit)
                    double amount = -(tx.getAmount() / Values.Amount.divider());
                    double feeAmount = Math.abs(amount);
                    
                    dto.setNumberOfContracts(0);
                    dto.setPricePerContract(0.0);
                    dto.setFees(feeAmount);
                    dto.setTotalAmount(amount);
                    
                    // Converted amounts in base currency
                    dto.setBaseCurrency(client.getBaseCurrency());
                    
                    Money amountMoney = tx.getMonetaryAmount().with(converter.at(tx.getDateTime()));
                    double convertedAmount = -(amountMoney.getAmount() / Values.Amount.divider());
                    dto.setTotalAmountInBaseCurrency(convertedAmount);
                    dto.setFeesInBaseCurrency(Math.abs(convertedAmount));
                    dto.setPricePerContractInBaseCurrency(0.0);
                    
                    // Add security information if available
                    if (tx.getSecurity() != null) {
                        dto.setSecurityUuid(tx.getSecurity().getUUID());
                        dto.setOriginalSecurityName(tx.getSecurity().getName());
                        dto.setSecurityIsin(tx.getSecurity().getIsin());
                        
                        // Try to parse option details if available
                        parseOptionSecurity(tx.getSecurity().getName(), dto);
                    }
                    
                    // Add account information
                    dto.setAccountUuid(account.getUUID());
                    dto.setAccountName(account.getName());
                    dto.setNote(tx.getNote());
                    dto.setSource(tx.getSource());
                    
                    optionsEarnings.add(dto);
                }
            }
            
            // Sort by date (most recent first)
            optionsEarnings.sort((a, b) -> b.getTransactionTime().compareTo(a.getTransactionTime()));
            
            // Calculate totals in base currency
            double totalAmountInBaseCurrency = optionsEarnings.stream()
                .mapToDouble(OptionTransactionDto::getTotalAmountInBaseCurrency)
                .sum();
            double totalFeesInBaseCurrency = optionsEarnings.stream()
                .mapToDouble(OptionTransactionDto::getFeesInBaseCurrency)
                .sum();
            int totalContracts = optionsEarnings.stream()
                .mapToInt(OptionTransactionDto::getNumberOfContracts)
                .sum();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("count", optionsEarnings.size());
            response.put("baseCurrency", client.getBaseCurrency());
            response.put("totalAmountInBaseCurrency", totalAmountInBaseCurrency);
            response.put("totalFeesInBaseCurrency", totalFeesInBaseCurrency);
            response.put("totalContracts", totalContracts);
            response.put("optionsEarnings", optionsEarnings);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} options transactions for portfolio {} (total in base currency: {}, total contracts: {})", 
                optionsEarnings.size(), portfolioId, totalAmountInBaseCurrency, totalContracts);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting options earnings for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new options transaction.
     * TODO: Implement options transaction creation
     * 
     * @param portfolioId The portfolio ID
     * @param optionData Options transaction data
     * @return Created options transaction
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOption(@PathParam("portfolioId") String portfolioId,
                                 Map<String, Object> optionData) {
        // TODO: Implement options transaction creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Options transaction creation not yet implemented");
    }
    
    /**
     * Update an existing options transaction.
     * TODO: Implement options transaction update
     * 
     * @param portfolioId The portfolio ID
     * @param optionUuid The options transaction UUID
     * @param optionData Updated options transaction data
     * @return Updated options transaction
     */
    @PUT
    @Path("/{optionUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOption(@PathParam("portfolioId") String portfolioId,
                                 @PathParam("optionUuid") String optionUuid,
                                 Map<String, Object> optionData) {
        // TODO: Implement options transaction update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Options transaction update not yet implemented");
    }
    
    /**
     * Delete an options transaction.
     * TODO: Implement options transaction deletion
     * 
     * @param portfolioId The portfolio ID
     * @param optionUuid The options transaction UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{optionUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOption(@PathParam("portfolioId") String portfolioId,
                                 @PathParam("optionUuid") String optionUuid) {
        // TODO: Implement options transaction deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Options transaction deletion not yet implemented");
    }
    
    /**
     * Helper method to parse option security name.
     * Format: "SPXW 13OCT25 6025 P" or variations
     */
    private void parseOptionSecurity(String securityName, OptionTransactionDto dto) {
        if (securityName == null || securityName.trim().isEmpty()) {
            return;
        }
        
        try {
            // Parse format: "SPXW 13OCT25 6025 P"
            String[] parts = securityName.trim().split("\\s+");
            
            if (parts.length >= 4) {
                // Parse underlying security name (remove W suffix if present)
                String underlying = parts[0];
                if (underlying.endsWith("W")) {
                    underlying = underlying.substring(0, underlying.length() - 1);
                }
                dto.setUnderlyingSecurityName(underlying);
                
                // Parse expiration date
                dto.setExpirationDate(parts[1]);
                dto.setParsedExpirationDate(parseExpirationDate(parts[1]));
                
                // Parse strike price
                try {
                    dto.setStrikePrice(Double.parseDouble(parts[2]));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse strike price: {}", parts[2]);
                }
                
                // Parse option type (P or C)
                dto.setOptionType(parts[3]);
            }
        } catch (Exception e) {
            logger.warn("Could not parse option security name: {}", securityName, e);
        }
    }
    
    /**
     * Helper method to parse expiration date from format like "13OCT25".
     */
    private LocalDate parseExpirationDate(String expirationStr) {
        try {
            // Format: DDMMMYY (e.g., "13OCT25")
            String day = expirationStr.substring(0, 2);
            String month = expirationStr.substring(2, 5);
            String year = expirationStr.substring(5, 7);
            
            // Convert month abbreviation to number
            int monthNum = switch (month.toUpperCase()) {
                case "JAN" -> 1;
                case "FEB" -> 2;
                case "MAR" -> 3;
                case "APR" -> 4;
                case "MAY" -> 5;
                case "JUN" -> 6;
                case "JUL" -> 7;
                case "AUG" -> 8;
                case "SEP" -> 9;
                case "OCT" -> 10;
                case "NOV" -> 11;
                case "DEC" -> 12;
                default -> throw new IllegalArgumentException("Invalid month: " + month);
            };
            
            // Assume 20xx for years
            int fullYear = 2000 + Integer.parseInt(year);
            
            return LocalDate.of(fullYear, monthNum, Integer.parseInt(day));
        } catch (Exception e) {
            logger.warn("Could not parse expiration date: {}", expirationStr, e);
            return null;
        }
    }
    
    /**
     * Helper method to find the underlying security price at a specific date.
     */
    private Double findUnderlyingSecurityPrice(Client client, String underlyingSecurityName, LocalDateTime transactionTime) {
        if (underlyingSecurityName == null || client == null) {
            return null;
        }
        
        try {
            // Find the underlying security by name (e.g., SPX)
            Security underlyingSecurity = client.getSecurities().stream()
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(underlyingSecurityName))
                .findFirst()
                .orElse(null);
            
            if (underlyingSecurity == null) {
                logger.debug("Could not find underlying security with name: {}", underlyingSecurityName);
                return null;
            }
            
            // Get the price at or before the transaction date
            LocalDate txDate = transactionTime.toLocalDate();
            SecurityPrice priceAtDate = underlyingSecurity.getSecurityPrice(txDate);
            
            if (priceAtDate != null) {
                return priceAtDate.getValue() / (double) Values.Quote.divider();
            }
            
        } catch (Exception e) {
            logger.warn("Error finding underlying security price for {}: {}", underlyingSecurityName, e.getMessage());
        }
        
        return null;
    }
}

