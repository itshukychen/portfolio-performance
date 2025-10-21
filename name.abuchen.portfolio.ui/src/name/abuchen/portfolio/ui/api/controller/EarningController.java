package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.api.dto.EarningsTransactionDto;

/**
 * REST Controller for earnings operations.
 * 
 * This controller provides endpoints to manage earnings (dividends, interest) within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/earnings")
public class EarningController extends BaseController {
    
    /**
     * Get earnings transactions for a given period.
     * 
     * This endpoint returns all earnings transactions (dividends, interest, interest charges)
     * within a specified date range.
     * 
     * @param portfolioId The portfolio ID (file)
     * @param startDate Start date for filtering transactions (optional, defaults to 1 year ago)
     * @param endDate End date for filtering transactions (optional, defaults to today)
     * @param type Filter by transaction type: DIVIDENDS, INTEREST, INTEREST_CHARGE, or ALL (optional, defaults to ALL)
     * @return Response containing the list of earnings transactions
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEarnings(@PathParam("portfolioId") String portfolioId,
                               @QueryParam("startDate") String startDate,
                               @QueryParam("endDate") String endDate,
                               @QueryParam("type") String type) {
        try {
            logger.info("Getting earnings for portfolio {} from {} to {} (type: {})", 
                portfolioId, startDate, endDate, type);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing earnings");
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
            
            // Determine which transaction types to include
            Predicate<AccountTransaction.Type> typeFilter;
            String filterType = type != null && !type.isEmpty() ? type.toUpperCase() : "ALL";
            
            switch (filterType) {
                case "DIVIDENDS":
                    typeFilter = t -> t == AccountTransaction.Type.DIVIDENDS;
                    break;
                case "INTEREST":
                    typeFilter = t -> t == AccountTransaction.Type.INTEREST 
                                   || t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
                case "INTEREST_CHARGE":
                    typeFilter = t -> t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
                case "ALL":
                default:
                    typeFilter = t -> t == AccountTransaction.Type.DIVIDENDS 
                                   || t == AccountTransaction.Type.INTEREST 
                                   || t == AccountTransaction.Type.INTEREST_CHARGE;
                    break;
            }
            
            // Create currency converter for base currency conversion
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Collect all earnings transactions from all accounts
            List<EarningsTransactionDto> earnings = new ArrayList<>();
            
            for (Account account : client.getAccounts()) {
                for (AccountTransaction tx : account.getTransactions()) {
                    // Filter by type
                    if (!typeFilter.test(tx.getType())) {
                        continue;
                    }
                    
                    // Filter by date range
                    LocalDate txDate = tx.getDateTime().toLocalDate();
                    if (txDate.isBefore(start) || txDate.isAfter(end)) {
                        continue;
                    }
                    
                    // Create DTO
                    EarningsTransactionDto dto = new EarningsTransactionDto();
                    dto.setUuid(tx.getUUID());
                    dto.setDateTime(tx.getDateTime());
                    dto.setType(tx.getType().name());
                    dto.setCurrencyCode(tx.getCurrencyCode());
                    
                    // Original currency amounts
                    double amount = tx.getAmount() / Values.Amount.divider();
                    double grossValue = tx.getGrossValueAmount() / Values.Amount.divider();
                    
                    Money taxesMoney = tx.getUnitSum(name.abuchen.portfolio.model.Transaction.Unit.Type.TAX);
                    Money feesMoney = tx.getUnitSum(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE);
                    
                    double taxes = taxesMoney.getAmount() / Values.Amount.divider();
                    double fees = feesMoney.getAmount() / Values.Amount.divider();
                    
                    dto.setAmount(amount);
                    dto.setGrossValue(grossValue);
                    dto.setTaxes(taxes);
                    dto.setFees(fees);
                    
                    // Converted amounts in base currency
                    dto.setBaseCurrency(client.getBaseCurrency());
                    
                    Money amountMoney = tx.getMonetaryAmount().with(converter.at(tx.getDateTime()));
                    dto.setAmountInBaseCurrency(amountMoney.getAmount() / Values.Amount.divider());
                    
                    Money grossValueMoney = tx.getGrossValue().with(converter.at(tx.getDateTime()));
                    dto.setGrossValueInBaseCurrency(grossValueMoney.getAmount() / Values.Amount.divider());
                    
                    Money taxesConverted = taxesMoney.with(converter.at(tx.getDateTime()));
                    dto.setTaxesInBaseCurrency(taxesConverted.getAmount() / Values.Amount.divider());
                    
                    Money feesConverted = feesMoney.with(converter.at(tx.getDateTime()));
                    dto.setFeesInBaseCurrency(feesConverted.getAmount() / Values.Amount.divider());
                    
                    // Add security information if available
                    if (tx.getSecurity() != null) {
                        dto.setSecurityUuid(tx.getSecurity().getUUID());
                        dto.setSecurityName(tx.getSecurity().getName());
                        dto.setSecurityIsin(tx.getSecurity().getIsin());
                    }
                    
                    // Add account information
                    dto.setAccountUuid(account.getUUID());
                    dto.setAccountName(account.getName());
                    dto.setNote(tx.getNote());
                    dto.setSource(tx.getSource());
                    
                    earnings.add(dto);
                }
            }
            
            // Sort by date (most recent first)
            earnings.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
            
            // Calculate totals in base currency (for consistent aggregation across currencies)
            double totalAmountInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getAmountInBaseCurrency)
                .sum();
            double totalGrossValueInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getGrossValueInBaseCurrency)
                .sum();
            double totalTaxesInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getTaxesInBaseCurrency)
                .sum();
            double totalFeesInBaseCurrency = earnings.stream()
                .mapToDouble(EarningsTransactionDto::getFeesInBaseCurrency)
                .sum();
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("filterType", filterType);
            response.put("count", earnings.size());
            response.put("baseCurrency", client.getBaseCurrency());
            response.put("totalAmountInBaseCurrency", totalAmountInBaseCurrency);
            response.put("totalGrossValueInBaseCurrency", totalGrossValueInBaseCurrency);
            response.put("totalTaxesInBaseCurrency", totalTaxesInBaseCurrency);
            response.put("totalFeesInBaseCurrency", totalFeesInBaseCurrency);
            response.put("earnings", earnings);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} earnings transactions for portfolio {} (total in base currency: {}, gross: {})", 
                earnings.size(), portfolioId, totalAmountInBaseCurrency, totalGrossValueInBaseCurrency);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting earnings for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new earnings transaction.
     * TODO: Implement earnings transaction creation
     * 
     * @param portfolioId The portfolio ID
     * @param earningData Earnings transaction data
     * @return Created earnings transaction
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEarning(@PathParam("portfolioId") String portfolioId,
                                  Map<String, Object> earningData) {
        // TODO: Implement earnings transaction creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Earnings transaction creation not yet implemented");
    }
    
    /**
     * Update an existing earnings transaction.
     * TODO: Implement earnings transaction update
     * 
     * @param portfolioId The portfolio ID
     * @param earningUuid The earnings transaction UUID
     * @param earningData Updated earnings transaction data
     * @return Updated earnings transaction
     */
    @PUT
    @Path("/{earningUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEarning(@PathParam("portfolioId") String portfolioId,
                                  @PathParam("earningUuid") String earningUuid,
                                  Map<String, Object> earningData) {
        // TODO: Implement earnings transaction update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Earnings transaction update not yet implemented");
    }
    
    /**
     * Delete an earnings transaction.
     * TODO: Implement earnings transaction deletion
     * 
     * @param portfolioId The portfolio ID
     * @param earningUuid The earnings transaction UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{earningUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEarning(@PathParam("portfolioId") String portfolioId,
                                  @PathParam("earningUuid") String earningUuid) {
        // TODO: Implement earnings transaction deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Earnings transaction deletion not yet implemented");
    }
}

