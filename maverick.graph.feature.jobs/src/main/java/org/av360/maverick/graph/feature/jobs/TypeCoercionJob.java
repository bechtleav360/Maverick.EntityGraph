package org.av360.maverick.graph.feature.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.types.InsertLocalTypes;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
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
public class TypeCoercionJob implements Job {
    private final EntityServices entityServices;
    private final QueryServices queryServices;

    private final InsertLocalTypes localTypesTransformer;
    private final TransactionsStore transactionsStore;

    public TypeCoercionJob(EntityServices entityServices, QueryServices queryServices, @Autowired(required = false) @Nullable InsertLocalTypes localTypesTransformer, TransactionsStore transactionsStore) {
        this.entityServices = entityServices;
        this.queryServices = queryServices;
        this.localTypesTransformer = localTypesTransformer;
        this.transactionsStore = transactionsStore;
    }

    @Override
    public String getName() {
        return "typeCoercion";
    }

    public Mono<Void> run(Authentication authentication) {
        if(Objects.isNull(this.localTypesTransformer)) return Mono.error(new InvalidConfiguration("Type Coercion Transformer is disabled"));


        return this.findCandidates(authentication)
                .doOnNext(res -> log.trace("Convert type of resource with id '{}'", res.stringValue()))
                .flatMap(res -> this.loadFragment(authentication, res))
                .flatMap(localTypesTransformer::getStatements)
                .collect(new ModelCollector())
                .doOnNext(model -> log.trace("Collected {} statements for new types", model.size()))
                .flatMap(model -> this.entityServices.getStore().insert(model, new RdfTransaction()))
                .flatMapMany(trx -> this.entityServices.getStore().commit(trx, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .flatMap(transaction -> this.transactionsStore.store(List.of(transaction), authentication))
                .doOnError(throwable -> {
                    log.error("Exception while finding and replacing subject identifiers: {}", throwable.getMessage());
                })
                .doOnSubscribe(sub -> {
                    log.debug("Checking for external or anonymous subject identifiers.");
                })
                .then();

    }

    private Flux<Statement> getTypeStatements(Model model) {
        return localTypesTransformer.getStatements(model);
    }

    private Mono<Model> loadFragment(Authentication authentication, Resource value) {
        return this.entityServices.getStore().listStatements(value, null, null, authentication)
                .map(statements -> statements.stream().collect(new ModelCollector()));


    }

    private Flux<Resource> findCandidates(Authentication authentication) {
        /*
               SELECT ?entity WHERE
                    { ?entity a ?type .
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Individual> . }
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Classifier> . }
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Embedded> . } }
               LIMIT 500


               SELECT ?entity WHERE
                    { ?entity a ?type .
                      FILTER NOT EXISTS {  FILTER STRSTARTS(str(?entity), "urn:pwid:meg:e")  }
               LIMIT 500

               SELECT ?entity WHERE { ?entity a ?type . FILTER NOT EXISTS {} FILTER ( STRSTARTS( ?type, "urn:pwid:meg" ) ) } LIMIT 500

              Variable entity = SparqlBuilder.var("entity");
                Variable type = SparqlBuilder.var("type");
                Expression<?> function = Expressions.function(SparqlFunction.STRSTARTS, Expressions.str(type), Rdf.literalOf(Local.URN_PREFIX));
                SelectQuery query = Queries.SELECT(entity)
                .where(entity.isA(type).filter(Expressions.not(function))
                ).limit(500);
        */
        String tpl = """
                SELECT DISTINCT ?entity WHERE {
                    ?entity a ?type .
                    FILTER NOT EXISTS {
                      FILTER STRSTARTS(str(?type), "%s")
                    }
                } LIMIT 500
                """;
        String query = String.format(tpl, Local.URN_PREFIX);
        return this.queryServices.queryValues(query, authentication)
                .map(bindings -> bindings.getValue("entity"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }


}
