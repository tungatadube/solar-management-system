package com.solar.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

@Configuration
@Slf4j
public class TimezoneConfig {

    private static final String TIMEZONE = "Australia/Adelaide";

    @PostConstruct
    public void init() {
        // Set the JVM default timezone
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
        log.info("Application timezone set to: {}", TIMEZONE);
        log.info("Default timezone is now: {}", TimeZone.getDefault().getID());
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();

        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't include timezone offset in serialization
        // LocalDateTime will be serialized as-is without timezone conversion

        return mapper;
    }
}
