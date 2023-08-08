package org.av360.maverick.graph.feature.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.transformers.types.AssignLocalTypes;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.MergingModelCollector;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * <p>
 * Type coercion detects linked data fragments within a repository and tries to infer
 * - individuals (entities)
 * - classifier (shared embedded)
 * - embedded (single use embedded)
 * </p>
 * <p>
 * Individual are fragments with characteristic properties which induce uniqueness.
 * Classifiers are concepts used to categorize or cluster the individuals.
 * A fragment is a collection of statements with a common subject. This job queries for fragments which are neither
 * individual nor classifier. The entity api serves only individuals.
 *</p>
 */
@Service
@Slf4j(topic = "graph.jobs.coercion")
@SuppressWarnings("javadoc")
public class AssignInternalTypesJob extends Job {

    public static String NAME = "typeCoercion";
    private final EntityServices entityServices;
    private final QueryServices queryServices;

    private  final TransactionsService transactionsService;

    private final AssignLocalTypes localTypesTransformer;
    private final TransactionsStore transactionsStore;

    public AssignInternalTypesJob(EntityServices entityServices, QueryServices queryServices, TransactionsService transactionsService, @Autowired(required = false) @Nullable AssignLocalTypes localTypesTransformer, TransactionsStore transactionsStore) {
        this.entityServices = entityServices;
        this.queryServices = queryServices;
        this.transactionsService = transactionsService;
        this.localTypesTransformer = localTypesTransformer;
        this.transactionsStore = transactionsStore;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public Mono<Void> run(SessionContext ctx) {
        if(Objects.isNull(this.localTypesTransformer)) return Mono.error(new InvalidConfiguration("Type Coercion Transformer is disabled"));

        return this.findCandidates(ctx)
                .doOnNext(res -> log.trace("Convert type of resource with id '{}'", res.stringValue()))
                .flatMap(res -> this.loadFragment(ctx, res))
                .flatMap(fragment -> this.localTypesTransformer.handle(fragment, ctx.getEnvironment()))
                .collect(new MergingModelCollector())
                .doOnNext(model -> log.trace("Collected {} statements for new types", model.size()))
                .flatMap(model -> this.entityServices.getStore(ctx).insertModel(model, new RdfTransaction()))
                .flatMapMany(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .doOnNext(transaction -> Assert.isTrue(transaction.get().contains(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction))
                .buffer(100)
                .flatMap(transactions -> this.transactionsService.save(transactions, ctx))
                .doOnError(throwable -> log.error("Exception while assigning internal types: {}", throwable.getMessage()))
                .doOnSubscribe(sub -> {
                    log.trace("Checking for entities with missing internal type definitions.");
                    ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.ENTITIES));
                })
                .doOnComplete(() -> log.debug("Completed checking for entities with missing internal type definitions."))
                .then();

    }



    private Mono<Model> loadFragment(SessionContext ctx, Resource value) {
        return this.entityServices.getStore(ctx).listStatements(value, null, null, ctx.getEnvironment())
                .map(statements -> statements.stream().collect(new ModelCollector()));


    }

    private Flux<Resource> findCandidates(SessionContext ctx) {
        /*
               SELECT ?entity WHERE
                    { ?entity a ?type .
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Individual> . }
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Classifier> . }
                      FILTER NOT EXISTS { ?entity a <urn:pwid:meg:e:Embedded> . }
                      }
               LIMIT 500


               SELECT ?entity WHERE
                    { ?entity a ?type .
                      FILTER NOT EXISTS {  FILTER STRSTARTS(str(?entity), "urn:pwid:meg:e")  }
                      }
               LIMIT 500

        */
        String tpl = """
                SELECT DISTINCT ?entity WHERE {
                    ?entity a ?type .
                    FILTER NOT EXISTS { ?entity a <%s> . }
                    FILTER NOT EXISTS { ?entity a <%s> . }
                    FILTER NOT EXISTS { ?entity a <%s> . }
                } LIMIT 1000
                """;
        String query = String.format(tpl, Local.Entities.TYPE_INDIVIDUAL, Local.Entities.TYPE_CLASSIFIER, Local.Entities.TYPE_EMBEDDED);
        return this.queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(bindings -> bindings.getValue("entity"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }


}
