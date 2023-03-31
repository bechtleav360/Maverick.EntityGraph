package org.av360.maverick.graph.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@OpenAPIDefinition
@SecurityScheme(name = "api_key", type = SecuritySchemeType.APIKEY, paramName = "X-API-KEY", in = SecuritySchemeIn.HEADER)
public class OpenApiConfiguration {
    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return route(GET("/swagger"), req -> ServerResponse.temporaryRedirect(URI.create("/swagger-ui.html")).build()
        ).and(route(GET("/actuator/"), req -> ServerResponse.temporaryRedirect(URI.create("/actuator")).build()));
    }





    @Bean("EntityApiDefinition")
    public GroupedOpenApi adminApiDefinition(@Qualifier("EntityApiDefinitionBuilder") GroupedOpenApi.Builder builder) {
        return builder.build();
    }

    @Bean("EntityApiDefinitionBuilder")
    public GroupedOpenApi.Builder entitiesAPIBuilder(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Entities API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Entity Graph API").description("API to access and update the entity graph.").version(version));
                })
                .pathsToMatch("/api/entities/**");
    }


    @Bean("QueryApiDefinition")
    public GroupedOpenApi queryApiDefinition(@Qualifier("QueryApiDefinitionBuilder") GroupedOpenApi.Builder builder) {
        return builder.build();
    }

    @Bean("QueryApiDefinitionBuilder")
    public GroupedOpenApi.Builder queryAPIBuilder(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Query API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Query Service API").description("API to run sparql queries.").version(version));
                })
                .pathsToMatch("/api/query/**");
    }

    @Bean("TransactionsApiDefinition")
    public GroupedOpenApi transactionsApiDefinition(@Qualifier("TransactionsApiDefinitionBuilder") GroupedOpenApi.Builder builder) {
        return builder.build();
    }

    @Bean("TransactionsApiDefinitionBuilder")
    public GroupedOpenApi.Builder transactionsAPIBuilder(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Transactions API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Transactions Service API").description("API to access transactions. All changes to entities are logged in transactions.").version(version));
                })
                .pathsToMatch("/api/transactions/**");
    }



}
