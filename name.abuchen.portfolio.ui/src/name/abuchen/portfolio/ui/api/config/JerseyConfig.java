package name.abuchen.portfolio.ui.api.config;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import name.abuchen.portfolio.ui.api.controller.HelloWorldController;
import name.abuchen.portfolio.ui.api.controller.MockPortfolioController;

/**
 * Jersey configuration for JSON support in the Portfolio Performance API server.
 * Uses Jersey 2.x with manual resource registration to avoid HK2 dependency issues.
 */
public class JerseyConfig extends ResourceConfig {
    
    /**
     * Provider to configure ObjectMapper with Java 8 date/time support
     */
    @Provider
    public static class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
        private final ObjectMapper mapper;

        public ObjectMapperContextResolver() {
            mapper = new ObjectMapper();
            // Register JavaTimeModule to handle Java 8 date/time types like LocalDateTime
            mapper.registerModule(new JavaTimeModule());
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return mapper;
        }
    }
    
    public JerseyConfig() {
        // Register Jackson for JSON processing
        register(JacksonFeature.class);
        
        // Register our custom ObjectMapper provider
        register(ObjectMapperContextResolver.class);
        
        // Manually register controllers to avoid package scanning issues
        register(HelloWorldController.class);
        register(MockPortfolioController.class);
        
        // Disable Bean Validation to avoid javax.validation dependency in OSGi
        property(ServerProperties.BV_FEATURE_DISABLE, true);

        // Disable WADL generation to avoid potential issues
        property("jersey.config.server.wadl.disable", "true");
    }
}