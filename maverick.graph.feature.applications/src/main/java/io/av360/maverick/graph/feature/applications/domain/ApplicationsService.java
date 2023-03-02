package io.av360.maverick.graph.feature.applications.domain;

import io.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import io.av360.maverick.graph.api.security.errors.UnknownApiKey;
import io.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import io.av360.maverick.graph.feature.applications.domain.events.TokenCreatedEvent;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import io.av360.maverick.graph.feature.applications.domain.model.Subscription;
import io.av360.maverick.graph.feature.applications.domain.vocab.ApplicationTerms;
import io.av360.maverick.graph.feature.applications.domain.vocab.SubscriptionTerms;
import io.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import io.av360.maverick.graph.model.errors.DuplicateRecordsException;
import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Applications separate tenants. Each application has its own separate stores.
 * An application has a set of unique API Keys. The api key identifies the application.
 */
@Service
@Slf4j(topic = "graph.feature.apps.domain")
public class ApplicationsService {

    private final ApplicationsStore applicationsStore;

    private final ApplicationEventPublisher eventPublisher;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher) {
        this.applicationsStore = applicationsStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new application.
     *
     * @param label          Label for the application
     * @param flags            Flags for this application
     * @param authentication Current authentication information
     * @return New application as mono
     */
    public Mono<Application> createApplication(String label, ApplicationFlags flags, Authentication authentication) {

        // generate application identifier
        String applicationIdentifier = GeneratedIdentifier.generateRandomKey(16);
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, applicationIdentifier);

        Application application = new Application(
                subject,
                label,
                applicationIdentifier,
                flags
        );

        // store application
        ModelBuilder modelBuilder = new ModelBuilder();

        modelBuilder.subject(application.iri());
        modelBuilder.add(RDF.TYPE, ApplicationTerms.TYPE);
        modelBuilder.add(ApplicationTerms.HAS_KEY, application.key());
        modelBuilder.add(ApplicationTerms.HAS_LABEL, application.label());
        modelBuilder.add(ApplicationTerms.IS_PERSISTENT, flags.isPersistent());
        modelBuilder.add(ApplicationTerms.IS_PUBLIC, flags.isPublic());


        return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.SYSTEM)
                .then(Mono.just(application))
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationCreatedEvent(app));
                })
                .doOnSubscribe(subs -> log.debug("Creating a new application with label '{}' and persistence set to '{}' ", label, flags.isPersistent()));
    }


    public Mono<Subscription> getSubscription(String keyIdentifier, Authentication authentication) {

        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subscriptionIdentifier = SparqlBuilder.var("b");

        Variable subLabel = SparqlBuilder.var("f");
        Variable subPersistent = SparqlBuilder.var("g");
        Variable subPublic = SparqlBuilder.var("h");

        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(SubscriptionTerms.HAS_KEY, keyIdentifier)
                        .andHas(SubscriptionTerms.HAS_LABEL, keyName)
                        .andHas(SubscriptionTerms.HAS_ISSUE_DATE, keyDate)
                        .andHas(SubscriptionTerms.IS_ACTIVE, keyActive)
                        .andHas(SubscriptionTerms.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(ApplicationTerms.HAS_KEY, subscriptionIdentifier)
                                .andHas(ApplicationTerms.IS_PERSISTENT, subPersistent)
                                .andHas(ApplicationTerms.IS_PUBLIC, subPublic)
                                .andHas(ApplicationTerms.HAS_LABEL, subLabel)
                        )
                );
        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .collectList()
                .flatMap(result -> {
                    List<BindingSet> bindingSets = result.stream().toList();
                    Assert.isTrue(bindingSets.size() == 1, "Found multiple key definitions for id " + keyIdentifier);
                    return Mono.just(bindingSets.get(0));
                })
                .map(BindingsAccessor::new)
                .map(ba ->

                        new Subscription(
                                ba.asIRI(nodeKey),
                                ba.asString(keyName),
                                keyIdentifier,
                                ba.asBoolean(keyActive),
                                ba.asString(keyDate),
                                new Application(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(subLabel),
                                        ba.asString(subscriptionIdentifier),
                                        new ApplicationFlags(
                                                ba.asBoolean(subPersistent),
                                                ba.asBoolean(subPublic)
                                        )
                                )
                        )
                )
                .switchIfEmpty(Mono.error(new UnknownApiKey(keyIdentifier)))
                .filter(Subscription::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(keyIdentifier)))
                .doOnSubscribe(subs -> log.debug("Requesting application details for application key '{}'", keyIdentifier));


    }

    private IRI asIRI(BindingSet bindings, Variable var) {
        return (IRI) bindings.getValue(var.getVarName());
    }

    public Flux<Subscription> getSubscriptionsForApplication(String applicationIdentifier, Authentication authentication) {

        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyIdentifier = SparqlBuilder.var("b");
        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subLabel = SparqlBuilder.var("f");
        Variable subPersistent = SparqlBuilder.var("g");
        Variable subPublic = SparqlBuilder.var("h");



        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(SubscriptionTerms.HAS_KEY, keyIdentifier)
                        .andHas(SubscriptionTerms.HAS_LABEL, keyName)
                        .andHas(SubscriptionTerms.HAS_ISSUE_DATE, keyDate)
                        .andHas(SubscriptionTerms.IS_ACTIVE, keyActive)
                        .andHas(SubscriptionTerms.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(ApplicationTerms.HAS_KEY, applicationIdentifier)
                                .andHas(ApplicationTerms.IS_PERSISTENT, subPersistent)
                                .andHas(ApplicationTerms.IS_PUBLIC, subPublic)
                                .andHas(ApplicationTerms.HAS_LABEL, subLabel)
                        )

                );
        String qs = q.getQueryString();

        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Subscription(
                                ba.asIRI(nodeKey),
                                ba.asString(keyName),
                                ba.asString(keyIdentifier),
                                ba.asBoolean(keyActive),
                                ba.asString(keyDate),
                                new Application(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(subLabel),
                                        applicationIdentifier,
                                        new ApplicationFlags(
                                                ba.asBoolean(subPersistent),
                                                ba.asBoolean(subPublic)
                                        )
                                )
                        )
                )
                .doOnSubscribe(sub -> log.debug("Requesting all API Keys for application with key '{}'", applicationIdentifier));
    }


    public Flux<Application> listApplications(Authentication authentication) {


        Variable node = SparqlBuilder.var("n");
        Variable key = SparqlBuilder.var("a");
        Variable label = SparqlBuilder.var("b");
        Variable isPersistent = SparqlBuilder.var("c");
        Variable isPublic = SparqlBuilder.var("d");

        SelectQuery q = Queries.SELECT()
                .where(node.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, key)
                        .andHas(ApplicationTerms.HAS_LABEL, label)
                        .andHas(ApplicationTerms.IS_PERSISTENT, isPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, isPublic)
                )
                .limit(100);

        return this.applicationsStore.query(q, authentication, Authorities.SYSTEM)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(label),
                                ba.asString(key),
                                new ApplicationFlags(
                                        ba.asBoolean(isPersistent),
                                        ba.asBoolean(isPublic)
                                )

                        )
                )
                .doOnSubscribe(sub -> log.debug("Requesting all applications"));

    }

    public Mono<Void> revokeToken(String subscriptionId, String name, Authentication authentication) {
        log.debug("(Service) Revoking api key for application '{}'", subscriptionId);

        return Mono.error(new UnsupportedOperationException());
    }

    public Mono<Subscription> createSubscription(String applicationIdentifier, String name, Authentication authentication) {


        Variable node = SparqlBuilder.var("node");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable subLabel = SparqlBuilder.var("g");
        Variable subPublic = SparqlBuilder.var("p");
        SelectQuery q = Queries.SELECT()
                .where(node.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, applicationIdentifier)
                        .andHas(ApplicationTerms.HAS_LABEL, subLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, subPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, subPublic)
                )

                .limit(2);


        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .collectList()
                .flatMap(this::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(subLabel),
                                applicationIdentifier,
                                new ApplicationFlags(
                                        ba.asBoolean(subPersistent),
                                        ba.asBoolean(subPublic)
                                )
                        )
                )
                .map(application ->
                        new Subscription(
                                new GeneratedIdentifier(Local.Subscriptions.NAMESPACE),
                                name,
                                GeneratedIdentifier.generateRandomKey(16),
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
                    modelBuilder.add(SubscriptionTerms.OF_SUBSCRIPTION, apiKey.application().key());
                    modelBuilder.add(apiKey.application().iri(), ApplicationTerms.HAS_API_KEY, apiKey.iri());

                    return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.APPLICATION).then(Mono.just(apiKey));
                })
                .doOnSuccess(token -> {
                    this.eventPublisher.publishEvent(new TokenCreatedEvent(token));
                })
                .doOnSubscribe(subs -> log.debug("Generating new api key for subscriptions '{}'", applicationIdentifier));
    }

    private Mono<BindingSet> getUniqueBindingSet(List<BindingSet> result) {
        if (result.isEmpty()) return Mono.empty();

        if (result.size() > 1) {
            log.error("Found multiple results when expected exactly one");
            return Mono.error(new DuplicateRecordsException());
        }

        return Mono.just(result.get(0));

    }


    public Mono<Application> getApplicationByLabel(String applicationLabel, Authentication authentication) {
        Variable node = SparqlBuilder.var("node");
        Variable keyIdentifier = SparqlBuilder.var("b");
        Variable subLabel = SparqlBuilder.var("f");
        Variable subPersistent = SparqlBuilder.var("g");
        Variable subPublic = SparqlBuilder.var("h");


        SelectQuery q = Queries.SELECT()
                .where(node.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, keyIdentifier)
                        .andHas(ApplicationTerms.HAS_LABEL, applicationLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, subPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, subPublic)
                )

                .limit(2);


        return this.applicationsStore.query(q, authentication, Authorities.READER)
                .collectList()
                .flatMap(this::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(subLabel),
                                ba.asString(keyIdentifier),
                                new ApplicationFlags(
                                        ba.asBoolean(subPersistent),
                                        ba.asBoolean(subPublic)
                                )
                        )
                );
    }
}
