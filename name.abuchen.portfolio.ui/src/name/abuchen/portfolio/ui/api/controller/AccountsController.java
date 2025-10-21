package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.ui.api.dto.AccountDto;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.ui.api.dto.ValueDataPointDto;

/**
 * REST Controller for account operations.
 * 
 * This controller provides endpoints to manage accounts within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/accounts")
public class AccountsController extends BaseController {
    
    /**
     * Get all accounts in a portfolio.
     * 
     * @param portfolioId The portfolio ID
     * @return List of all accounts
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAccounts(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting all accounts for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing accounts");
            }
            
            // Get all accounts with current values
            List<AccountDto> accounts = client.getAccounts().stream()
                .map(account -> convertAccountToDtoWithValues(account, client))
                .collect(Collectors.toList());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("count", accounts.size());
            response.put("accounts", accounts);
            
            logger.info("Returning {} accounts for portfolio {}", accounts.size(), portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting accounts for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get a specific account by UUID.
     * 
     * @param portfolioId The portfolio ID
     * @param accountUuid The account UUID
     * @return Account details
     */
    @GET
    @Path("/{accountUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountById(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("accountUuid") String accountUuid) {
        try {
            logger.info("Getting account {} for portfolio {}", accountUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing accounts");
            }
            
            // Find the account by UUID
            Account account = client.getAccounts().stream()
                .filter(a -> accountUuid.equals(a.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (account == null) {
                logger.warn("Account not found: {} in portfolio: {}", accountUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Account not found", 
                    "Account with UUID " + accountUuid + " not found in portfolio");
            }
            
            // Convert to DTO with values
            AccountDto accountDto = convertAccountToDtoWithValues(account, client);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("account", accountDto);
            
            logger.info("Returning account {} ({}) for portfolio {}", 
                account.getName(), accountUuid, portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting account {} for portfolio {}: {}", 
                accountUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get account value over time.
     * 
     * This endpoint returns the account balance over a specified date range.
     * The values are calculated based on account transactions up to each date.
     * 
     * @param portfolioId The portfolio ID
     * @param accountUuid The account UUID
     * @param startDate Start date for the time series (optional, defaults to 1 year ago)
     * @param endDate End date for the time series (optional, defaults to today)
     * @return Response containing the account's values over time
     */
    @GET
    @Path("/{accountUuid}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountValues(@PathParam("portfolioId") String portfolioId,
                                     @PathParam("accountUuid") String accountUuid,
                                     @QueryParam("startDate") String startDate,
                                     @QueryParam("endDate") String endDate) {
        try {
            logger.info("Getting values for account {} in portfolio {}", accountUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing account values");
            }
            
            // Find the account by UUID
            Account account = client.getAccounts().stream()
                .filter(a -> accountUuid.equals(a.getUUID()))
                .findFirst()
                .orElse(null);
            
            if (account == null) {
                logger.warn("Account not found: {} in portfolio: {}", accountUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Account not found", 
                    "Account with UUID " + accountUuid + " not found in portfolio");
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
            
            // Create currency converter
            ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            
            // Calculate values for each date in range
            List<ValueDataPointDto> valuePoints = new ArrayList<>();
            LocalDate currentDate = start;
            
            while (!currentDate.isAfter(end)) {
                AccountSnapshot snapshot = AccountSnapshot.create(account, converter, currentDate);
                Money funds = snapshot.getFunds();
                
                // Convert to double value
                double value = funds.getAmount() / Values.Amount.divider();
                
                valuePoints.add(new ValueDataPointDto(currentDate, value));
                currentDate = currentDate.plusDays(1);
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("accountUuid", accountUuid);
            response.put("accountName", account.getName());
            response.put("currencyCode", account.getCurrencyCode());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("dataPointsCount", valuePoints.size());
            response.put("values", valuePoints);
            response.put("timezone", ZoneId.systemDefault().getId());
            
            logger.info("Returning {} value data points for account {} ({})", 
                valuePoints.size(), account.getName(), accountUuid);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting values for account {} in portfolio {}: {}", 
                accountUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new account.
     * TODO: Implement account creation
     * 
     * @param portfolioId The portfolio ID
     * @param accountData Account data
     * @return Created account
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(@PathParam("portfolioId") String portfolioId,
                                  Map<String, Object> accountData) {
        // TODO: Implement account creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Account creation not yet implemented");
    }
    
    /**
     * Update an existing account.
     * TODO: Implement account update
     * 
     * @param portfolioId The portfolio ID
     * @param accountUuid The account UUID
     * @param accountData Updated account data
     * @return Updated account
     */
    @PUT
    @Path("/{accountUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccount(@PathParam("portfolioId") String portfolioId,
                                  @PathParam("accountUuid") String accountUuid,
                                  Map<String, Object> accountData) {
        // TODO: Implement account update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Account update not yet implemented");
    }
    
    /**
     * Delete an account.
     * TODO: Implement account deletion
     * 
     * @param portfolioId The portfolio ID
     * @param accountUuid The account UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{accountUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(@PathParam("portfolioId") String portfolioId,
                                  @PathParam("accountUuid") String accountUuid) {
        // TODO: Implement account deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Account deletion not yet implemented");
    }
    
    /**
     * Helper method to convert Account to AccountDto.
     * Aligned with PortfolioFileService.loadAccounts()
     */
    private AccountDto convertAccountToDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setUuid(account.getUUID());
        dto.setName(account.getName());
        dto.setCurrencyCode(account.getCurrencyCode());
        dto.setNote(account.getNote());
        dto.setRetired(account.isRetired());
        dto.setTransactionsCount(account.getTransactions().size());
        dto.setUpdatedAt(account.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Helper method to convert Account to AccountDto with current values.
     * This version includes current value calculations.
     */
    private AccountDto convertAccountToDtoWithValues(Account account, Client client) {
        AccountDto dto = convertAccountToDto(account);
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Create currency converter for account valuations in base currency
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
        CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        
        // Calculate current value
        try {
            long currentAmount = account.getCurrentAmount(now);
            // Convert from internal representation (multiplied by 100) to decimal
            dto.setCurrentValue(currentAmount / 100.0);
            
            // Convert to base currency
            Money accountMoney = Money.of(account.getCurrencyCode(), currentAmount);
            Money convertedMoney = accountMoney.with(converter.at(java.time.LocalDate.now()));
            dto.setCurrentValueInBaseCurrency(convertedMoney.getAmount() / 100.0);
        } catch (Exception e) {
            logger.warn("Failed to calculate account balance for {}: {}", account.getName(), e.getMessage());
            dto.setCurrentValue(0.0);
            dto.setCurrentValueInBaseCurrency(0.0);
        }
        
        return dto;
    }
}

