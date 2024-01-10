package org.av360.maverick.graph.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.av360.maverick.graph.model.enums.PropertyType;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;
import java.util.Arrays;

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

                    // change order of tags
                    Tag[] reordered = new Tag[4];
                    openApi.getTags().forEach(tg -> {
                        if(tg.getName().equalsIgnoreCase("Entities")) reordered[0] = tg;
                        if(tg.getName().equalsIgnoreCase("Values")) reordered[1] = tg;
                        if(tg.getName().equalsIgnoreCase("Relations")) reordered[2] = tg;
                        if(tg.getName().equalsIgnoreCase("Details")) reordered[3] = tg;
                    });
                    openApi.tags(Arrays.asList(reordered));
                })
                .pathsToMatch("/api/entities/**");
    }


    @Bean("QueryApiDefinition")
    public GroupedOpenApi queryApiDefinition(@Qualifier("QueryApiDefinitionBuilder") GroupedOpenApi.Builder builder) {
        return builder.build();
    }

    @Bean("QueryApiDefinitionBuilder")
    public GroupedOpenApi.Builder queryAPIBuilder(@Value("${info.app.version:unknown}") String version) {
        String description = """
                This Web API enables the execution of SPARQL queries, specifically tailored for SELECT and CONSTRUCT types, allowing users to interact with and retrieve data from the repositories.
                                
                #### Supported Query Types
                
                `SELECT`: Retrieves and lists data based on specified criteria.
                
                `CONSTRUCT`: Constructs new RDF graphs from existing dataset data.
                                
                #### Result Encoding
                All results are returned in Unicode encoding, ensuring broad compatibility with international characters and symbols. 
                Make sure you attach the correct encoding ("text/csv; charset=utf-8") to your Accept Header. 
                                
                #### Query Limitations
                Users should respect the imposed query limits to ensure service stability and responsiveness.
                Overly complex or large queries may be restricted to maintain system performance.
                                
                """;

        return GroupedOpenApi.builder()
                .group("Query API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("Query Service API")
                            .description(description).version(version));
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

    @Bean
    public Converter<String, PropertyType> convertPropertyType() {
        return new Converter<String, PropertyType>() {
            @Override
            public PropertyType convert(String source) {
                return PropertyType.valueOf(source.toUpperCase());
            }
        };
    }


}
