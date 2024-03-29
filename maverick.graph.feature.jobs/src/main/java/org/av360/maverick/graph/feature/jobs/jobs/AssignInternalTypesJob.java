/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.feature.jobs.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.preprocessors.types.AssignLocalTypes;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.av360.maverick.graph.store.rdf.helpers.MergingModelCollector;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
@Component
@Slf4j(topic = "graph.jobs.coercion")
@SuppressWarnings("javadoc")
public class AssignInternalTypesJob implements ScheduledJob {

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
                .map(model -> new RdfTransaction().forInsert(model))
                .flatMapMany(trx -> this.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment()))
                .doOnNext(transaction -> Assert.isTrue(transaction.getModel().contains(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction))
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
        return this.entityServices.getStore(ctx).asFragmentable().getFragment(value, ctx.getEnvironment())
                .map(TripleModel::getModel);
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
                    FILTER NOT EXISTs { FILTER STRSTARTS (STR(?type), "http://www.w3.org/1999/02/22-rdf-syntax-ns") }
                } LIMIT 1000
                """;
        String query = String.format(tpl, Local.Entities.TYPE_INDIVIDUAL, Local.Entities.TYPE_CLASSIFIER, Local.Entities.TYPE_EMBEDDED);
        return this.queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(bindings -> bindings.getValue("entity"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }


}
