package org.av360.maverick.graph.feature.navigation.domain.impl;

import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.RequestDetails;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
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
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NavigationServicesImpl implements NavigationServices {

    private final EntityServices entityServices;

    private final SessionHistory sessionHistory;

    ValueFactory vf = SimpleValueFactory.getInstance();

    public NavigationServicesImpl(EntityServices entityServices, SessionHistory sessionHistory) {
        this.entityServices = entityServices;
        this.sessionHistory = sessionHistory;
    }

    @Override
    public Flux<AnnotatedStatement> start(Authentication authentication, WebSession session) {
        return ReactiveRequestUriContextHolder.getURI()
                .map(requestUrl -> {

                    ModelBuilder builder = new ModelBuilder();



                    Resource root = vf.createIRI(Local.URN_PREFIX, "nav");
                    Resource swaggerLink = vf.createIRI(Local.URN_PREFIX, "explorer");
                    Resource openApiLink = vf.createIRI(Local.URN_PREFIX, "specification"); //v3/api-docs

                    if(authentication.getDetails() instanceof RequestDetails details) {
                        this.sessionHistory.add(session.getId(), details.path());
                        this.sessionHistory.previous(session.getId()).map(path -> builder.add(root, HYDRA.PREVIOUS, "?"+path));
                    }


                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE)
                            .setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
                            .setNamespace(SDO.PREFIX, SDO.NAMESPACE)
                    ;

                    builder.subject(root)
                            .add(RDF.TYPE, HYDRA.API_DOCUMENTATION)
                            .add(HYDRA.TITLE, "Maverick.EntityGraph")
                            .add(HYDRA.DESCRIPTION, "Opinionated Web API to access linked data fragments in a knowledge graph.")
                            .add(HYDRA.ENTRYPOINT, ResolvableUrlPrefix+"/api/entities")
                            .add(HYDRA.VIEW, swaggerLink)
                            .add(HYDRA.VIEW, openApiLink);

                    builder.subject(swaggerLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Swagger UI to interact with the API")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                            .add(HYDRA.ENTRYPOINT, ResolvableUrlPrefix+"/webjars/swagger-ui/index.html?urls.primaryName=Entities API");
                    builder.subject(openApiLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Machine-readable OpenApi Documentation")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.APPLICATION_JSON_VALUE))
                            .add(HYDRA.ENTRYPOINT, ResolvableUrlPrefix+"/v3/api-docs");
                    return builder.build();
                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())).collect(Collectors.toSet()))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<AnnotatedStatement> browse(MultiValueMap<String, String> params, Authentication authentication, WebSession session) {
        if (params.containsKey("entities")) {
            if (params.getFirst("entities").equalsIgnoreCase("list"))
                return this.list(params, authentication, session);
            else if (StringUtils.hasLength(params.getFirst("entities"))) {
                return this.entityServices.find(params.getFirst("entities"), null, authentication).flatMapIterable(TripleModel::asStatements);
            }
        }
        if (params.containsKey("entities") && params.getFirst("entities").equalsIgnoreCase("list")) {
            return this.entityServices.list(authentication, 100, 0).flatMapIterable(TripleModel::asStatements);
        }

        return Flux.empty();
    }


    public Flux<AnnotatedStatement> list(MultiValueMap<String, String> params, Authentication authentication, WebSession session) {
        int limit = Optional.ofNullable(params.getFirst("limit")).map(Integer::parseInt).orElse(50);
        int offset = Optional.ofNullable(params.getFirst("offset")).map(Integer::parseInt).orElse(0);

        return this.entityServices.list(authentication, limit, offset)
                .collectList()
                .map(list -> {
                    ModelBuilder builder = new ModelBuilder();
                    IRI nodeCollection = vf.createIRI(ResolvableUrlPrefix+"/api/entities");
                    IRI nodePaging = vf.createIRI(String.format(ResolvableUrlPrefix+"/api/entities?limit=%s&offset=%s", limit, offset));

                    if(authentication.getDetails() instanceof RequestDetails details) {
                        this.sessionHistory.add(session.getId(), details.path());
                        this.sessionHistory.previous(session.getId()).map(path -> builder.add(nodeCollection, HYDRA.PREVIOUS, "?"+path));
                    }

                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE);
                    builder.add(nodeCollection, RDF.TYPE, HYDRA.COLLECTION);
                    builder.add(nodeCollection, HYDRA.VIEW, nodePaging);
                    builder.add(nodeCollection, HYDRA.PREVIOUS, ResolvableUrlPrefix+"/nav");

                    // create navigation
                    builder.subject(nodePaging);
                    builder.add(RDF.TYPE, HYDRA.PARTIAL_COLLECTION_VIEW);
                    builder.add(HYDRA.PREVIOUS, String.format(ResolvableUrlPrefix+"/api/entities?limit=%s&offset=%s", limit, Math.max(offset - limit, 0)));
                    builder.add(HYDRA.NEXT, String.format(ResolvableUrlPrefix+"/api/entities?limit=%s&offset=%s", limit, offset+limit));
                    builder.add(HYDRA.FIRST, String.format(ResolvableUrlPrefix+"/api/entities?limit=%s&offset=%s", limit, 0));

                    list.forEach(rdfEntity -> {
                        builder.add(HYDRA.MEMBER, rdfEntity.getIdentifier());
                        builder.build().getNamespaces().addAll(rdfEntity.getNamespaces());
                        builder.build().addAll(rdfEntity.getModel());
                    });

                    return builder.build();

                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())))
                .flatMapMany(Flux::fromStream);

    }
}
