package name.abuchen.portfolio.ui.api.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Hello World Controller for the Portfolio Performance API server.
 * Provides a simple hello world endpoint for testing.
 */
@Path("/api")
public class HelloWorldController {

    /**
     * Hello world endpoint that returns a JSON response.
     * 
     * @return Hello world message with timestamp and version
     */
    @GET
    @Path("/hello")
    @Produces(MediaType.APPLICATION_JSON)
    public Response hello() {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "Hello from Portfolio Performance API!");
        responseData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        responseData.put("version", "1.0.0");
        responseData.put("status", "running");
        
        return Response.ok(responseData).build();
    }
}
