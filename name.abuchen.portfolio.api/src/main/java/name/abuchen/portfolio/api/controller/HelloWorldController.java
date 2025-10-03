package name.abuchen.portfolio.api.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;

import name.abuchen.portfolio.api.dto.HelloResponse;
import name.abuchen.portfolio.api.dto.HealthResponse;

/**
 * Hello World REST Controller for the Portfolio Performance API.
 * 
 * This controller provides basic endpoints to verify the API server is working
 * and can be extended to expose portfolio data and calculations.
 */
@Path("/api/v1")
public class HelloWorldController {


    /**
     * Health check endpoint to verify the API server is healthy.
     * 
     * @return Health status information
     */
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "UP - Portfolio Performance API";
    }

}
