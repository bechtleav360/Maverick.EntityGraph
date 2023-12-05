package org.av360.maverick.graph.feature.applications.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import org.av360.maverick.graph.api.security.errors.UnknownApiKey;
import org.av360.maverick.graph.feature.applications.model.domain.QueryVariables;
import org.av360.maverick.graph.feature.applications.model.domain.Subscription;
import org.av360.maverick.graph.feature.applications.model.events.TokenCreatedEvent;
import org.av360.maverick.graph.feature.applications.model.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.model.vocab.SubscriptionTerms;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.identifier.RandomIdentifier;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

/**
 * Applications separate tenants. Each node has its own separate stores.
 * An node has a set of unique API Keys. The api key identifies the node.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class SubscriptionsService {



    private final ApplicationsStore applicationsStore;

    private final ApplicationsService applicationsService;

    private final ApplicationEventPublisher eventPublisher;


    public SubscriptionsService(ApplicationsStore applicationsStore, ApplicationsService applicationsService, ApplicationEventPublisher eventPublisher) {
        this.applicationsStore = applicationsStore;
        this.applicationsService = applicationsService;
        this.eventPublisher = eventPublisher;
    }


    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Subscription> getSubscription(String subscriptionIdentifier, SessionContext ctx) {

        SelectQuery q = Queries.SELECT()
                .where(QueryVariables.varNodeSubscription.has(SubscriptionTerms.HAS_KEY, subscriptionIdentifier)
                        .andHas(SubscriptionTerms.HAS_LABEL, QueryVariables.varSubLabel)
                        .andHas(SubscriptionTerms.HAS_ISSUE_DATE, QueryVariables.varSubIssued)
                        .andHas(SubscriptionTerms.IS_ACTIVE, QueryVariables.varSubActive)
                        .andHas(SubscriptionTerms.FOR_APPLICATION, QueryVariables.varAppKey)
                        .and(QueryVariables.varNodeApplication.has(ApplicationTerms.HAS_KEY, QueryVariables.varAppKey)
                                .andHas(ApplicationTerms.IS_PERSISTENT, QueryVariables.varAppFlagPersistent)
                                .andHas(ApplicationTerms.IS_PUBLIC, QueryVariables.varAppFlagPublic)
                                .andHas(ApplicationTerms.HAS_LABEL, QueryVariables.varAppLabel)
                        )
                );
        return this.applicationsStore.query(q, ctx.getEnvironment())
                .singleOrEmpty()
                .map(BindingsAccessor::new)
                .flatMap(QueryVariables::buildSubscriptionFromBindings)
                .switchIfEmpty(Mono.error(new UnknownApiKey(subscriptionIdentifier)))
                .filter(Subscription::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(subscriptionIdentifier)))
                .doOnSubscribe(subs -> log.debug("Requesting node details for node key '{}'", subscriptionIdentifier));


    }

    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Flux<Subscription> listSubscriptionsForApplication(String applicationKey, SessionContext ctx) {

        return this.applicationsService.getApplication(applicationKey, ctx)
                .flatMapMany(app -> {
                    SelectQuery q = Queries.SELECT()
                            .where(QueryVariables.varNodeSubscription.has(SubscriptionTerms.HAS_KEY, QueryVariables.varSubKey)
                                    .andHas(SubscriptionTerms.HAS_LABEL, QueryVariables.varSubLabel)
                                    .andHas(SubscriptionTerms.HAS_ISSUE_DATE, QueryVariables.varSubIssued)
                                    .andHas(SubscriptionTerms.IS_ACTIVE, QueryVariables.varSubActive)
                                    .andHas(SubscriptionTerms.FOR_APPLICATION, app.key())
                            );

                    return this.applicationsStore.query(q, ctx.getEnvironment())
                            .map(BindingsAccessor::new)
                            .flatMap(ba -> QueryVariables.buildSubscriptionFromBindings(ba, app));
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Requesting all API Keys for node with key '{}'", applicationKey));
    }


    public Mono<Void> revokeToken(String subscriptionId, String name, SessionContext ctx) {
        log.debug("Revoking api key for node '{}'", subscriptionId);

        return Mono.error(new UnsupportedOperationException());
    }
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Subscription> createSubscription(String applicationKey, String subscriptionLabel, SessionContext ctx) {

        return this.applicationsService.getApplication(applicationKey, ctx)
                .map(application ->
                        new Subscription(
                                IdentifierServices.buildRandomIRI(Local.Applications.NAME),
                                subscriptionLabel,
                                RandomIdentifier.generateRandomKey(16),
                                true,
                                ZonedDateTime.now().toString(),
                                application
                        )
                )
                .flatMap(apiKey -> {
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.subject(apiKey.iri());
                    modelBuilder.add(RDF.TYPE, SubscriptionTerms.TYPE);
                    modelBuilder.add(SubscriptionTerms.HAS_KEY, apiKey.key());
                    modelBuilder.add(SubscriptionTerms.HAS_LABEL, apiKey.label());
                    modelBuilder.add(SubscriptionTerms.HAS_ISSUE_DATE, apiKey.issueDate());
                    modelBuilder.add(SubscriptionTerms.IS_ACTIVE, apiKey.active());
                    modelBuilder.add(SubscriptionTerms.FOR_APPLICATION, apiKey.application().key());
                    modelBuilder.add(apiKey.application().iri(), ApplicationTerms.HAS_API_KEY, apiKey.iri());

                    Transaction transaction = new RdfTransaction().forInsert(modelBuilder.build());

                    return this.applicationsStore.commit(transaction, ctx.getEnvironment())
                            .flatMap(Transaction::verifyCompleted)
                            .then(Mono.just(apiKey));
                })
                .doOnSuccess(token -> {
                    this.eventPublisher.publishEvent(new TokenCreatedEvent(token));
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Generating new subscription key for node '{}'", applicationKey));
    }


}
