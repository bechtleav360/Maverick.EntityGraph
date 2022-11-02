package io.av360.maverick.graph.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.net.URI;
import java.util.List;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
class OpenApiConfiguration {

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return route(GET("/"), req ->
                ServerResponse.temporaryRedirect(URI.create("/swagger-ui/")).build()
        );
    }

    @Bean
    public Docket entitiesDocket() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Entities API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Entity Graph API")
                        .description("API to access and update the entity graph.")
                        .version("0.1.0")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.av360.maverick.graph.api.controller.entities"))
                // .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    @Bean
    public Docket queryDocket() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Query API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Query Service API")
                        .description("API to run sparql queries.")
                        .version("0.3.0")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.av360.maverick.graph.api.controller.queries"))
                // .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    @Bean
    public Docket queryTransactions() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Transactions API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Transaction Services API")
                        .description("API to access transactions. All changes to entities are logged in transactions.")
                        .version("0.1.0")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.av360.maverick.graph.api.controller.transactions"))
                // .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).build();
        //.operationSelector(operationContext -> true)
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


}