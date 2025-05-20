package net.kadookie.aptusassist.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotEnvConfig implements EnvironmentPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DotEnvConfig.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String envDirectory = environment.getProperty("spring.profiles.active", "").equals("prod") ? "." : "../";
        try {
            Dotenv dotenv = Dotenv.configure()
                    // .directory(envDirectory)
                    .directory("/resources")
                    .filename(".env")
                    .load();
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> dotenvMap.put(entry.getKey(), entry.getValue()));
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenvProperties", dotenvMap));
            logger.info("Loaded .env file from {}", envDirectory + "/.env");
            logger.info("APTUS_USERNAME: {}", dotenv.get("APTUS_USERNAME"));
            logger.info("APTUS_BASE_URL: {}", dotenv.get("APTUS_BASE_URL"));
        } catch (Exception e) {
            logger.error("Failed to load .env file from {}: {}", envDirectory + "/.env", e.getMessage());
            throw e;
        }
    }
}