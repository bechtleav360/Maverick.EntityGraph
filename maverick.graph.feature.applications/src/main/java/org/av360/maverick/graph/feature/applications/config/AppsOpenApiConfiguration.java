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
                .name(Globals.HEADER_APPLICATION_LABEL)
                .description("Label of application this operation should apply to. You can query a list of all available applications using the endpoint '/api/applications'")
                .addExample(Globals.DEFAULT_APPLICATION_LABEL, new Example().value(Globals.DEFAULT_APPLICATION_LABEL));

        if (Objects.nonNull(this.adminApiBuilder)) {
            adminApiBuilder.addOperationCustomizer((ops, method)
                    -> ops.addParametersItem(header));
        }

        if (Objects.nonNull(this.queryApiBuilder)) {
            queryApiBuilder.addOperationCustomizer((ops, method)
                    -> ops.addParametersItem(header));
        }
        if (Objects.nonNull(this.entityApiBuilder)) {
            entityApiBuilder.addOperationCustomizer((ops, method) -> {
                if(Objects.nonNull(ops.getParameters()) && ops.getParameters().stream().anyMatch(parameter -> parameter.getName().equalsIgnoreCase(Globals.HEADER_APPLICATION_LABEL))) { // since we override
                    return ops;
                } else return ops.addParametersItem(header);
            });
        }

    }
}