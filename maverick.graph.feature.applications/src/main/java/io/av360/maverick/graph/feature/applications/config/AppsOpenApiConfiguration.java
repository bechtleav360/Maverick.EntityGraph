package io.av360.maverick.graph.feature.applications.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppsOpenApiConfiguration {

    @Bean
    public GroupedOpenApi applicationsApiDefinition(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Applications API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Maverick.EntityGraph Applications API").description("API to register applications and generate or revoke Api Keys (part of multi-tenancy feature). Requires admin authentication.").version(version));
                })
                .pathsToMatch("/api/applications/**", "/api/sc/**/entities/**")
                .build();
    }





}