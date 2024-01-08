package org.av360.maverick.graph.feature.navigation.services;

import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.*;
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

@Service
public class NavigationServicesImpl implements NavigationServices {

    ValueFactory vf = SimpleValueFactory.getInstance();
    @Value("${application.features.modules.navigation.configuration.limit:100}")
    int defaultLimit = 100;
    private EntityServices entityServices;


    private Map<UUID, List<String>> history = new HashMap<>();

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> start(SessionContext ctx) {
        return ReactiveRequestUriContextHolder.getURI()
                .map(requestUrl -> {

                    ModelBuilder builder = new ModelBuilder();

                    Resource apiDocsNode = Values.bnode("apiDocs");
                    Resource swaggerLink = Values.bnode("swagger");
                    Resource openApiLink = Values.bnode("openAPI");
                    Resource navView = Values.bnode("navigation");

                    /*


                    if(ctx.getDetails() instanceof RequestDetails details) {
                        this.sessionHistory.add(session.getId(), details.path());
                        this.sessionHistory.previous(session.getId()).map(path -> builder.add(root, HYDRA.PREVIOUS, "?"+path));
                    }
                    */

                    builder.namedGraph(NavigationServices.NAVIGATION_CONTEXT);
                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE)
                            .setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
                            .setNamespace(SDO.PREFIX, SDO.NAMESPACE)
                            .setNamespace(Local.PREFIX, Local.URN_PREFIX)
                    ;

                    // builder.namedGraph(NAVIGATION_CONTEXT);

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
                            .add(HYDRA.ENTRYPOINT, "?/nav");
                    builder.subject(swaggerLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Swagger UI to interact with the API")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                            .add(HYDRA.ENTRYPOINT, "?/webjars/swagger-ui/index.html");
                    builder.subject(openApiLink)
                            .add(RDF.TYPE, HYDRA.LINK)
                            .add(HYDRA.TITLE, "Machine-readable OpenApi Documentation")
                            .add(HYDRA.RETURNS, vf.createLiteral(MediaType.APPLICATION_JSON_VALUE))
                            .add(HYDRA.ENTRYPOINT, "?/v3/api-docs");
                    return builder.build();
                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())))
                .flatMapMany(Flux::fromStream);
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
                    builder.build().getNamespaces().addAll(fragment.getNamespaces());

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
                    fragment.streamStatements()
                            .filter(statement -> ! statement.getSubject().isTriple())
                            .forEach(statement -> builder.add(statement.getSubject(), statement.getPredicate(), statement.getObject()));

                    builder.namedGraph(DETAILS_CONTEXT);
                    fragment.streamStatements()
                            .filter(statement -> statement.getSubject().isTriple())
                            .forEach(statement -> builder.add(statement.getSubject(), statement.getPredicate(), statement.getObject()));

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
                    builder.add(nodeCollection, HYDRA.ENTRYPOINT, this.generateResolvableIRI(""));

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
                    Model resultingModel = builder.build();


                    builder.namedGraph(DATA_CONTEXT);
                    list.forEach(rdfEntity -> {
                        resultingModel.getNamespaces().addAll(rdfEntity.getNamespaces());
                        resultingModel.add(nodeCollection, HYDRA.MEMBER, rdfEntity.getIdentifier(), NAVIGATION_CONTEXT);
                        this.generateEntityViewURL(rdfEntity.getIdentifier()).ifPresent(literal -> {
                            resultingModel.add(rdfEntity.getIdentifier(), HYDRA.VIEW, literal, NAVIGATION_CONTEXT);
                        });


                        rdfEntity.streamStatements()
                                .forEach(s -> resultingModel.add(s.getSubject(), s.getPredicate(), s.getObject(), DATA_CONTEXT));
                    });

                    return builder.build();

                })
                .map(model -> model.stream().map(statement -> AnnotatedStatement.wrap(statement, model.getNamespaces())))
                .flatMapMany(Flux::fromStream);
    }

    private Optional<Literal> generateEntityViewURL(Resource identifier) {
        if(identifier instanceof IRI iri) {
            String localName = iri.getLocalName();
            String[] split = localName.split("\\.");
            Literal literal = null;
            if(split.length == 2) {
                literal = Values.literal("?/nav/s/%s/entities/%s".formatted(split[0], split[1]));
            } else {
                literal = Values.literal("?/nav/s/default/entities/%s".formatted(localName));
            }
            return Optional.ofNullable(literal);

        } else return Optional.empty();
    }

    public Literal generateResolvableIRI(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder("?");
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

        return Values.literal(sb.toString());
    }


    @Autowired
    public void setEntityServices(EntityServices entityServices) {
        this.entityServices = entityServices;
    }
}
