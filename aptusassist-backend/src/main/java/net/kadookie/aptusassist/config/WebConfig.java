package net.kadookie.aptusassist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC configuration for the spa booking application.
 * <p>
 * Configuration Scope:
 * <ul>
 * <li>CORS policies for frontend integration</li>
 * <li>Request/response handling customization</li>
 * <li>Interceptor registration</li>
 * </ul>
 * <p>
 * Security Considerations:
 * <ul>
 * <li>Development CORS is permissive (* origins)</li>
 * <li>Production should restrict allowedOrigins</li>
 * <li>Allowed methods limited to GET/POST</li>
 * <li>Headers restricted to Content-Type</li>
 * </ul>
 * <p>
 * Future Extensions:
 * <ul>
 * <li>Add CSRF protection</li>
 * <li>Implement request logging</li>
 * <li>Add response compression</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * Configures CORS mappings for the application.
     * <p>
     * Current settings:
     * <ul>
     * <li>All paths (/**)</li>
     * <li>All origins (*) - development only</li>
     * <li>Allowed methods: GET, POST</li>
     * <li>Allowed headers: Content-Type</li>
     * </ul>
     *
     * @param registry CorsRegistry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type");
    }
}