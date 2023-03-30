package org.av360.maverick.graph.feature.jobs.services;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.setIndividual.InsertLocalTypes;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Type coercion detects linked data fragments within a repository and tries to infer
 * - individuals (entities)
 * - classifier
 *
 * Individual are fragments with characteristic properties which induce uniqueness.
 * Classifiers are concepts used to categorize or cluster the individuals.
 *
 * A fragment is a collection of statements with a common subject. This job queries for fragments which are neither
 * individual nor classifier. The entity api serves only individuals.
 *
 */
@Service
@Slf4j(topic = "graph.jobs.coercion")
public class TypeCoercionService  {
    private final EntityServices entityServices;
    private final QueryServices queryServices;

    private final InsertLocalTypes localTypesTransformer;

    public TypeCoercionService(EntityServices entityServices, QueryServices queryServices, @Autowired(required = false) @Nullable InsertLocalTypes localTypesTransformer) {
        this.entityServices = entityServices;
        this.queryServices = queryServices;
        this.localTypesTransformer = localTypesTransformer;
    }

    public Mono<Void> run(Authentication authentication) {
        if(Objects.isNull(this.localTypesTransformer)) return Mono.error(new InvalidConfiguration("Type Coercion Transformer is disabled"));


        return this.findCandidates(authentication)
                .doOnNext(value -> log.trace("Identified value with id '{}' ", value.stringValue()))
                .flatMap(value -> this.loadFragment(authentication, value))
                .flatMap(localTypesTransformer::getStatements)
                .collect(new ModelCollector())
                .doOnNext(model -> log.trace("Collected {} statements for new types", model.size()))
                .flatMap(model -> this.entityServices.getStore().insert(model, authentication, Authorities.SYSTEM))
                .then();

    }

    private Flux<Statement> getTypeStatements(Model model) {
        return localTypesTransformer.getStatements(model);
    }

    private Mono<Model> loadFragment(Authentication authentication, Value value) {

        Variable s = SparqlBuilder.var("s");
        Variable p = SparqlBuilder.var("p");
        Variable o = SparqlBuilder.var("o");
        ConstructQuery q = Queries.CONSTRUCT().where(GraphPatterns.tp(s, p, o));

        ModelBuilder modelBuilder = new ModelBuilder();

        return this.queryServices.queryGraph(q, authentication)
                .map(sts -> modelBuilder.add(sts.getSubject(), sts.getPredicate(), sts.getObject()))
                .then(Mono.just(modelBuilder.build()));

    }

    private Flux<IRI> findCandidates(Authentication authentication) {
                /*
        SELECT ?entity ?type
        WHERE {
          ?entity a ?type
          FILTER NOT EXISTS {
            ?entity rdf:type <http://example.com/type>
          }
        } LIMIT 100
        */


        Variable entity = SparqlBuilder.var("entity");
        Variable type = SparqlBuilder.var("type");
        SelectQuery query = Queries.SELECT(entity)
                .where(entity.isA(type)
                        .filterNotExists(entity.isA(Local.Entities.INDIVIDUAL))
                        .filterNotExists(entity.isA(Local.Entities.CLASSIFIER))
                        .filterNotExists(entity.isA(Local.Entities.EMBEDDED))
                ).limit(500);
        return this.queryServices.queryValues(query, authentication)
                .map(bindings -> bindings.getValue(entity.getVarName()))
                .filter(Value::isIRI)
                .map(value -> (IRI) value);
    }


}
