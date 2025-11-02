package co.cobre.cbmm.accounts.adapters.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for JSON serialization/deserialization
 * Includes support for Java Records and Java Time API
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java Time module for ZonedDateTime, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());

        // Use snake_case for property names
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail on unknown properties to allow flexibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}

