package io.av360.maverick.graph.feature.admin.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminOpenApiConfiguration {

    @Bean
    public GroupedOpenApi adminApiDefinition(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Admin API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Admin API").description("API for admin operations (part of admin feature).").version(version));
                })
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /*
    @Bean
    public Docket adminApiDocket() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Admin API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Maverick.EntityGraph Admin API")
                        .description("API for admin operations (part of admin feature).")
                        .version("0.0.1-SNAPSHOT")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.av360.maverick.graph.feature.admin.api"))
                .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return List.of(new SecurityReference("X-API-KEY", authorizationScopes));
    }

    private ApiKey apiKey() {
        return new ApiKey("X-API-KEY", "X-API-KEY", "header");
    }
     */

}