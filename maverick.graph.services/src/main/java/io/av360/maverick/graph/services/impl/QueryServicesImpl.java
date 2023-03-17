package io.av360.maverick.graph.services.impl;

import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.services.SchemaServices;
import io.av360.maverick.graph.services.transformers.DelegatingTransformer;
import io.av360.maverick.graph.store.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Slf4j(topic = "graph.srvc.query")
public class QueryServicesImpl implements QueryServices {

    private final EntityStore entityStore;

    private final SchemaServices schemaServices;

    private final QueryParser queryParser;
    public QueryServicesImpl(EntityStore graph, SchemaServices schemaServices) {
        this.entityStore = graph;
        this.schemaServices = schemaServices;

        queryParser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
    }


    @Override
    public Flux<BindingSet> queryValues(String query, Authentication authentication) {
        return Mono.just(query)
                .map(q -> queryParser.parseQuery(query, null))
                .flatMapMany(ptq -> this.entityStore.query(query, authentication))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running select query in entity store: \n {}", query);
                });
    }

    @Override
    public Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication) {
        return this.entityStore.query(query.getQueryString(), authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running select query in entity store: \n {}", query);
                });
    }

    @Override
    public Flux<NamespaceAwareStatement> queryGraph(String queryStr, Authentication authentication) {
        return Mono.just(queryStr)
                .map(q -> queryParser.parseQuery(queryStr, null))
                .flatMapMany(ptq -> this.entityStore.construct(queryStr, authentication))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running select query in entity store: \n {}", queryStr);
                });

    }

    public Flux<NamespaceAwareStatement> queryGraph(ConstructQuery query, Authentication authentication) {
        return this.entityStore.construct(query.getQueryString(), authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running construct query in entity store: \n {}", query.getQueryString());
                });
    }











    @Autowired
    public void linkTransformers(DelegatingTransformer transformers) {
        transformers.registerQueryService(this);
    }


}
