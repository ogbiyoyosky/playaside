package com.playvora.playvora_api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * Jackson configuration for Java 8 time types support.
 * This ensures OffsetDateTime and other Java 8 time types are properly serialized/deserialized in UTC.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Register Java 8 time module for OffsetDateTime support
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        objectMapper.registerModule(javaTimeModule);

        // Disable writing dates as timestamps (use ISO-8601 strings instead)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Set timezone to UTC for all date/time serialization
        objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));

        return objectMapper;
    }
}

