package com.heisenbug.claims.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI claimsOpenApi(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Powered Insurance Claims Processing Platform — API")
                        .description("""
                                Backend API for the claims platform. Authoritative behavior contract lives in \
                                `docs/FRS.md` v0.2; realization decisions in `docs/TSD.md` v1.1. Error codes \
                                follow the `E-<DOMAIN>-<NNN>` convention from FRS §3 and the `_NOT_FOUND` / \
                                `VALIDATION_FAILED` / `<DOMAIN>_CONFLICT` conventions enforced by \
                                `GlobalExceptionHandler`.""")
                        .version("v1")
                        .contact(new Contact()
                                .name("Heisenbug Hunters")
                                .email("monark@amnex.com"))
                        .license(new License().name("Internal — Amnex Sarjan Hackathon 2026")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local dev"),
                        new Server().url("https://api.claims.qa.internal").description("QA"),
                        new Server().url("https://api.claims.uat.internal").description("UAT"),
                        new Server().url("https://api.claims.internal").description("Production")))
                .tags(List.of(
                        new Tag().name("claims").description("FNOL + claim lifecycle (FR-2, FR-8, FR-9)"),
                        new Tag().name("rbac-users").description("User directory & role assignments (FR-1, §3)"),
                        new Tag().name("rbac-roles").description("Role management & permission sets (§3)"),
                        new Tag().name("rbac-permissions").description("Seeded capability × scope catalog (§3.2)"),
                        new Tag().name("rbac-doa-matrix").description("DOA limits by role × LOB × geo (M-26)")))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Short-lived JWT (≤15 min) from FR-1; surveyor tokens carry a `licence_id` claim.")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME));
    }
}
