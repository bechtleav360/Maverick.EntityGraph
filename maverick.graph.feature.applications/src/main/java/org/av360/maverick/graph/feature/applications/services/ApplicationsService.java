package org.av360.maverick.graph.feature.applications.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.security.SubscriptionToken;
import org.av360.maverick.graph.feature.applications.services.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.services.events.GraphApplicationEvent;
import org.av360.maverick.graph.feature.applications.services.model.Application;
import org.av360.maverick.graph.feature.applications.services.model.ApplicationFlags;
import org.av360.maverick.graph.feature.applications.services.model.ConfigurationItem;
import org.av360.maverick.graph.feature.applications.services.model.QueryVariables;
import org.av360.maverick.graph.feature.applications.services.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Applications separate tenants. Each node has its own separate stores.
 * An node has a set of unique API Keys. The api key identifies the node.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class ApplicationsService implements ApplicationListener<GraphApplicationEvent> {


    private final Cache<String, Application> cache;

    private boolean caching_enabled = true;

    private final ApplicationsStore applicationsStore;
    private final ApplicationEventPublisher eventPublisher;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher) {
        this.applicationsStore = applicationsStore;
        this.eventPublisher = eventPublisher;
        // this.cache = Caffeine.newBuilder().recordStats().expireAfterAccess(60, TimeUnit.MINUTES).build();
        this.cache = Caffeine.newBuilder().recordStats().build();
    }




    /**
     * Creates a new node.
     *
     * @param label          Label for the node
     * @param flags          Flags for this node
     * @param authentication Current authentication information
     * @return New node as mono
     */
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Application> createApplication(String label, ApplicationFlags flags, Map<String, Serializable> configuration, SessionContext ctx) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthenticationOrThrow().isAuthenticated());
            Validate.isTrue(Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities()));

            // FIXME: authorize in service
        } catch (Exception e) { return Mono.error(e); }


        LocalIdentifier subject = IdentifierServices.createRandomIdentifier(Local.Applications.NAMESPACE);

        Application application = new Application(
                subject,
                label,
                subject.getLocalName(),
                flags,
                configuration
        );

        // store node
        ModelBuilder modelBuilder = new ModelBuilder();

        modelBuilder.subject(application.iri());
        modelBuilder.add(RDF.TYPE, ApplicationTerms.TYPE);
        modelBuilder.add(ApplicationTerms.HAS_KEY, application.key());
        modelBuilder.add(ApplicationTerms.HAS_LABEL, application.label());
        modelBuilder.add(ApplicationTerms.IS_PERSISTENT, flags.isPersistent());
        modelBuilder.add(ApplicationTerms.IS_PUBLIC, flags.isPublic());

        application.configuration().forEach((key, value) -> {
            this.buildConfigurationItem(key, value, application.iri(), modelBuilder);
        });

        ctx.updateEnvironment(env-> env.setRepositoryType(RepositoryType.APPLICATION));

        Mono<Application> applicationMono = this.applicationsStore.insertModel(modelBuilder.build(), ctx.getEnvironment())
                .then(Mono.just(application))
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationCreatedEvent(app));
                    log.debug("Created application with key '{}' and label '{}'", app.key(), app.label());
                })

                .doOnSubscribe(StreamsLogger.debug(log, "Creating a new application with label '{}' and persistence set to '{}' ", label, flags.isPersistent()));

        return this.getApplicationByLabel(label, ctx)
                .onErrorResume(throwable -> throwable instanceof InvalidApplication, throwable -> applicationMono);


    }
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    private ModelBuilder buildConfigurationItem(String configKey, Serializable configValue, IRI applicationIdentifier, @Nullable ModelBuilder builder) {
        if(Objects.isNull(builder)) builder = new ModelBuilder();

        LocalIdentifier configNode = IdentifierServices.createRandomIdentifier(Local.Applications.NAMESPACE);
        builder.add(configNode, RDF.TYPE, ApplicationTerms.CONFIGURATION_ITEM);
        builder.add(configNode, ApplicationTerms.CONFIG_KEY, configKey);
        builder.add(configNode, ApplicationTerms.CONFIG_VALUE, configValue.toString());
        builder.add(configNode, ApplicationTerms.CONFIG_FOR, applicationIdentifier);

        return builder;
    }
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Application> createConfigurationItem(Application application, String configKey, Serializable configValue, SessionContext ctx) {
        this.assertUpdatePrivilege(application, ctx);

        ModelBuilder m = this.buildConfigurationItem(configKey, configValue, application.iri(), null);

        return this.applicationsStore.insertModel(m.build(), ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.APPLICATION)).getEnvironment())
                .then(this.getApplication(application.key(), ctx))
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(app));
                    log.debug("Updated configuration key '{}' of node with label '{}'", app.key(), app.label());
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Updating configuration key '{}' for node with label '{}'", configKey, application.label()));
    }
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> deleteConfigurationItem(Application application, String configurationKey, SessionContext ctx) {
        this.assertUpdatePrivilege(application, ctx);

        SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                        .andHas(ApplicationTerms.CONFIG_KEY, configurationKey)
                        .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                        .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, application.iri()))
                );
        Flux<IRI> nodesToDelete = this.applicationsStore.query(listConfigurationItemsQuery, ctx.getEnvironment())
                .map(BindingsAccessor::new)
                .map(ba -> ba.findValue(QueryVariables.varNodeConfigurationItem))
                .filter(Optional::isPresent)
                .filter(opt -> opt.get().isIRI())
                .map(opt -> (IRI) opt.get());
        return nodesToDelete.flatMap(configNode -> this.applicationsStore.listStatements(configNode, null, null, ctx.getEnvironment()))
                .map(LinkedHashModel::new)
                .flatMap(model -> this.applicationsStore.deleteModel(model, ctx.getEnvironment()))
                .collectList()
                .then()
                .doOnSuccess(res -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(application));
                    log.debug("Removed configuration item with key '{}' from application with label '{}'", configurationKey, application.label());
                });
    }
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Flux<ConfigurationItem> listConfigurationItems(Application application, SessionContext ctx) {
        assertReadPrivilege(application, ctx);

        SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                        .andHas(ApplicationTerms.CONFIG_KEY, QueryVariables.varConfigKey)
                        .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                        .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, application.iri()))
                );
        return this.applicationsStore.query(listConfigurationItemsQuery, ctx.getEnvironment())
                .map(BindingsAccessor::new)
                .flatMap(QueryVariables::buildConfigurationItemFromBindings);

    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> delete(Application application, SessionContext context) {
        assertUpdatePrivilege(application, context);

        return this.listConfigurationItems(application, context)
                .flatMap(configurationItem -> this.deleteConfigurationItem(application, configurationItem.key(), context)).collectList()
                .doOnSuccess(suc -> log.trace("Deleted all configuration item statements for application with label '{}'", application.label()))
                .then( this.applicationsStore.listStatements(application.iri(), null, null, context.getEnvironment()))
                .flatMap(statements -> this.applicationsStore.deleteModel(statements, context.getEnvironment()))
                .doOnSuccess(suc -> log.trace("Deleted all application statements for application with label '{}'", application.label()))
                .then()
                .doOnSuccess(res -> {
                    this.eventPublisher.publishEvent(new ApplicationDeletedEvent(application.label()));
                    log.debug("Deleted application with label '{}'", application.label());
                });


    }
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Application> getApplication(String applicationKey, SessionContext ctx) {

        Application cached = this.caching_enabled ? this.cache.getIfPresent(applicationKey) : null;
        Mono<Application> response = null;
        if (Objects.isNull(cached)) {
            // rebuild cache
            response = this.listApplications(ctx)
                    .filter(application -> {
                        boolean res = application.key().equalsIgnoreCase(applicationKey);
                        return res;
                    })
                    .switchIfEmpty(Mono.error(new InvalidApplication(applicationKey)))
                    .single();
        } else {
            response =  Mono.just(cached);
        }

        return response.filter(application -> verifyReadingPrivilege(application, ctx));
    }


    private boolean verifyReadingPrivilege(Application requestedApplication, SessionContext ctx) {
        boolean isPublic = requestedApplication.flags().isPublic();
        boolean isSubscriber = (ctx.getAuthenticationOrThrow() instanceof SubscriptionToken subscriptionToken) && (subscriptionToken.getApplication().key().equalsIgnoreCase(requestedApplication.key()));
        boolean isAdmin = Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities());

        return isPublic || isSubscriber || isAdmin;
    }

    private void assertUpdatePrivilege(Application requestedApplication, SessionContext ctx) {
        if(! this.verifyUpdatePrivilege(requestedApplication, ctx)) throw new InsufficientPrivilegeException("Unknown application or insufficient privilege.");
    }
    private void assertReadPrivilege(Application requestedApplication, SessionContext ctx) {
        if(! this.verifyReadingPrivilege(requestedApplication, ctx)) throw new InsufficientPrivilegeException("Unknown application or insufficient privilege.");
    }

    private boolean verifyUpdatePrivilege(Application requestedApplication, SessionContext ctx) {
        boolean isApplicationAdmin = (ctx.getAuthenticationOrThrow() instanceof SubscriptionToken subscriptionToken)
                && (subscriptionToken.getApplication().key().equalsIgnoreCase(requestedApplication.key()))
                && Authorities.satisfies(Authorities.APPLICATION, ctx.getAuthenticationOrThrow().getAuthorities());
        boolean isAdmin = Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities());

        return isApplicationAdmin || isAdmin;
    }
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<Application> listApplications(SessionContext ctx) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthentication().get().isAuthenticated());
        } catch (Exception e) { return Flux.error(e); }


        ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.APPLICATION));

        if (!this.caching_enabled || this.cache.asMap().isEmpty()) {

            SelectQuery listApplicationsQuery = Queries.SELECT()
                    .where(QueryVariables.varNodeApplication.isA(ApplicationTerms.TYPE)
                            .andHas(ApplicationTerms.HAS_KEY, QueryVariables.varAppKey)
                            .andHas(ApplicationTerms.HAS_LABEL, QueryVariables.varAppLabel)
                            .andHas(ApplicationTerms.IS_PERSISTENT, QueryVariables.varAppFlagPersistent)
                            .andHas(ApplicationTerms.IS_PUBLIC, QueryVariables.varAppFlagPublic)
                    );
            SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                    .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                            .andHas(ApplicationTerms.CONFIG_KEY, QueryVariables.varConfigKey)
                            .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                            .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, QueryVariables.varNodeApplication).optional())
                    );

            return Mono.zip(
                            this.applicationsStore.query(listApplicationsQuery, ctx.getEnvironment())
                                    .map(BindingsAccessor::new)
                                    .flatMap(QueryVariables::buildApplicationFromBindings)
                                    .collectList()
                            ,
                            this.applicationsStore.query(listConfigurationItemsQuery, ctx.getEnvironment())
                                    .map(BindingsAccessor::new)
                                    .flatMap(QueryVariables::buildConfigurationItemFromBindings)
                                    .collectList()
                    ).flatMapMany(tuple -> {

                        Map<String, Application> applications = tuple.getT1().stream().collect(Collectors.toMap(x -> x.iri().getLocalName(), x -> x));
                        tuple.getT2().forEach(configurationItem -> {
                            if (applications.containsKey(configurationItem.appNode().getLocalName())) {
                                applications.get(configurationItem.appNode().getLocalName()).configuration().put(configurationItem.key(), configurationItem.value());
                            } else
                                Flux.error(new InconsistentModelException("Dangling configuration item pointing to missing node " + configurationItem.node()));
                        });
                        return Flux.fromIterable(applications.values());
                    })
                    .doOnNext(application -> this.cache.put(application.key(), application))
                    .filter(application -> verifyReadingPrivilege(application, ctx))
                    .doOnSubscribe(StreamsLogger.debug(log, "Loading all applications from repository ({})", ctx.getEnvironment()));
        } else {
            return Flux.fromIterable(this.cache.asMap().values()).filter(application -> verifyReadingPrivilege(application, ctx));
        }
    }
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<Application> getApplicationByLabel(String applicationLabel, SessionContext ctx) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthentication().get().isAuthenticated());
        } catch (Exception e) { return Mono.error(e); }


        if (applicationLabel.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) return Mono.empty();

        return this.listApplications(ctx)
                .filter(application -> application.label().equalsIgnoreCase(applicationLabel))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationLabel)))
                .single()
                .doOnSubscribe(StreamsLogger.trace(log, "Loading application with label '{}'", applicationLabel));
    }


    @Override
    public void onApplicationEvent(GraphApplicationEvent event) {
        this.cache.invalidateAll();
        this.cache.cleanUp();
    }


}
