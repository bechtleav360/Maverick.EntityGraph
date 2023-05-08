package org.av360.maverick.graph.feature.applications.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import org.av360.maverick.graph.api.security.errors.UnknownApiKey;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.TokenCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import org.av360.maverick.graph.feature.applications.domain.model.Subscription;
import org.av360.maverick.graph.feature.applications.domain.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.domain.vocab.SubscriptionTerms;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.identifier.RandomIdentifier;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ModifyQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Applications separate tenants. Each application has its own separate stores.
 * An application has a set of unique API Keys. The api key identifies the application.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class ApplicationsService implements ApplicationListener<ApplicationUpdatedEvent> {

    private static final Variable varAppIri = SparqlBuilder.var("appNode");
    private static final Variable varAppKey = SparqlBuilder.var("appId");
    private static final Variable varAppLabel = SparqlBuilder.var("appLabel");
    private static final Variable varAppFlagPersistent = SparqlBuilder.var("appPersistent");
    private static final Variable varAppFlagPublic = SparqlBuilder.var("appPublic");
    private static final Variable varAppFlagS3Host = SparqlBuilder.var("appS3Host");
    private static final Variable varAppFlagS3BucketId = SparqlBuilder.var("appS3BucketId");
    private static final Variable varAppFlagExportFrequency = SparqlBuilder.var("appExportFrequency");

    private static final Variable varSubIri = SparqlBuilder.var("subNode");
    private static final Variable varSubKey = SparqlBuilder.var("subId");
    private static final Variable varSubIssued = SparqlBuilder.var("subIssued");
    private static final Variable varSubActive = SparqlBuilder.var("subActive");
    private static final Variable varSubLabel = SparqlBuilder.var("subLabel");

    private final Cache<String, Application> cache;

    private final ApplicationsStore applicationsStore;

    private final ApplicationEventPublisher eventPublisher;

    private final IdentifierFactory identifierFactory;
    private MeterRegistry meterRegistry;
    private Gauge cacheGauge;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher, IdentifierFactory identifierFactory) {
        this.applicationsStore = applicationsStore;
        this.eventPublisher = eventPublisher;
        this.identifierFactory = identifierFactory;
        this.cache = Caffeine.newBuilder().recordStats().expireAfterAccess(60, TimeUnit.MINUTES).build();
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



        LocalIdentifier subject = identifierFactory.createRandomIdentifier(Local.Applications.NAMESPACE);

        Application application = new Application(
                subject,
                label,
                subject.getLocalName(),
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
        modelBuilder.add(ApplicationTerms.HAS_S3HOST, flags.s3Host());
        modelBuilder.add(ApplicationTerms.HAS_S3BUCKETID, flags.s3BucketId());
        modelBuilder.add(ApplicationTerms.HAS_EXPORT_FREQUENCY, flags.exportFrequency());


        Mono<Application> applicationMono = this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.SYSTEM)
                .then(Mono.just(application))
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationCreatedEvent(app));
                    log.debug("Created application with key '{}' and label '{}'", app.key(), app.label());
                })

                .doOnSubscribe(StreamsLogger.debug(log, "Creating a new application with label '{}' and persistence set to '{}' ", label, flags.isPersistent()));

        return this.getApplicationByLabel(label, authentication)
                .onErrorResume(throwable -> throwable instanceof InvalidApplication, throwable -> applicationMono);


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


    public Flux<Subscription> listSubscriptionsForApplication(String applicationKey, Authentication authentication) {

        return this.getApplication(applicationKey, authentication)
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
                .doOnSubscribe(StreamsLogger.debug(log, "Requesting all API Keys for application with key '{}'", applicationKey));
    }


    public Flux<Application> listApplications(Authentication authentication) {


        ConcurrentMap<String, Application> allApplications = this.cache.asMap();
        if(allApplications.isEmpty()) {
            SelectQuery q = Queries.SELECT()
                    .where(varAppIri.isA(ApplicationTerms.TYPE)
                            .andHas(ApplicationTerms.HAS_KEY, varAppKey)
                            .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                            .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                            .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                            .andHas(ApplicationTerms.HAS_S3HOST, varAppFlagS3Host)
                            .andHas(ApplicationTerms.HAS_S3BUCKETID, varAppFlagS3BucketId)
                            .andHas(ApplicationTerms.HAS_EXPORT_FREQUENCY, varAppFlagExportFrequency)
                    )
                    .limit(100);

            return this.applicationsStore.query(q, authentication, Authorities.GUEST)
                    .map(BindingsAccessor::new)
                    .map(this::buildApplicationFromBindings)
                    .doOnNext(application -> this.cache.put(application.key(), application))
                    .doOnSubscribe(StreamsLogger.debug(log, "Loading all applications from repository."));
        } else {
            return Flux.fromIterable(allApplications.values());
        }
    }

    public Mono<Void> revokeToken(String subscriptionId, String name, Authentication authentication) {
        log.debug(" Revoking api key for application '{}'", subscriptionId);

        return Mono.error(new UnsupportedOperationException());
    }

    public Mono<Subscription> createSubscription(String applicationKey, String subscriptionLabel, Authentication authentication) {

        return this.getApplication(applicationKey, authentication)
                .map(application ->
                        new Subscription(
                                identifierFactory.createRandomIdentifier(Local.Applications.NAMESPACE),
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

                    return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.APPLICATION).then(Mono.just(apiKey));
                })
                .doOnSuccess(token -> {
                    this.eventPublisher.publishEvent(new TokenCreatedEvent(token));
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Generating new subscription key for application '{}'", applicationKey));
    }


    public Mono<Application> getApplication(String applicationKey, Authentication authentication) {

        Application cached = this.cache.getIfPresent(applicationKey);
        if(Objects.isNull(cached)) {
            // rebuild cache
            return this.listApplications(authentication)
                    .filter(application -> application.key().equalsIgnoreCase(applicationKey))
                    .switchIfEmpty(Mono.error(new InvalidApplication(applicationKey)))
                    .single();
        } else {
            return Mono.just(cached);
        }
    }




    public Mono<Application> getApplicationByLabel(String applicationLabel, Authentication authentication) {
        if(applicationLabel.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) return Mono.empty();

        return this.listApplications(authentication)
                .filter(application -> application.label().equalsIgnoreCase(applicationLabel))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationLabel)))
                .single();
    }

    private Application buildApplicationFromBindings(BindingsAccessor ba) {
        return new Application(
                ba.asIRI(varAppIri),
                ba.asString(varAppLabel),
                ba.asString(varAppKey),
                new ApplicationFlags(
                        ba.asBoolean(varAppFlagPersistent),
                        ba.asBoolean(varAppFlagPublic),
                        ba.asString(varAppFlagS3Host),
                        ba.asString(varAppFlagS3BucketId),
                        ba.asString(varAppFlagExportFrequency)
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



    @Override
    public void onApplicationEvent(ApplicationUpdatedEvent event) {
        this.cache.invalidateAll();
    }


    @Autowired
    private void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
}
