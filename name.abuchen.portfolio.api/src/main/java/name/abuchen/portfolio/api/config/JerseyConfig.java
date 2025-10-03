package name.abuchen.portfolio.api.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jersey configuration for JSON support.
 */
public class JerseyConfig extends ResourceConfig {
    
    public JerseyConfig() {
        // Register Jackson for JSON processing
        register(JacksonFeature.class);
        
        // Configure ObjectMapper for Java 8 time support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        register(mapper);
        
        // Register controllers
        packages("name.abuchen.portfolio.api.controller");
    }
}
