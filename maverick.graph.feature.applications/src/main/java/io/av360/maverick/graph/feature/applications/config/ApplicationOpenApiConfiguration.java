package io.av360.maverick.graph.feature.applications.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ApplicationOpenApiConfiguration {

/*    @Bean
    public Docket applicationApiDocket() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Applications API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Maverick.EntityGraph Applications API")
                        .description("API to register applications and generate or revoke Api Keys (part of multi-tenancy feature). Requires admin authentication. ")
                        .version("0.2.0")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.av360.maverick.graph.feature.applications.api"))
                // .paths(PathSelectors.ant("/api/**"))
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