package io.av360.maverick.graph.feature.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.List;

@Configuration
public class AdminOpenApiConfiguration {
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