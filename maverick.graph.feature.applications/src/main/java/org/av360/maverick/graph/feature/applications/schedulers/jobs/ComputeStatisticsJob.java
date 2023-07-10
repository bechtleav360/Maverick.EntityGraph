package org.av360.maverick.graph.feature.applications.schedulers.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.transformers.types.AssignLocalTypes;
import org.av360.maverick.graph.store.TransactionsStore;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.math.BigInteger;

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
public class ComputeStatisticsJob implements Job {

    public static String NAME = "computeStatistics";
    private final ApplicationsService applicationsService;
    private final QueryServices queryServices;

    private  final TransactionsService transactionsService;

    private final AssignLocalTypes localTypesTransformer;
    private final TransactionsStore transactionsStore;

    public ComputeStatisticsJob(ApplicationsService applicationsService, QueryServices queryServices, TransactionsService transactionsService, @Autowired(required = false) @Nullable AssignLocalTypes localTypesTransformer, TransactionsStore transactionsStore) {
        this.applicationsService = applicationsService;
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
        return this.applicationsService.getApplicationByLabel(ctx.getEnvironment().getScope().label(), ctx)
                        .flatMap(application ->
                                this.countIndividuals(ctx)
                                .map(count -> this.applicationsService.setMetric(application, "count_individuals", count.intValue(), ctx))
                                .then(Mono.just(application))
                        ).flatMap(application ->
                        this.countIndividuals(ctx)
                                .map(count -> this.applicationsService.setMetric(application, "count_individuals", count.intValue(), ctx))
                                .then(Mono.just(application))
                        ).then();


    }

    private Mono<BigInteger> countIndividuals(SessionContext ctx) {
        String q = """
                SELECT( COUNT(?entity ) as ?count) WHERE { ?entity a <urn:pwid:meg:e:Individual> }
                """;
        return this.count(q, ctx);
    }

    private Mono<BigInteger> countClassifier(SessionContext ctx) {
        String q = """
                SELECT( COUNT(?entity ) as ?count) WHERE { ?entity a <urn:pwid:meg:e:Classifier> }
                """;
        return this.count(q, ctx);
    }


    private Mono<BigInteger> count(String query, SessionContext ctx) {
        return this.queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(bindings -> bindings.getValue("count"))
                .filter(Value::isLiteral)
                .map(value -> (Literal)value)
                .map(Literal::integerValue)
                .next()
                .switchIfEmpty(Mono.just(BigInteger.ZERO));
    }

}
