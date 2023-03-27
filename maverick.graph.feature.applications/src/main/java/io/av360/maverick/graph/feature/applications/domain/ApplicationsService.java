package io.av360.maverick.graph.feature.applications.domain;

import io.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import io.av360.maverick.graph.api.security.errors.UnknownApiKey;
import io.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import io.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import io.av360.maverick.graph.feature.applications.domain.events.TokenCreatedEvent;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import io.av360.maverick.graph.feature.applications.domain.model.Subscription;
import io.av360.maverick.graph.feature.applications.domain.vocab.ApplicationTerms;
import io.av360.maverick.graph.feature.applications.domain.vocab.SubscriptionTerms;
import io.av360.maverick.graph.feature.applications.store.ApplicationsStore;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static io.av360.maverick.graph.model.util.StreamsLogger.debug;

/**
 * Applications separate tenants. Each application has its own separate stores.
 * An application has a set of unique API Keys. The api key identifies the application.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class ApplicationsService {

    private static final Variable varAppIri = SparqlBuilder.var("appNode");
    private static final Variable varAppKey = SparqlBuilder.var("appId");
    private static final Variable varAppLabel = SparqlBuilder.var("appLabel");
    private static final Variable varAppFlagPersistent = SparqlBuilder.var("appPersistent");
    private static final Variable varAppFlagPublic = SparqlBuilder.var("appPublic");

    private static final Variable varSubIri = SparqlBuilder.var("subNode");
    private static final Variable varSubKey = SparqlBuilder.var("subId");
    private static final Variable varSubIssued = SparqlBuilder.var("subIssued");
    private static final Variable varSubActive = SparqlBuilder.var("subActive");
    private static final Variable varSubLabel = SparqlBuilder.var("subLabel");


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

        // generate application iri
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
                .doOnSubscribe(debug(log, "Creating a new application with label '{}' and persistence set to '{}' ", label, flags.isPersistent()));
    }


    public Mono<Subscription> getSubscription(String subscriptionIdentifier, Authentication authentication) {



        SelectQuery q = Queries.SELECT()
                .where(varSubIri.has(SubscriptionTerms.HAS_KEY, subscriptionIdentifier)
                        .andHas(SubscriptionTerms.HAS_LABEL, varSubLabel)
                        .andHas(SubscriptionTerms.HAS_ISSUE_DATE, varSubIssued)
                        .andHas(SubscriptionTerms.IS_ACTIVE, varSubActive)
                        .andHas(SubscriptionTerms.FOR_APPLICATION, varAppKey)
                        .and(varAppIri.has(ApplicationTerms.HAS_KEY, varAppKey)
                                .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                                .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                                .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                        )
                );
        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .singleOrEmpty()
                .map(BindingsAccessor::new)
                .map(this::buildSubscriptionFromBindings)
                .switchIfEmpty(Mono.error(new UnknownApiKey(subscriptionIdentifier)))
                .filter(Subscription::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(subscriptionIdentifier)))
                .doOnSubscribe(subs -> log.debug("Requesting application details for application key '{}'", subscriptionIdentifier));


    }

    private IRI asIRI(BindingSet bindings, Variable var) {
        return (IRI) bindings.getValue(var.getVarName());
    }

    public Flux<Subscription> listSubscriptionsForApplication(String applicationIdentifier, Authentication authentication) {

        return this.getApplication(applicationIdentifier, authentication)
                .flatMapMany(app -> {
                    SelectQuery q = Queries.SELECT()
                            .where(varSubIri.has(SubscriptionTerms.HAS_KEY, varSubKey)
                                    .andHas(SubscriptionTerms.HAS_LABEL, varSubLabel)
                                    .andHas(SubscriptionTerms.HAS_ISSUE_DATE, varSubIssued)
                                    .andHas(SubscriptionTerms.IS_ACTIVE, varSubActive)
                                    .andHas(SubscriptionTerms.FOR_APPLICATION, app.key())
                            );

                    return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                            .map(BindingsAccessor::new)
                            .map(ba -> this.buildSubscriptionFromBindings(ba, app));
                })
                .doOnSubscribe(debug(log, "Requesting all API Keys for application with key '{}'", applicationIdentifier));
    }


    public Flux<Application> listApplications(Authentication authentication) {


        SelectQuery q = Queries.SELECT()
                .where(varAppIri.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, varAppKey)
                        .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                )
                .limit(100);

        return this.applicationsStore.query(q, authentication, Authorities.SYSTEM)
                .map(BindingsAccessor::new)
                .map(this::buildApplicationFromBindings)
                .doOnSubscribe(debug(log, "Loading all applications from repository."));

    }

    public Mono<Void> revokeToken(String subscriptionId, String name, Authentication authentication) {
        log.debug(" Revoking api key for application '{}'", subscriptionId);

        return Mono.error(new UnsupportedOperationException());
    }

    public Mono<Subscription> createSubscription(String applicationIdentifier, String subscriptionLabel, Authentication authentication) {

        return this.getApplication(applicationIdentifier, authentication)
                .map(application ->
                        new Subscription(
                                new GeneratedIdentifier(Local.Subscriptions.NAMESPACE),
                                subscriptionLabel,
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
                    modelBuilder.add(SubscriptionTerms.FOR_APPLICATION, apiKey.application().key());
                    modelBuilder.add(apiKey.application().iri(), ApplicationTerms.HAS_API_KEY, apiKey.iri());

                    return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.APPLICATION).then(Mono.just(apiKey));
                })
                .doOnSuccess(token -> {
                    this.eventPublisher.publishEvent(new TokenCreatedEvent(token));
                })
                .doOnSubscribe(debug(log, "Generating new subscription key for application '{}'", applicationIdentifier));
    }


    public Mono<Application> getApplication(String applicationKey, Authentication authentication) {

        SelectQuery q = Queries.SELECT().distinct()
                .where(varAppIri.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, varAppKey)
                        .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                );

        return this.applicationsStore.query(q, authentication, Authorities.READER)
                .singleOrEmpty()
                .map(BindingsAccessor::new)
                .map(this::buildApplicationFromBindings)
                .doOnSubscribe(debug(log, "Requesting application with identifier '{}'", applicationKey));
    }




    public Mono<Application> getApplicationByLabel(String applicationLabel, Authentication authentication) {


        SelectQuery q = Queries.SELECT()
                .where(varAppIri.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, varAppKey)
                        .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                );


        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationLabel)))
                .doOnNext(bindings -> {
                    log.trace("Found application: {}", bindings.getValue(varAppLabel.getVarName()));
                })
                .collectList()
                .flatMap(BindingsAccessor::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(this::buildApplicationFromBindings)
                .doOnSubscribe(sub -> log.debug("Requesting application with label '{}'", applicationLabel));
    }

    private Application buildApplicationFromBindings(BindingsAccessor ba) {
        return new Application(
                ba.asIRI(varAppIri),
                ba.asString(varAppLabel),
                ba.asString(varAppKey),
                new ApplicationFlags(
                        ba.asBoolean(varAppFlagPersistent),
                        ba.asBoolean(varAppFlagPublic)
                )
        );
    }

    private Subscription buildSubscriptionFromBindings(BindingsAccessor ba, Application app) {

        return new Subscription(
                ba.asIRI(varSubIri),
                ba.asString(varSubLabel),
                ba.asString(varSubKey),
                ba.asBoolean(varSubActive),
                ba.asString(varSubIssued),
                app
                );
    }

    private Subscription buildSubscriptionFromBindings(BindingsAccessor ba) {

        return new Subscription(
                ba.asIRI(varSubIri),
                ba.asString(varSubLabel),
                ba.asString(varSubKey),
                ba.asBoolean(varSubActive),
                ba.asString(varSubIssued),
                this.buildApplicationFromBindings(ba)
        );
    }
}
