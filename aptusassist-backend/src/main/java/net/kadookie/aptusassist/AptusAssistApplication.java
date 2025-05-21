package net.kadookie.aptusassist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the AptusAssist Application.
 * <p>
 * Architecture Overview:
 * <ul>
 * <li>Spring Boot 3.3 application</li>
 * <li>Embedded Tomcat server</li>
 * <li>H2 file database</li>
 * <li>JPA/Hibernate ORM</li>
 * <li>RESTful API endpoints</li>
 * </ul>
 * <p>
 * Runtime Characteristics:
 * <ul>
 * <li>Default port: 8080</li>
 * <li>Startup time: ~5-10 seconds</li>
 * <li>Memory footprint: ~200MB</li>
 * <li>Thread pool: Tomcat defaults</li>
 * </ul>
 * <p>
 * Deployment Considerations:
 * <ul>
 * <li>Packaged as executable JAR</li>
 * <li>External database recommended for production</li>
 * <li>Configure JVM memory parameters</li>
 * <li>Enable HTTPS in production</li>
 * </ul>
 * <p>
 * Monitoring Recommendations:
 * <ul>
 * <li>Spring Boot Actuator endpoints</li>
 * <li>Log aggregation</li>
 * <li>Health checks</li>
 * <li>Performance metrics</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class AptusAssistApplication {
    private static final Logger logger = LoggerFactory.getLogger(AptusAssistApplication.class);

    /**
     * Application entry point.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Application");
        SpringApplication.run(AptusAssistApplication.class, args);
        logger.info("Application started successfully");
    }

}
