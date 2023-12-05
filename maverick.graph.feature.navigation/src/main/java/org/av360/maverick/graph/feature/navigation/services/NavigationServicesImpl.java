package org.av360.maverick.graph.feature.navigation.services;

import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NavigationServicesImpl implements NavigationServices {

    ValueFactory vf = SimpleValueFactory.getInstance();
    @Value("${application.features.modules.navigation.configuration.limit:100}")
    int defaultLimit = 100;
    private EntityServices entityServices;
    private IRI NAVIGATION_CONTEXT = Values.iri("urn:nav");
    private IRI DATA_CONTEXT = Values.iri("urn:data");

    private Map<UUID, List<String>> history = new HashMap<>();

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> start(SessionContext ctx) {
        return ReactiveRequestUriContextHolder.getURI()
                .map(requestUrl -> {

                    ModelBuilder builder = new ModelBuilder();

                    Resource apiDocsNode = Values.bnode();
                    Resource swaggerLink = Values.bnode();
                    Resource openApiLink = Values.bnode();
                    Resource navView = Values.bnode();

                    /*
                    if(ctx.getDetails() instanceof RequestDetails details) {
                        this.sessionHistory.add(session.getId(), details.path());
                        this.sessionHistory.previous(session.getId()).map(path -> builder.add(root, HYDRA.PREVIOUS, "?"+path));
                    }
                    */


                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE)
                            .setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
                            .setNamespace(SDO.PREFIX, SDO.NAMESPACE)
                    ;

                    builder.namedGraph(NAVIGATION_CONTEXT);

                    builder.subject(apiDocsNode)
                            .add(RDF.TYPE, HYDRA.API_DOCUMENTATION)
                            .add(HYDRA.TITLE, "Maverick.EntityGraph")
                            .add(HYDRA.DESCRIPTION, "Opinionated Web API to access linked data fragments in a knowledge graph.")
                            .add(HYDRA.ENTRYPOINT, ("?/api"))
                            .add(HYDRA.VIEW, swaggerLink)
                            .add(HYDRA.VIEW, openApiLink)
                            .add(HYDRA.VIEW, navView);

                    // we start a new session
                    UUID sessionId = UUID.randomUUID();
                    this.history.put(sessionId, new ArrayList<>());
                    builder.subject(Values.bnode()).add(Values.iri("urn:session"), sessionId);

                    builder.subject(navView)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Navigating through the graph")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                            .add(HYDRA.ENTRYPOINT, this.generateResolvableIRI(""));
                    builder.subject(swaggerLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Swagger UI to interact with the API")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                            .add(HYDRA.ENTRYPOINT, this.generateResolvableIRI("/webjars/swagger-ui/index.html", Map.of("urls.primaryName", "Entities API")));
                    builder.subject(openApiLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Machine-readable OpenApi Documentation")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.APPLICATION_JSON_VALUE))
                            .add(HYDRA.ENTRYPOINT, this.generateResolvableIRI("/v3/api-docs"));
                    return builder.build();
                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())).collect(Collectors.toSet()))
                .flatMapMany(Flux::fromIterable);
    }


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx) {
        if (params.containsKey("entities")) {
            if (params.get("entities").equalsIgnoreCase("list"))
                return this.list(params, ctx, null);
            else if (params.get("entities").equalsIgnoreCase("view"))
                return this.view(params, ctx);
            else if (StringUtils.hasLength(params.get("entities"))) {
                throw new NotImplementedException();

            }
        }

        return Flux.empty();
    }

    @Override
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> view(Map<String, String> params, SessionContext ctx) {
        return this.entityServices.find(params.get("key"), null, true, 0, ctx)
                .map(fragment -> {
                    ModelBuilder builder = new ModelBuilder();
                    // IRI currentView = this.generateResolvableIRI("/api/entities/%s".formatted(((IRI) fragment.getIdentifier()).getLocalName()));
                    Resource currentView = Values.bnode();
                    builder.namedGraph(NAVIGATION_CONTEXT);
                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE);
                    builder.add(currentView, RDF.TYPE, HYDRA.VIEW);
                    builder.add(currentView, HYDRA.DESCRIPTION, "Details of a linked data fragment.");
                    if (ctx.getRequestDetails().isPresent() && ctx.getRequestDetails().get().getHeaders().containsKey("X-MEG-PREVIOUS")) {
                        String previous_url_string = ctx.getRequestDetails().get().getHeaders().get("X-MEG-PREVIOUS");
                        URI uri = URI.create(previous_url_string);
                        builder.add(currentView, HYDRA.PREVIOUS, this.generateResolvableIRI("/" + uri.getPath()));
                    } else {
                        builder.add(currentView, HYDRA.PREVIOUS, this.generateResolvableIRI(""));
                    }


                    builder.namedGraph(DATA_CONTEXT);
                    builder.build().getNamespaces().addAll(fragment.getNamespaces());
                    builder.build().addAll(fragment.getModel());
                    return builder.build();
                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())))
                .flatMapMany(Flux::fromStream);
    }


    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> list(Map<String, String> params, SessionContext ctx, String query) {
        Integer limit = Optional.ofNullable(params.get("limit")).map(Integer::parseInt).orElse(defaultLimit);
        Integer offset = Optional.ofNullable(params.get("offset")).map(Integer::parseInt).orElse(0);
        params.put("limit", limit.toString());
        params.put("offset", offset.toString());

        return Mono.zip(
                        this.entityServices.count(ctx),
                        this.entityServices.list(limit, offset, ctx, query).collectList()
                )
                .map(tuple -> {
                    Long count = tuple.getT1();
                    List<RdfFragment> list = tuple.getT2();


                    ModelBuilder builder = new ModelBuilder(new TreeModel());
                    Resource nodeCollection = Values.bnode("collection"); // this.generateResolvableIRI("/api/entities");
                    Resource nodePaging = Values.bnode("paging"); // this.generateResolvableIRI("/api/entities", params);

                    builder.namedGraph(NAVIGATION_CONTEXT);
                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE);
                    builder.add(nodeCollection, RDF.TYPE, HYDRA.COLLECTION);
                    builder.add(nodeCollection, HYDRA.TOTAL_ITEMS, Values.literal(count));
                    builder.add(nodeCollection, HYDRA.VIEW, nodePaging);
                    builder.add(nodeCollection, HYDRA.PREVIOUS, this.generateResolvableIRI(""));

                    // create navigation
                    builder.subject(nodePaging);
                    builder.add(RDF.TYPE, HYDRA.PARTIAL_COLLECTION_VIEW);

                    builder.add(HYDRA.LIMIT, limit);
                    builder.add(HYDRA.OFFSET, offset);
                    if (list.size() >= limit) {
                        Map<String, String> urlParameters = new HashMap<>(params);
                        urlParameters.put("offset", (offset + limit) + "");
                        builder.add(HYDRA.NEXT, this.generateResolvableIRI("entities", urlParameters));
                    }
                    if (offset > 0) {
                        Map<String, String> urlParameters = new HashMap<>(params);
                        urlParameters.put("offset", Math.max(offset - limit, 0) + "");
                        builder.add(HYDRA.PREVIOUS, this.generateResolvableIRI("entities", urlParameters));
                    }
                    if (offset > limit) {
                        Map<String, String> urlParameters = new HashMap<>(params);
                        urlParameters.put("offset", "0");
                        builder.add(HYDRA.FIRST, this.generateResolvableIRI("entities", urlParameters));
                    }


                    builder.namedGraph(DATA_CONTEXT);
                    list.forEach(rdfEntity -> {
                        builder.build().getNamespaces().addAll(rdfEntity.getNamespaces());
                        builder.build().addAll(rdfEntity.getModel());
                    });

                    return builder.build();

                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())))
                .flatMapMany(Flux::fromStream);
    }

    public IRI generateResolvableIRI(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(ResolvableUrlPrefix);
        // FIXME: scope infection, should be handled in delegating
        if (!path.startsWith("/")) {
            sb.append("/nav");
        }
        if (params.containsKey("scope")) {
            sb.append("/s/").append(params.get("scope"));
            params.remove("scope");
        }
        if (StringUtils.hasLength(path)) {
            sb.append("/").append(path);
        }


        Iterator<Map.Entry<String, String>> entriesItr = params.entrySet().iterator();
        if (entriesItr.hasNext()) sb.append("?");
        while (entriesItr.hasNext()) {
            Map.Entry<String, String> next = entriesItr.next();
            sb.append(next.getKey()).append("=").append(next.getValue());
            if (entriesItr.hasNext()) sb.append("&");
        }

        return vf.createIRI(sb.toString());
    }


    @Autowired
    public void setEntityServices(EntityServices entityServices) {
        this.entityServices = entityServices;
    }
}
