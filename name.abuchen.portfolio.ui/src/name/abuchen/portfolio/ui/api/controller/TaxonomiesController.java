package name.abuchen.portfolio.ui.api.controller;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.api.dto.AssignmentDto;
import name.abuchen.portfolio.ui.api.dto.ClassificationDto;
import name.abuchen.portfolio.ui.api.dto.TaxonomyDto;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode;

/**
 * REST Controller for taxonomy operations.
 * 
 * This controller provides endpoints to manage taxonomies and their classifications within a portfolio.
 */
@Path("/api/v1/portfolios/{portfolioId}/taxonomies")
public class TaxonomiesController extends BaseController {
    
    /**
     * Get all taxonomies in a portfolio.
     * 
     * @param portfolioId The portfolio ID
     * @return List of all taxonomies with their classifications
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTaxonomies(@PathParam("portfolioId") String portfolioId) {
        try {
            logger.info("Getting all taxonomies for portfolio: {}", portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing taxonomies");
            }
            
            // Get all taxonomies and convert to DTOs
            List<TaxonomyDto> taxonomies = convertTaxonomiesToDto(client);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("count", taxonomies.size());
            response.put("taxonomies", taxonomies);
            
            logger.info("Returning {} taxonomies for portfolio {}", taxonomies.size(), portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting taxonomies for portfolio {}: {}", 
                portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Get a specific taxonomy by ID.
     * 
     * @param portfolioId The portfolio ID
     * @param taxonomyId The taxonomy ID
     * @return Taxonomy details with classifications
     */
    @GET
    @Path("/{taxonomyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTaxonomyById(@PathParam("portfolioId") String portfolioId,
                                    @PathParam("taxonomyId") String taxonomyId) {
        try {
            logger.info("Getting taxonomy {} for portfolio {}", taxonomyId, portfolioId);
            
            // Get the cached Client for this portfolio
            Client client = portfolioFileService.getPortfolio(portfolioId);
            
            if (client == null) {
                logger.warn("No cached client found for portfolio: {}", portfolioId);
                return createPreconditionRequiredResponse(
                    "PORTFOLIO_NOT_LOADED", 
                    "Portfolio must be opened first before accessing taxonomies");
            }
            
            // Find the taxonomy by ID
            name.abuchen.portfolio.model.Taxonomy taxonomy = client.getTaxonomies().stream()
                .filter(t -> taxonomyId.equals(t.getId()))
                .findFirst()
                .orElse(null);
            
            if (taxonomy == null) {
                logger.warn("Taxonomy not found: {} in portfolio: {}", taxonomyId, portfolioId);
                return createErrorResponse(Response.Status.NOT_FOUND, 
                    "Taxonomy not found", 
                    "Taxonomy with ID " + taxonomyId + " not found in portfolio");
            }
            
            // Convert to DTO
            TaxonomyDto taxonomyDto = convertTaxonomyToDto(taxonomy, client);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("portfolioId", portfolioId);
            response.put("taxonomy", taxonomyDto);
            
            logger.info("Returning taxonomy {} ({}) for portfolio {}", 
                taxonomy.getName(), taxonomyId, portfolioId);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Unexpected error getting taxonomy {} for portfolio {}: {}", 
                taxonomyId, portfolioId, e.getMessage(), e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "Internal server error", 
                e.getMessage());
        }
    }
    
    /**
     * Create a new taxonomy.
     * TODO: Implement taxonomy creation
     * 
     * @param portfolioId The portfolio ID
     * @param taxonomyData Taxonomy data
     * @return Created taxonomy
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTaxonomy(@PathParam("portfolioId") String portfolioId,
                                   Map<String, Object> taxonomyData) {
        // TODO: Implement taxonomy creation
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Taxonomy creation not yet implemented");
    }
    
    /**
     * Update an existing taxonomy.
     * TODO: Implement taxonomy update
     * 
     * @param portfolioId The portfolio ID
     * @param taxonomyId The taxonomy ID
     * @param taxonomyData Updated taxonomy data
     * @return Updated taxonomy
     */
    @PUT
    @Path("/{taxonomyId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTaxonomy(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("taxonomyId") String taxonomyId,
                                   Map<String, Object> taxonomyData) {
        // TODO: Implement taxonomy update
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Taxonomy update not yet implemented");
    }
    
    /**
     * Delete a taxonomy.
     * TODO: Implement taxonomy deletion
     * 
     * @param portfolioId The portfolio ID
     * @param taxonomyId The taxonomy ID
     * @return Deletion confirmation
     */
    @DELETE
    @Path("/{taxonomyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTaxonomy(@PathParam("portfolioId") String portfolioId,
                                   @PathParam("taxonomyId") String taxonomyId) {
        // TODO: Implement taxonomy deletion
        return createErrorResponse(Response.Status.NOT_IMPLEMENTED, 
            "Not implemented", 
            "Taxonomy deletion not yet implemented");
    }
    
    // ===== Helper Methods (Migrated from PortfolioFileService) =====
    
    /**
     * Helper method to convert all taxonomies from client to DTOs.
     * Migrated from PortfolioFileService.loadTaxonomies()
     */
    private List<TaxonomyDto> convertTaxonomiesToDto(Client client) {
        List<TaxonomyDto> taxonomyDtos = new ArrayList<>();
        
        // Create factory for exchange rates
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
        
        for (name.abuchen.portfolio.model.Taxonomy taxonomy : client.getTaxonomies()) {
            taxonomyDtos.add(convertTaxonomyToDto(taxonomy, client));
        }
        
        return taxonomyDtos;
    }
    
    /**
     * Helper method to convert a single Taxonomy to TaxonomyDto.
     */
    private TaxonomyDto convertTaxonomyToDto(name.abuchen.portfolio.model.Taxonomy taxonomy, Client client) {
        TaxonomyDto dto = new TaxonomyDto();
        dto.setId(taxonomy.getId());
        dto.setName(taxonomy.getName());
        dto.setSource(taxonomy.getSource());
        dto.setDimensions(taxonomy.getDimensions());
        dto.setClassificationsCount(taxonomy.getAllClassifications().size());
        dto.setHeight(taxonomy.getHeigth());
        
        // Create factory for exchange rates
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
        
        // Create TaxonomyModel to calculate actual values and proportions
        TaxonomyModel model = new TaxonomyModel(factory, client, taxonomy);
        
        // Convert the root classification
        if (taxonomy.getRoot() != null) {
            dto.setRoot(convertClassification(taxonomy.getRoot(), model.getClassificationRootNode()));
        }
        
        return dto;
    }
    
    /**
     * Recursively converts a Classification to a ClassificationDto.
     * Migrated from PortfolioFileService.convertClassification()
     * 
     * @param classification The classification to convert
     * @param taxonomyNode The corresponding TaxonomyNode (used to calculate proportions)
     * @return The converted ClassificationDto
     */
    private ClassificationDto convertClassification(name.abuchen.portfolio.model.Classification classification, 
                                                   TaxonomyNode taxonomyNode) {
        ClassificationDto dto = new ClassificationDto();
        dto.setId(classification.getId());
        dto.setName(classification.getName());
        dto.setDescription(classification.getNote());
        dto.setColor(classification.getColor());
        dto.setWeight(classification.getWeight());
        dto.setRank(classification.getRank());
        dto.setKey(classification.getKey());
        
        // Calculate proportion (Actual %) for this classification relative to its parent
        if (taxonomyNode != null && taxonomyNode.getParent() != null) {
            TaxonomyNode parentNode = taxonomyNode.getParent();
            if (parentNode.getActual() != null && parentNode.getActual().getAmount() > 0 &&
                taxonomyNode.getActual() != null) {
                long parentActualAmount = parentNode.getActual().getAmount();
                long actualAmount = taxonomyNode.getActual().getAmount();
                double proportion = (double) actualAmount / (double) parentActualAmount;
                dto.setProportion(proportion);
            }
        }
        
        // Get parent actual amount for calculating assignment proportions
        long parentActual = taxonomyNode != null && taxonomyNode.getActual() != null 
            ? taxonomyNode.getActual().getAmount() : 0;
        
        // Convert assignments
        List<AssignmentDto> assignmentDtos = new ArrayList<>();
        for (name.abuchen.portfolio.model.Classification.Assignment assignment : classification.getAssignments()) {
            AssignmentDto assignmentDto = new AssignmentDto();
            assignmentDto.setInvestmentVehicleUuid(assignment.getInvestmentVehicle().getUUID());
            assignmentDto.setInvestmentVehicleName(assignment.getInvestmentVehicle().getName());
            assignmentDto.setWeight(assignment.getWeight());
            assignmentDto.setRank(assignment.getRank());
            
            // Calculate proportion (Actual %) from TaxonomyNode
            if (taxonomyNode != null && parentActual > 0) {
                TaxonomyNode assignmentNode = findAssignmentNode(taxonomyNode, assignment);
                if (assignmentNode != null && assignmentNode.getActual() != null) {
                    long actual = assignmentNode.getActual().getAmount();
                    double proportion = (double) actual / (double) parentActual;
                    assignmentDto.setProportion(proportion);
                }
            }
            
            assignmentDtos.add(assignmentDto);
        }
        dto.setAssignments(assignmentDtos);
        
        // Recursively convert children
        List<ClassificationDto> childrenDtos = new ArrayList<>();
        for (name.abuchen.portfolio.model.Classification child : classification.getChildren()) {
            // Find the corresponding child node
            TaxonomyNode childNode = findChildNode(taxonomyNode, child);
            childrenDtos.add(convertClassification(child, childNode));
        }
        dto.setChildren(childrenDtos);
        
        return dto;
    }
    
    /**
     * Finds the assignment node that corresponds to the given assignment.
     * Migrated from PortfolioFileService.findAssignmentNode()
     * 
     * @param parentNode The parent taxonomy node
     * @param assignment The assignment to find
     * @return The matching TaxonomyNode or null if not found
     */
    private TaxonomyNode findAssignmentNode(TaxonomyNode parentNode, 
                                           name.abuchen.portfolio.model.Classification.Assignment assignment) {
        if (parentNode == null) return null;
        
        for (TaxonomyNode child : parentNode.getChildren()) {
            if (child.isAssignment() && 
                child.getAssignment().getInvestmentVehicle().equals(assignment.getInvestmentVehicle())) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * Finds the child taxonomy node that corresponds to the given classification.
     * Migrated from PortfolioFileService.findChildNode()
     * 
     * @param parentNode The parent taxonomy node
     * @param classification The classification to find
     * @return The matching TaxonomyNode or null if not found
     */
    private TaxonomyNode findChildNode(TaxonomyNode parentNode, 
                                      name.abuchen.portfolio.model.Classification classification) {
        if (parentNode == null) return null;
        
        for (TaxonomyNode child : parentNode.getChildren()) {
            if (child.isClassification() && 
                child.getClassification().getId().equals(classification.getId())) {
                return child;
            }
        }
        return null;
    }
}

