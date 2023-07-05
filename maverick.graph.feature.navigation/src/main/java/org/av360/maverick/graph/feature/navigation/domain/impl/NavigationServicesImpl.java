package org.av360.maverick.graph.feature.navigation.domain.impl;

import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NavigationServicesImpl implements NavigationServices {

    private EntityServices entityServices;

    ValueFactory vf = SimpleValueFactory.getInstance();


    @Value("${application.features.modules.navigation.configuration.limit:100}")
    int defaultLimit = 100;


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> start(SessionContext ctx) {
        return ReactiveRequestUriContextHolder.getURI()
                .map(requestUrl -> {

                    ModelBuilder builder = new ModelBuilder();

                    Resource root = vf.createIRI(Local.URN_PREFIX, "nav");
                    Resource swaggerLink = vf.createIRI(Local.URN_PREFIX, "explorer");
                    Resource openApiLink = vf.createIRI(Local.URN_PREFIX, "specification"); //v3/api-docs

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

                    builder.subject(root)
                            .add(RDF.TYPE, HYDRA.API_DOCUMENTATION)
                            .add(HYDRA.TITLE, "Maverick.EntityGraph")
                            .add(HYDRA.DESCRIPTION, "Opinionated Web API to access linked data fragments in a knowledge graph.")
                            .add(HYDRA.ENTRYPOINT, this.generateResolvableIRI("/api/entities"))
                            .add(HYDRA.VIEW, swaggerLink)
                            .add(HYDRA.VIEW, openApiLink);

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
    public Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx) {
        if (params.containsKey("entities")) {
            if (params.get("entities").equalsIgnoreCase("list"))
                return this.list(params, ctx);
            else if (StringUtils.hasLength(params.get("entities"))) {
                return this.entityServices.find(params.get("entities"), null, ctx).flatMapIterable(TripleModel::asStatements);
            }
        }

        return Flux.empty();
    }



    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> list(Map<String, String> params, SessionContext ctx) {
        Integer limit = Optional.ofNullable(params.get("limit")).map(Integer::parseInt).orElse(defaultLimit);
        Integer offset = Optional.ofNullable(params.get("offset")).map(Integer::parseInt).orElse(0);
        params.put("limit", limit.toString());
        params.put("offset", offset.toString());



        return this.entityServices.list(limit, offset, ctx)
                .collectList()
                .map(list -> {
                    ModelBuilder builder = new ModelBuilder();
                    IRI nodeCollection = this.generateResolvableIRI("/api/entities");
                    IRI nodePaging = this.generateResolvableIRI("/api/entities",  params);

                    builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE);
                    builder.add(nodeCollection, RDF.TYPE, HYDRA.COLLECTION);
                    builder.add(nodeCollection, HYDRA.VIEW, nodePaging);
                    builder.add(nodeCollection, HYDRA.PREVIOUS, this.generateResolvableIRI("/nav"));

                    // create navigation
                    builder.subject(nodePaging);
                    builder.add(RDF.TYPE, HYDRA.PARTIAL_COLLECTION_VIEW);

                    builder.add(HYDRA.LIMIT, limit);
                    builder.add(HYDRA.OFFSET, offset);
                    if(offset > 0) {
                        params.put("offset", Math.max(offset - limit, 0)+"");
                        builder.add(HYDRA.PREVIOUS, this.generateResolvableIRI("/api/entities",  params));
                    }
                    if(offset > limit) {
                        params.put("offset", "0");
                        builder.add(HYDRA.FIRST, this.generateResolvableIRI("/api/entities",  params));
                    }
                    if(list.size() <= limit) {
                        params.put("offset", (offset+limit)+"");
                        builder.add(HYDRA.NEXT, this.generateResolvableIRI("/api/entities", params));
                    }

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

    public IRI generateResolvableIRI(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(ResolvableUrlPrefix);
        sb.append(path);

        Iterator<Map.Entry<String, String>> entriesItr = params.entrySet().iterator();
        if(entriesItr.hasNext()) sb.append("?");
        while(entriesItr.hasNext()) {
            Map.Entry<String, String> next = entriesItr.next();
            sb.append(next.getKey()).append("=").append(next.getValue());
            if(entriesItr.hasNext()) sb.append("&");
        }

        return vf.createIRI(sb.toString());
    }


    @Autowired
    public void setEntityServices(EntityServices entityServices) {
        this.entityServices = entityServices;
    }
}
