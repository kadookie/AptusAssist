package net.kadookie.aptusassist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC configuration for the booking application.
 * <p>
 * Configuration Scope:
 * <ul>
 * <li>CORS policies for frontend integration</li>
 * <li>Request/response handling customization</li>
 * <li>Interceptor registration</li>
 * </ul>
 * <p>
 * Current CORS Mappings:
 * <ul>
 * <li>/book endpoint - POST only from localhost:3737</li>
 * <li>/slots endpoint - GET only from localhost:3737</li>
 * </ul>
 * <p>
 * Security Considerations:
 * <ul>
 * <li>Development allows only localhost:3737</li>
 * <li>Production should be configured via application.properties</li>
 * <li>Allowed methods limited to GET/POST</li>
 * <li>Headers restricted to Content-Type</li>
 * </ul>
 * <p>
 * Implementation Details:
 * <ul>
 * <li>Uses Spring's CorsRegistry for configuration</li>
 * <li>Mappings are path-specific for granular control</li>
 * <li>Headers are explicitly whitelisted</li>
 * </ul>
 * <p>
 * Future Extensions:
 * <ul>
 * <li>Add CSRF protection</li>
 * <li>Implement request logging</li>
 * <li>Add response compression</li>
 * </ul>
 *
 * @see org.springframework.web.servlet.config.annotation.CorsRegistry
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
        /**
         * Configures path-specific CORS mappings for the application.
         * <p>
         * Implementation Details:
         * <ul>
         * <li>Separate mappings for /book (POST) and /slots (GET)</li>
         * <li>Origins restricted to localhost:3737 (frontend dev server)</li>
         * <li>Content-Type header required for all requests</li>
         * <li>No credentials support (stateless API)</li>
         * </ul>
         * <p>
         * Production Configuration:
         * <ul>
         * <li>Override allowedOrigins via application.properties</li>
         * <li>Consider adding maxAge for preflight caching</li>
         * </ul>
         *
         * @param registry CorsRegistry to configure mappings
         * @see org.springframework.web.servlet.config.annotation.CorsRegistration
         */
        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/book")
                                .allowedOrigins("http://localhost")
                                .allowedMethods("POST")
                                .allowedHeaders("Content-Type");

                registry.addMapping("/slots")
                                .allowedOrigins("http://localhost")
                                .allowedMethods("GET")
                                .allowedHeaders("Content-Type");
        }

}