package org.av360.maverick.graph.feature.applications.domain;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import org.av360.maverick.graph.api.security.errors.UnknownApiKey;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;

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

    private final IdentifierFactory identifierFactory;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher, IdentifierFactory identifierFactory) {
        this.applicationsStore = applicationsStore;
        this.eventPublisher = eventPublisher;
        this.identifierFactory = identifierFactory;
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



        LocalIdentifier subject = identifierFactory.createRandomIdentifier(Local.Subscriptions.NAMESPACE);

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
                .doOnSubscribe(StreamsLogger.debug(log, "Requesting all API Keys for application with key '{}'", applicationIdentifier));
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
                .doOnSubscribe(StreamsLogger.debug(log, "Loading all applications from repository."));

    }

    public Mono<Void> revokeToken(String subscriptionId, String name, Authentication authentication) {
        log.debug(" Revoking api key for application '{}'", subscriptionId);

        return Mono.error(new UnsupportedOperationException());
    }

    public Mono<Subscription> createSubscription(String applicationKey, String subscriptionLabel, Authentication authentication) {

        return this.getApplication(applicationKey, authentication)
                .map(application ->
                        new Subscription(
                                identifierFactory.createRandomIdentifier(Local.Subscriptions.NAMESPACE),
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

        SelectQuery q = Queries.SELECT().distinct()
                .where(varAppIri.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, applicationKey)
                        .andHas(ApplicationTerms.HAS_LABEL, varAppLabel)
                        .andHas(ApplicationTerms.IS_PERSISTENT, varAppFlagPersistent)
                        .andHas(ApplicationTerms.IS_PUBLIC, varAppFlagPublic)
                );
        String queryString = q.getQueryString();
        return this.applicationsStore.query(q, authentication, Authorities.READER)
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationKey)))
                .map(BindingsAccessor::new)
                .map(ba -> new Application(
                        ba.asIRI(varAppIri),
                        ba.asString(varAppLabel),
                        applicationKey,
                        new ApplicationFlags(
                                ba.asBoolean(varAppFlagPersistent),
                                ba.asBoolean(varAppFlagPublic),
                                null,
                                null,
                                null
                        )
                ))
                .doOnSubscribe(StreamsLogger.debug(log, "Loading application with identifier '{}'", applicationKey));
    }




    public Mono<Application> getApplicationByLabel(String applicationLabel, Authentication authentication) {
        if(applicationLabel.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) return Mono.empty();

        SelectQuery q = Queries.SELECT()
                .where(varAppIri.isA(ApplicationTerms.TYPE)
                        .andHas(ApplicationTerms.HAS_KEY, varAppKey)
                        .andHas(ApplicationTerms.HAS_LABEL, applicationLabel)
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
                .map(ba -> new Application(
                        ba.asIRI(varAppIri),
                        applicationLabel,
                        ba.asString(varAppKey),
                        new ApplicationFlags(
                                ba.asBoolean(varAppFlagPersistent),
                                ba.asBoolean(varAppFlagPublic),
                                null,
                                null,
                                null

                        )
                ))
                .doOnSubscribe(sub -> log.debug("Requesting application with label '{}'", applicationLabel));
    }

    private Application buildApplicationFromBindings(BindingsAccessor ba) {
        return new Application(
                ba.asIRI(varAppIri),
                ba.asString(varAppLabel),
                ba.asString(varAppKey),
                new ApplicationFlags(
                        ba.asBoolean(varAppFlagPersistent),
                        ba.asBoolean(varAppFlagPublic),
                        null,
                        null,
                        null
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

//    public Mono<Void> setApplicationConfig(String applicationIdentifier, String s3Host, String s3Bucket, String exportFrequency, Authentication authentication) {
//
//        Variable node = SparqlBuilder.var("n");
//        Variable s3HostVar = SparqlBuilder.var("d");
//        Variable s3BucketVar = SparqlBuilder.var("e");
//        Variable exportFrequencyVar = SparqlBuilder.var("f");
//
//        ModifyQuery q = Queries.MODIFY()
//                .where(node.isA(Application.TYPE)
//                        .andHas(Application.HAS_KEY, applicationIdentifier)
//                )
//                .delete(node.has(Application.HAS_S3_HOST, s3HostVar))
//                .delete(node.has(Application.HAS_S3_BUCKET_ID, s3BucketVar))
//                .delete(node.has(Application.HAS_EXPORT_FREQUENCY, exportFrequencyVar))
//                .insert(node.has(Application.HAS_S3_HOST, s3Host))
//                .insert(node.has(Application.HAS_S3_BUCKET_ID, s3Bucket))
//                .insert(node.has(Application.HAS_EXPORT_FREQUENCY, exportFrequency));
//
//        return this.applicationsStore.modify(q, authentication, Authorities.APPLICATION)
//                .then()
//                .doOnSubscribe(sub -> log.debug("Setting application config for application with key '{}'", applicationIdentifier));
//
//    }
//
//    public Mono<HashMap<String, String>> getApplicationConfig(String applicationIdentifier, Authentication authentication) {
//
//        Variable node = SparqlBuilder.var("n");
//        Variable label = SparqlBuilder.var("b");
//        Variable persistent = SparqlBuilder.var("c");
//        Variable s3Host = SparqlBuilder.var("d");
//        Variable s3Bucket = SparqlBuilder.var("e");
//        Variable exportFrequency = SparqlBuilder.var("f");
//
//        SelectQuery q = Queries.SELECT()
//                .where(node.isA(Application.TYPE)
//                        .andHas(Application.HAS_KEY, applicationIdentifier)
//                        .andHas(Application.HAS_LABEL, label)
//                        .andHas(Application.IS_PERSISTENT, persistent)
//                        .andHas(Application.HAS_S3_HOST, s3Host)
//                        .andHas(Application.HAS_S3_BUCKET_ID, s3Bucket)
//                        .andHas(Application.HAS_EXPORT_FREQUENCY, exportFrequency)
//                )
//                .limit(1);
//
//        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
//                .collectList()
//                .flatMap(this::getUniqueBindingSet)
//                .map(BindingsAccessor::new)
//                .map(ba -> {
//                    HashMap<String, String> map = new HashMap<>();
//                    map.put("label", ba.asString(label));
//                    map.put("persistent", ba.asString(persistent));
//                    map.put("s3Host", ba.asString(s3Host));
//                    map.put("s3Bucket", ba.asString(s3Bucket));
//                    map.put("exportFrequency", ba.asString(exportFrequency));
//                    return map;
//                })
//                .doOnSubscribe(sub -> log.debug("Getting application config for application with key '{}'", applicationIdentifier));
//    }
//
//    public Mono<String> exportApplication(String applicationIdentifier, Authentication authentication) {
//
//        Variable node = SparqlBuilder.var("n");
//        Variable label = SparqlBuilder.var("b");
//        Variable persistent = SparqlBuilder.var("c");
//        Variable s3Host = SparqlBuilder.var("d");
//        Variable s3Bucket = SparqlBuilder.var("e");
//
//        SelectQuery q = Queries.SELECT()
//                .where(node.isA(Application.TYPE)
//                        .andHas(Application.HAS_KEY, applicationIdentifier)
//                        .andHas(Application.HAS_LABEL, label)
//                        .andHas(Application.IS_PERSISTENT, persistent)
//                        .andHas(Application.HAS_S3_HOST, s3Host)
//                        .andHas(Application.HAS_S3_BUCKET_ID, s3Bucket)
//                )
//                .limit(1);
//
//
//        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
//                .collectList()
//                .flatMap(this::getUniqueBindingSet)
//                .map(BindingsAccessor::new)
//                .map(ba -> {
//                    //TODO: fix this
//                    this.queryServices.queryGraph()
//
//                    try {
//                        S3Client s3 = S3Client.builder()
//                                .endpointOverride(URI.create(ba.asString(s3Host)))
//                                .build();
//                        PutObjectRequest objectRequest = PutObjectRequest.builder()
//                                .bucket(ba.asString(s3Bucket))
//                                .key(exportIdentifier)
//                                .build();
//                        s3.putObject(objectRequest, RequestBody.fromString(ba.asString(node)));
//
//                    } catch (S3Exception e) {
//                        log.error(e.awsErrorDetails().errorMessage());
//                    }
//
//                    return exportIdentifier;
//                })
//                .doOnSubscribe(sub -> log.debug("Exporting application with key '{}'", applicationIdentifier));
//
//    }
//
//    public Mono<HashMap<String, String>> getExport(String applicationIdentifier, String exportId, Authentication authentication) {
//        log.debug("(Service) Getting export '{}' for application '{}'", exportId, applicationIdentifier);
//
//        Variable node = SparqlBuilder.var("n");
//        Variable label = SparqlBuilder.var("b");
//        Variable persistent = SparqlBuilder.var("c");
//        Variable s3Host = SparqlBuilder.var("d");
//        Variable s3Bucket = SparqlBuilder.var("e");
//
//        SelectQuery q = Queries.SELECT()
//                .where(node.isA(Application.TYPE)
//                        .andHas(Application.HAS_KEY, applicationIdentifier)
//                        .andHas(Application.HAS_LABEL, label)
//                        .andHas(Application.IS_PERSISTENT, persistent)
//                        .andHas(Application.HAS_S3_HOST, s3Host)
//                        .andHas(Application.HAS_S3_BUCKET_ID, s3Bucket)
//                )
//                .limit(1);
//
//        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
//                .collectList()
//                .flatMap(this::getUniqueBindingSet)
//                .map(BindingsAccessor::new)
//                .mapNotNull(ba -> {
//
//                    try {
//                        S3Client s3 = S3Client.builder()
//                                .endpointOverride(URI.create(ba.asString(s3Host)))
//                                .build();
//                        GetObjectRequest objectRequest = GetObjectRequest.builder()
//                                .bucket(ba.asString(s3Bucket))
//                                .key(exportId)
//                                .build();
//                        ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
//
//                        return objectBytes.asUtf8String();
//                    } catch (S3Exception e) {
//                        log.error(e.awsErrorDetails().errorMessage());
//                    }
//
//                    return null;
//                })
//                .map(responseBody -> {
//                    HashMap<String, String> response = new HashMap<>();
//                    response.put("application", applicationIdentifier);
//                    response.put("exportId", exportId);
//                    response.put("export", responseBody);
//                    return response;
//                })
//                .doOnSubscribe(sub -> log.debug("Getting export '{}' for application '{}'", exportId, applicationIdentifier));
//    }
}
