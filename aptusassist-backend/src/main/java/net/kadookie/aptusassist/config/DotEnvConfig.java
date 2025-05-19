package net.kadookie.aptusassist.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotEnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(DotEnvConfig.class);

    @Bean
    public Dotenv dotenv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("../") // Load from AptusAssist/ (parent of aptusassist-backend/)
                .filename(".env")
                .load();
        logger.debug("Loaded .env file from AptusAssist/.env");

        return dotenv;
    }
}