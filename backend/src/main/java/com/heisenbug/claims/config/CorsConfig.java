package com.heisenbug.claims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String[] allowedOriginPatterns;

    public CorsConfig(@Value("${cors.allowed-origins:http://localhost:4200}") String originsCsv,
                      @Value("${cors.allowed-origin-patterns:}") String patternsCsv) {
        this.allowedOrigins = splitCsv(originsCsv);
        this.allowedOriginPatterns = splitCsv(patternsCsv);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var api = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);

        if (allowedOrigins.length > 0) {
            api.allowedOrigins(allowedOrigins);
        }
        if (allowedOriginPatterns.length > 0) {
            api.allowedOriginPatterns(allowedOriginPatterns);
        }

        var actuator = registry.addMapping("/actuator/**")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        if (allowedOrigins.length > 0) {
            actuator.allowedOrigins(allowedOrigins);
        }
        if (allowedOriginPatterns.length > 0) {
            actuator.allowedOriginPatterns(allowedOriginPatterns);
        }
    }

    private static String[] splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return new String[0];
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
