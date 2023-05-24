package org.av360.maverick.graph.feature.applications.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import org.av360.maverick.graph.feature.applications.domain.model.QueryVariables;
import org.av360.maverick.graph.feature.applications.domain.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Applications separate tenants. Each application has its own separate stores.
 * An application has a set of unique API Keys. The api key identifies the application.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class ApplicationsService implements ApplicationListener<ApplicationUpdatedEvent> {


    private final Cache<String, Application> cache;

    private final ApplicationsStore applicationsStore;

    private final ApplicationEventPublisher eventPublisher;

    private final IdentifierFactory identifierFactory;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher, IdentifierFactory identifierFactory) {
        this.applicationsStore = applicationsStore;
        this.eventPublisher = eventPublisher;
        this.identifierFactory = identifierFactory;
        // this.cache = Caffeine.newBuilder().recordStats().expireAfterAccess(60, TimeUnit.MINUTES).build();
        this.cache = Caffeine.newBuilder().recordStats().build();
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




    public Flux<Application> listApplications(Authentication authentication) {

        if(this.cache.asMap().isEmpty()) {

            SelectQuery q = Queries.SELECT()
                    .where(QueryVariables.varAppIri.isA(ApplicationTerms.TYPE)
                            .andHas(ApplicationTerms.HAS_KEY, QueryVariables.varAppKey)
                            .andHas(ApplicationTerms.HAS_LABEL, QueryVariables.varAppLabel)
                            .andHas(ApplicationTerms.IS_PERSISTENT, QueryVariables.varAppFlagPersistent)
                            .andHas(ApplicationTerms.IS_PUBLIC, QueryVariables.varAppFlagPublic)
                            .andHas(ApplicationTerms.HAS_S3HOST, QueryVariables.varAppFlagS3Host)
                            .andHas(ApplicationTerms.HAS_S3BUCKETID, QueryVariables.varAppFlagS3BucketId)
                            .andHas(ApplicationTerms.HAS_EXPORT_FREQUENCY, QueryVariables.varAppFlagExportFrequency)
                    )
                    .limit(100);

            return this.applicationsStore.query(q, authentication, Authorities.READER)
                    .map(BindingsAccessor::new)
                    .map(QueryVariables::buildApplicationFromBindings)
                    .doOnNext(application -> this.cache.put(application.key(), application))
                    .doOnSubscribe(StreamsLogger.debug(log, "Loading all applications from repository."));
        } else {
            return Flux.fromIterable(this.cache.asMap().values());
        }
    }

    public Mono<Application> getApplicationByLabel(String applicationLabel, Authentication authentication) {
        if(applicationLabel.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) return Mono.empty();

        return this.listApplications(authentication)
                .filter(application -> application.label().equalsIgnoreCase(applicationLabel))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationLabel)))
                .single();
    }





    @Override
    public void onApplicationEvent(ApplicationUpdatedEvent event) {
        this.cache.invalidateAll();
        this.cache.cleanUp();
    }



}
