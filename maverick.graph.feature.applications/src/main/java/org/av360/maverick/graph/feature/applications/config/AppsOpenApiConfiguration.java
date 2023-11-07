package org.av360.maverick.graph.feature.applications.config;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
public class AppsOpenApiConfiguration implements InitializingBean {

    @Autowired(required = false)
    @Qualifier("AdminApiDefinitionBuilder")
    private GroupedOpenApi.Builder adminApiBuilder;

    @Autowired(required = false)
    @Qualifier("EntityApiDefinitionBuilder")
    private GroupedOpenApi.Builder entityApiBuilder;

    @Autowired(required = false)
    @Qualifier("QueryApiDefinitionBuilder")
    private GroupedOpenApi.Builder queryApiBuilder;

    @Bean("ApplicationsApiDefinition")
    public GroupedOpenApi applicationsApiDefinition(@Value("${info.app.version:unknown}") String version) {
        return GroupedOpenApi.builder()
                .group("Applications API")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info().title("Maverick.EntityGraph Applications API").description("API to register applications and generate or revoke Api Keys (part of multi-tenancy feature). Requires admin authentication.").version(version));
                })
                .pathsToMatch("/api/applications/**")
                .build();
    }


    @Override
    public void afterPropertiesSet() {
        Parameter header = new HeaderParameter()
                .name("X-Application")
                .description("Label of application this operation should apply to. You can query a list of all available applications using the endpoint '/api/applications'")
                .addExample("default", new Example().value("default"));

        if (Objects.nonNull(this.adminApiBuilder)) {
            adminApiBuilder.addOperationCustomizer((ops, method)
                    -> ops.addParametersItem(header));
        }

        if (Objects.nonNull(this.queryApiBuilder)) {
            queryApiBuilder.addOperationCustomizer((ops, method)
                    -> ops.addParametersItem(header));
        }


        if (Objects.nonNull(this.entityApiBuilder)) {
            List<String> paths = new ArrayList<>(entityApiBuilder.build().getPathsToMatch());
            paths.add("/api/app/**/entities/**");

            entityApiBuilder
                    .addOperationCustomizer((ops, method) -> ops.addParametersItem(header))
                    .pathsToMatch(paths.toArray(new String[0]));

        }

    }
}