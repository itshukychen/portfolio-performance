package name.abuchen.portfolio.ui.api.controller;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.api.dto.TransactionDto;

/**
 * REST Controller for transaction operations.
 * 
 * This controller provides endpoints to manage all transactions (account and portfolio) within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/transactions")
public class TransactionsController extends BaseController {
    
    /**
     * Get all transactions in a portfolio.
     * Returns de-duplicated transactions from both accounts and portfolios.
     * 
     * @param portfolioId The portfolio ID
     * @return List of all transactions
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTransactions(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting all transactions for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing transactions");
            }
            
            // Get all de-duplicated transactions and convert to DTOs
            List<TransactionDto> transactions = convertTransactionsToDto(client.getAllTransactions());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("count", transactions.size());
            response.put("transactions", transactions);
            
            logger.info("Returning {} transactions for portfolio {}", transactions.size(), portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting transactions for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get a specific transaction by UUID.
     * 
     * @param portfolioId The portfolio ID
     * @param transactionUuid The transaction UUID
     * @return Transaction details
     */
    @GET
    @Path("/{transactionUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactionById(@PathParam("portfolioId") String portfolioId,
                                      @PathParam("transactionUuid") String transactionUuid) {
        try {
            logger.info("Getting transaction {} for portfolio {}", transactionUuid, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing transactions");
            }
            
            // Find the transaction by UUID
            TransactionPair<?> txPair = client.getAllTransactions().stream()
                .filter(pair -> transactionUuid.equals(pair.getTransaction().getUUID()))
                .findFirst()
                .orElse(null);
            
            if (txPair == null) {
                logger.warn("Transaction not found: {} in portfolio: {}", transactionUuid, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Transaction not found", 
                    "Transaction with UUID " + transactionUuid + " not found in portfolio");
            }
            
            // Convert to DTO
            TransactionDto transactionDto = convertTransactionPairToDto(txPair);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("transaction", transactionDto);
            
            logger.info("Returning transaction {} for portfolio {}", transactionUuid, portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting transaction {} for portfolio {}: {}", 
                transactionUuid, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new transaction.
     * TODO: Implement transaction creation
     * 
     * @param portfolioId The portfolio ID
     * @param transactionData Transaction data
     * @return Created transaction
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTransaction(@PathParam("portfolioId") String portfolioId,
                                      Map<String, Object> transactionData) {
        // TODO: Implement transaction creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Transaction creation not yet implemented");
    }
    
    /**
     * Update an existing transaction.
     * TODO: Implement transaction update
     * 
     * @param portfolioId The portfolio ID
     * @param transactionUuid The transaction UUID
     * @param transactionData Updated transaction data
     * @return Updated transaction
     */
    @PUT
    @Path("/{transactionUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTransaction(@PathParam("portfolioId") String portfolioId,
                                      @PathParam("transactionUuid") String transactionUuid,
                                      Map<String, Object> transactionData) {
        // TODO: Implement transaction update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Transaction update not yet implemented");
    }
    
    /**
     * Delete a transaction.
     * TODO: Implement transaction deletion
     * 
     * @param portfolioId The portfolio ID
     * @param transactionUuid The transaction UUID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{transactionUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTransaction(@PathParam("portfolioId") String portfolioId,
                                      @PathParam("transactionUuid") String transactionUuid) {
        // TODO: Implement transaction deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Transaction deletion not yet implemented");
    }
    
    /**
     * Helper method to convert a list of TransactionPair objects to TransactionDto list.
     * Migrated from PortfolioFileService.loadTransactions()
     */
    private List<TransactionDto> convertTransactionsToDto(List<TransactionPair<?>> transactionPairs) {
        List<TransactionDto> transactionDtos = new ArrayList<>();
        
        for (TransactionPair<?> pair : transactionPairs) {
            transactionDtos.add(convertTransactionPairToDto(pair));
        }
        
        // Sort by date descending (newest first)
        transactionDtos.sort((t1, t2) -> t2.getDateTime().compareTo(t1.getDateTime()));
        
        return transactionDtos;
    }
    
    /**
     * Helper method to convert a single TransactionPair to TransactionDto.
     */
    private TransactionDto convertTransactionPairToDto(TransactionPair<?> pair) {
        TransactionDto dto = new TransactionDto();
        
        var transaction = pair.getTransaction();
        
        // Set common fields
        dto.setUuid(transaction.getUUID());
        dto.setDateTime(transaction.getDateTime());
        dto.setCurrencyCode(transaction.getCurrencyCode());
        // Convert amount from internal representation to decimal
        dto.setAmount(transaction.getAmount() / 100.0);
        dto.setNote(transaction.getNote());
        dto.setSource(transaction.getSource());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        
        // Set security info if present
        if (transaction.getSecurity() != null) {
            dto.setSecurityUuid(transaction.getSecurity().getUUID());
            dto.setSecurityName(transaction.getSecurity().getName());
        }
        
        // Convert shares from internal representation to decimal
        dto.setShares(transaction.getShares() / 1000000.0);
        
        // Set owner info and transaction type
        if (pair.getOwner() instanceof name.abuchen.portfolio.model.Account) {
            name.abuchen.portfolio.model.Account account = (name.abuchen.portfolio.model.Account) pair.getOwner();
            dto.setOwnerUuid(account.getUUID());
            dto.setOwnerName(account.getName());
            dto.setTransactionType("ACCOUNT");
            
            if (transaction instanceof AccountTransaction) {
                AccountTransaction at = (AccountTransaction) transaction;
                dto.setType(at.getType().name());
            }
        } else if (pair.getOwner() instanceof name.abuchen.portfolio.model.Portfolio) {
            name.abuchen.portfolio.model.Portfolio portfolio = (name.abuchen.portfolio.model.Portfolio) pair.getOwner();
            dto.setOwnerUuid(portfolio.getUUID());
            dto.setOwnerName(portfolio.getName());
            dto.setTransactionType("PORTFOLIO");
            
            if (transaction instanceof PortfolioTransaction) {
                PortfolioTransaction pt = (PortfolioTransaction) transaction;
                dto.setType(pt.getType().name());
            }
        }
        
        return dto;
    }
}

