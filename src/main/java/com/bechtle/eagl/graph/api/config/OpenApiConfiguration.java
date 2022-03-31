package com.bechtle.eagl.graph.api.config;

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
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("Graph API")
                .apiInfo(new ApiInfoBuilder()
                        .title("Graph Service API")
                        .description("API to access the graph.")
                        .version("0.0.1-SNAPSHOT")
                        .license("Apache 2.0")
                        .licenseUrl("https://opensource.org/licenses/Apache-2.0")
                        .build())

                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.bechtle.eagl.graph.api.controller"))
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