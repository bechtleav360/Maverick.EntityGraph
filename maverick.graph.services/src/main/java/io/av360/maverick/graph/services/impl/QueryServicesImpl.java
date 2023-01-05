package io.av360.maverick.graph.services.impl;

import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.services.transformers.DelegatingTransformer;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.rdf.models.Entity;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@Slf4j(topic = "graph.service.query")
public class QueryServicesImpl implements QueryServices {

    private final EntityStore entityStore;

    public QueryServicesImpl(EntityStore graph) {
        this.entityStore = graph;
    }


    @Override
    public Flux<BindingSet> queryValues(String query, Authentication authentication) {
        return this.entityStore.query(query, authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running query in entity store.");
                });
    }

    @Override
    public Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication) {
        return this.queryValues(query.getQueryString(), authentication);
    }

    @Override
    public Flux<NamespaceAwareStatement> queryGraph(String query, Authentication authentication) {
        return this.entityStore.construct(query, authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled()) log.trace("Running query in entity store.");
                });

    }
    public Flux<NamespaceAwareStatement> queryGraph(ConstructQuery query, String applicationId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }


    public Flux<Entity> listEntities(Authentication authentication) {
        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery query = Queries.SELECT(idVariable).where(
                idVariable.isA(Local.Entities.TYPE));

        return this.queryValues(query.getQueryString(), authentication)
                .map(bindings -> (IRI) bindings.getValue(idVariable.getVarName()))
                .flatMap(id -> this.entityStore.getEntity(id, authentication));
    }

    public Flux<Entity> listEntities(Authentication authentication, int limit, int offset) {
        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery query = Queries.SELECT(idVariable).where(
                idVariable.isA(Local.Entities.TYPE)).limit(limit).offset(offset);

        return this.queryValues(query.getQueryString(), authentication)
                .map(bindings -> (IRI) bindings.getValue(idVariable.getVarName()))
                .flatMap(id -> this.entityStore.getEntity(id, authentication));
    }


    @Autowired
    public void linkTransformers(DelegatingTransformer transformers) {
        transformers.registerQueryService(this);
    }


}
