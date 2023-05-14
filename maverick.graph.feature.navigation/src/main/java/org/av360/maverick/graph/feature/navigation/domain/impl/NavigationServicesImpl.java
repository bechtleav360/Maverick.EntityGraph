package org.av360.maverick.graph.feature.navigation.domain.impl;

import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
public class NavigationServicesImpl implements NavigationServices {

    private final EntityServices entityServices;

    ValueFactory vf = SimpleValueFactory.getInstance();

    public NavigationServicesImpl(EntityServices entityServices) {
        this.entityServices = entityServices;
    }

    @Override
    public Flux<AnnotatedStatement> start(Authentication authentication) {
        return ReactiveRequestUriContextHolder.getURI()
                .map(requestUrl -> {
                    ModelBuilder builder = new ModelBuilder();

                    Resource root = vf.createIRI(Local.NAMESPACE, "Start");
                    Resource swaggerLink = vf.createIRI(Local.NAMESPACE, "SwaggerUI");
                    Resource openApiLink = vf.createIRI(Local.NAMESPACE, "OpenApiDocument"); //v3/api-docs

                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE)
                            .setNamespace(RDF.PREFIX, RDF.NAMESPACE)
                            .setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
                            .setNamespace(Local.PREFIX, Local.NAMESPACE)
                            .setNamespace("entity", "?/api/entities")
                    ;

                    builder.subject(root)
                            .add(RDF.TYPE, HYDRA.API_DOCUMENTATION)
                            .add(HYDRA.TITLE, "Maverick.EntityGraph")
                            .add(HYDRA.DESCRIPTION, "Opinionated Web API to access linked data fragments in a knowledge graph.")
                            .add(HYDRA.ENTRYPOINT, "?/api")
                            .add(vf.createIRI(Local.NAMESPACE, "swagger"), swaggerLink)
                            .add(vf.createIRI(Local.NAMESPACE, "openApi"), openApiLink);

                    builder.subject(swaggerLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Swagger UI to interact with the API")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                            .add(HYDRA.ENTRYPOINT, "?/webjars/swagger-ui/index.html?urls.primaryName=Entities API");
                    builder.subject(openApiLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Machine-readable OpenApi Documentation")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.APPLICATION_JSON_VALUE))
                            .add(HYDRA.ENTRYPOINT, "?/v3/api-docs");
                    return builder.build();
                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())).collect(Collectors.toSet()))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<AnnotatedStatement> browse(MultiValueMap<String, String> params, Authentication authentication) {
        if(params.containsKey("entities")) {
            if(params.getFirst("entities").equalsIgnoreCase("list"))
                return this.entityServices.list(authentication, 100, 0).flatMapIterable(TripleModel::asStatements);
            else if (StringUtils.hasLength(params.getFirst("entities"))) {
                return this.entityServices.find(params.getFirst("entities"), null, authentication).flatMapIterable(TripleModel::asStatements)   ;
            }
        }
        if(params.containsKey("entities") && params.getFirst("entities").equalsIgnoreCase("list")) {
            return this.entityServices.list(authentication, 100, 0).flatMapIterable(TripleModel::asStatements);
        }

        return Flux.empty();
    }
}
