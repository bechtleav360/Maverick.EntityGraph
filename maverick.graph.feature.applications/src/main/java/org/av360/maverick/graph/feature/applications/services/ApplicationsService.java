package org.av360.maverick.graph.feature.applications.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.model.domain.*;
import org.av360.maverick.graph.feature.applications.model.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.model.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.model.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.model.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.model.events.GraphApplicationEvent;
import org.av360.maverick.graph.feature.applications.model.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applications separate tenants. Each node has its own separate stores.
 * An node has a set of unique API Keys. The api key identifies the node.
 */
@Service
@Slf4j(topic = "graph.feat.apps.svc")
public class ApplicationsService implements ApplicationListener<GraphApplicationEvent> {

    private final Cache<String, Application> cache;

    public static boolean APPLICATION_CACHING = true;

    private final ApplicationsStore store;
    private final ApplicationEventPublisher eventPublisher;


    public ApplicationsService(ApplicationsStore applicationsStore, ApplicationEventPublisher eventPublisher) {
        this.store = applicationsStore;
        this.eventPublisher = eventPublisher;
        // this.cache = Caffeine.newBuilder().recordStats().expireAfterAccess(60, TimeUnit.MINUTES).build();
        this.cache = Caffeine.newBuilder().recordStats().build();
    }


    /**
     * Creates a new node.
     *
     * @param authentication Current authentication information
     * @param label          Label for the node
     * @param tags
     * @param flags          Flags for this node
     * @return New node as mono
     */
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> createApplication(String label, Set<String> tags, ApplicationFlags flags, Map<String, Serializable> configuration, SessionContext ctx) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthenticationOrThrow().isAuthenticated());
            Validate.isTrue(Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities()));

            // FIXME: authorize in service
        } catch (Exception e) {
            return Mono.error(e);
        }


        IRI subject = IdentifierServices.buildRandomIRI(Local.Applications.NAME);

        Application application = new Application(
                subject,
                label,
                subject.getLocalName(),
                tags,
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

        application.tags().forEach(tag -> {
            modelBuilder.add(ApplicationTerms.HAS_KEYWORD, tag);
        });

        application.configuration().forEach((key, value) -> {
            this.buildConfigurationItem(key, value, application.iri(), modelBuilder);
        });

        Mono<Application> applicationMono = this.store.importStatements(modelBuilder.build(), ctx.getEnvironment())
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
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> addKeyword(Application application, String tag, SessionContext ctx) {
        Statement statement = Statements.statement(application.iri(), ApplicationTerms.HAS_KEYWORD, Values.literal(tag), null);
        return this.store.commit(new RdfTransaction().inserts(statement), ctx.getEnvironment().setRepositoryType(RepositoryType.APPLICATION))
                .then(this.getApplication(application.key(), ctx, true))
                .doOnNext(trx -> this.cache.invalidateAll())
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(app));
                    log.debug("Added keyword '{}' to application with label '{}'", tag, app.label());
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Adding keyword '{}' to application with label '{}'", tag, application.label()));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> removeKeyword(Application application, String keyword, SessionContext ctx) {
        Statement statement = Statements.statement(application.iri(), ApplicationTerms.HAS_KEYWORD, Values.literal(keyword), null);
        return this.store.commit(new RdfTransaction().removes(statement), ctx.getEnvironment().setRepositoryType(RepositoryType.APPLICATION))
                .doOnNext(trx -> this.cache.invalidateAll())
                .then(this.getApplication(application.key(), ctx, true))
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(app));
                    log.debug("Removed keyword '{}' from application with label '{}'", keyword, app.label());
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Removing keyword '{}' from application with label '{}'", keyword, application.label()));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> createConfigurationItem(Application application, String configKey, Serializable configValue, SessionContext ctx) {
        this.assertUpdatePrivilege(application, ctx);

        ModelBuilder m = this.buildConfigurationItem(configKey, configValue, application.iri(), null);


        return this.deleteConfigurationItem(application, configKey, ctx)
                .then(
                        this.store.commit(new RdfTransaction().inserts(m.build()), ctx.getEnvironment().setRepositoryType(RepositoryType.APPLICATION))
                                .doOnSuccess(trx -> this.cache.invalidate(application.key()))
                )
                .then(
                        this.getApplication(application.key(), ctx, true)
                )
                .doOnSuccess(app -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(app));
                    log.debug("Updated configuration key '{}' of application with label '{}'", configKey, app.label());
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Updating configuration key '{}' for application with label '{}'", configKey, application.label()));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Void> setMetric(Application application, String key, Serializable value, SessionContext ctx) {
        ModelBuilder m = this.buildMetricsItem(key, value, application.iri(), null);

        return this.store.commit(new RdfTransaction().inserts(m.build()), ctx.getEnvironment().setRepositoryType(RepositoryType.APPLICATION))
                .filter(Transaction::isCompleted)
                .then()
                .doOnSuccess(app -> {
                    log.trace("Updated metrics '{}' of application with label '{}'", application.key(), application.label());
                })
                .doOnSubscribe(StreamsLogger.debug(log, "Updating metrics '{}' for node with label '{}'", key, application.label()));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Void> deleteConfigurationItem(Application application, String configurationKey, SessionContext ctx) {
        this.assertUpdatePrivilege(application, ctx);

        SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                        .andHas(ApplicationTerms.CONFIG_KEY, configurationKey)
                        .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                        .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, application.iri()))
                );
        Flux<IRI> nodesToDelete = this.store.query(listConfigurationItemsQuery, ctx.getEnvironment())
                .map(BindingsAccessor::new)
                .map(ba -> ba.findValue(QueryVariables.varNodeConfigurationItem))
                .filter(Optional::isPresent)
                .filter(opt -> opt.get().isIRI())
                .map(opt -> (IRI) opt.get());
        return nodesToDelete.flatMap(configNode -> this.store.listStatements(configNode, null, null, ctx.getEnvironment()))
                .map(statements -> new RdfTransaction().removes(statements))
                .flatMap(transaction -> this.store.commit(transaction, ctx.getEnvironment()))
                .then()
                .doOnSuccess(res -> {
                    this.eventPublisher.publishEvent(new ApplicationUpdatedEvent(application));
                    log.debug("Removed configuration item with key '{}' from application with label '{}'", configurationKey, application.label());
                });
    }

    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Flux<ConfigurationItem> listConfigurationItems(Application application, SessionContext ctx) {
        assertReadPrivilege(application, ctx);

        SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                        .andHas(ApplicationTerms.CONFIG_KEY, QueryVariables.varConfigKey)
                        .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                        .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, application.iri()))
                );
        return this.store.query(listConfigurationItemsQuery, ctx.getEnvironment())
                .map(BindingsAccessor::new)
                .flatMap(QueryVariables::buildConfigurationItemFromBindings);

    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Void> delete(Application application, SessionContext context) {
        assertUpdatePrivilege(application, context);

        return this.listConfigurationItems(application, context)
                .flatMap(configurationItem -> this.deleteConfigurationItem(application, configurationItem.key(), context)).collectList()
                .doOnSuccess(suc -> log.trace("Deleted all configuration item statements for application with label '{}'", application.label()))
                .then(this.store.listStatements(application.iri(), null, null, context.getEnvironment()))
                .map(statements -> new RdfTransaction().removes(statements))
                .flatMap(trx -> this.store.asCommitable().commit(trx, context.getEnvironment()))
                .doOnSuccess(suc -> log.trace("Deleted all application statements for application with label '{}'", application.label()))
                .then()
                .doOnSuccess(res -> {
                    this.eventPublisher.publishEvent(new ApplicationDeletedEvent(application.label()));
                    log.debug("Deleted application with label '{}'", application.label());
                });


    }

    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> getApplication(String applicationKey, SessionContext ctx) {
        return this.getApplication(applicationKey, ctx, false);
    }


    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> getApplication(String applicationKey, SessionContext ctx, boolean ignoreCache) {

        Application cached = APPLICATION_CACHING && !ignoreCache ? this.cache.getIfPresent(applicationKey) : null;
        Mono<Application> response = null;
        if (Objects.isNull(cached) || ignoreCache) {
            // rebuild cache
            response = this.listApplications(Set.of(), ctx, ignoreCache)
                    .filter(application -> {
                        boolean res = application.key().equalsIgnoreCase(applicationKey);
                        return res;
                    })
                    .switchIfEmpty(Mono.error(new InvalidApplication(applicationKey)))
                    .single();
        } else {
            response = Mono.just(cached);
        }

        return response.filter(application -> verifyReadingPrivilege(application, ctx));
    }

    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Flux<Application> listApplications(Set<String> tags, SessionContext ctx, boolean ignoreCache) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthentication().get().isAuthenticated());
        } catch (Exception e) {
            return Flux.error(e);
        }

        if (!APPLICATION_CACHING || ignoreCache || this.cache.asMap().isEmpty()) {

            GraphPattern whereClause = QueryVariables.varNodeApplication.isA(ApplicationTerms.TYPE)
                    .andHas(ApplicationTerms.HAS_KEY, QueryVariables.varAppKey)
                    .andHas(ApplicationTerms.HAS_LABEL, QueryVariables.varAppLabel)
                    .andHas(ApplicationTerms.IS_PERSISTENT, QueryVariables.varAppFlagPersistent)
                    .andHas(ApplicationTerms.IS_PUBLIC, QueryVariables.varAppFlagPublic)
                    .and(QueryVariables.varNodeApplication.has(ApplicationTerms.HAS_KEYWORD, QueryVariables.varAppKeyword).optional());

            if (Objects.nonNull(tags) && ! tags.isEmpty()) {
                Expression<?>[] array = tags.stream()
                        .map(tag -> Expressions.equals(QueryVariables.varAppKeyword, Rdf.literalOf(tag)))
                        .toArray(Expression[]::new);
                whereClause.filter(Expressions.and(array));
            }


            SelectQuery listApplicationsQuery = Queries.SELECT(
                            QueryVariables.varNodeApplication,
                            QueryVariables.varAppKey,
                            QueryVariables.varAppLabel,
                            QueryVariables.varAppFlagPersistent,
                            QueryVariables.varAppFlagPublic,
                            Expressions.group_concat(QueryVariables.varAppKeyword).as(QueryVariables.varAppKeywordList))
                    .where(whereClause)
                    .groupBy(QueryVariables.varNodeApplication, QueryVariables.varAppKey, QueryVariables.varAppLabel, QueryVariables.varAppFlagPersistent, QueryVariables.varAppFlagPublic);

            String query = listApplicationsQuery.getQueryString();


            SelectQuery listConfigurationItemsQuery = Queries.SELECT()
                    .where(QueryVariables.varNodeConfigurationItem.isA(ApplicationTerms.CONFIGURATION_ITEM)
                            .andHas(ApplicationTerms.CONFIG_KEY, QueryVariables.varConfigKey)
                            .andHas(ApplicationTerms.CONFIG_VALUE, QueryVariables.varConfigValue)
                            .and(QueryVariables.varNodeConfigurationItem.has(ApplicationTerms.CONFIG_FOR, QueryVariables.varNodeApplication).optional())
                    );

            return Mono.zip(
                            this.store.query(listApplicationsQuery, ctx.getEnvironment())
                                    .map(BindingsAccessor::new)
                                    .flatMap(QueryVariables::buildApplicationFromBindings)
                                    .collectList()
                            ,
                            this.store.query(listConfigurationItemsQuery, ctx.getEnvironment())
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
                    .doOnNext(application -> {
                        if(APPLICATION_CACHING) {
                            this.cache.put(application.key(), application);
                        }
                    })
                    .filter(application -> verifyReadingPrivilege(application, ctx))
                    .doOnSubscribe(StreamsLogger.debug(log, "Loading all applications from repository ({})", ctx.getEnvironment()));
        } else {
            return Flux.fromIterable(this.cache.asMap().values())
                    .filter(application -> verifyReadingPrivilege(application, ctx))
                    .filter(application -> {
                        if(Objects.isNull(tags) || tags.isEmpty()) return true;
                        return tags.stream().map(requestedTag -> application.tags().contains(requestedTag)).reduce(true, Boolean::logicalAnd);
                    })
                    ;
        }
    }

    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.APPLICATION)
    public Mono<Application> getApplicationByLabel(String applicationLabel, SessionContext ctx) {
        try {
            Validate.isTrue(ctx.getAuthentication().isPresent());
            Validate.isTrue(ctx.getAuthentication().get().isAuthenticated());
        } catch (Exception e) {
            return Mono.error(e);
        }


        if (applicationLabel.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) return Mono.empty();

        return this.listApplications(Set.of(), ctx, false)
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


    private ModelBuilder buildConfigurationItem(String configKey, Serializable configValue, IRI applicationIdentifier, @Nullable ModelBuilder builder) {
        if (Objects.isNull(builder)) builder = new ModelBuilder();

        IRI configNode = IdentifierServices.buildRandomIRI(Local.Applications.NAME);
        builder.add(configNode, RDF.TYPE, ApplicationTerms.CONFIGURATION_ITEM);
        builder.add(configNode, ApplicationTerms.CONFIG_KEY, configKey);
        builder.add(configNode, ApplicationTerms.CONFIG_VALUE, configValue.toString());
        builder.add(configNode, ApplicationTerms.CONFIG_FOR, applicationIdentifier);

        return builder;
    }

    private ModelBuilder buildMetricsItem(String metricsName, Serializable value, IRI applicationIdentifier, @Nullable ModelBuilder builder) {
        if (Objects.isNull(builder)) builder = new ModelBuilder();

        IRI configNode = IdentifierServices.buildRandomIRI(Local.Applications.NAME);
        builder.add(configNode, RDF.TYPE, SDO.QUANTITATIVE_VALUE);
        builder.add(configNode, SDO.NAME, metricsName);
        builder.add(configNode, SDO.VALUE, value);
        builder.add(configNode, ApplicationTerms.METRIC_FOR, applicationIdentifier);

        return builder;
    }

    @Deprecated
    private void assertUpdatePrivilege(Application requestedApplication, SessionContext ctx) {
        if (!this.verifyUpdatePrivilege(requestedApplication, ctx))
            throw new InsufficientPrivilegeException("Unknown application or insufficient privilege.");
    }

    @Deprecated
    private void assertReadPrivilege(Application requestedApplication, SessionContext ctx) {
        if (!this.verifyReadingPrivilege(requestedApplication, ctx))
            throw new InsufficientPrivilegeException("Unknown application or insufficient privilege.");
    }

    @Deprecated
    private boolean verifyReadingPrivilege(Application requestedApplication, SessionContext ctx) {
        boolean isPublic = requestedApplication.flags().isPublic();
        boolean isSubscriber = (ctx.getAuthenticationOrThrow() instanceof SubscriptionToken subscriptionToken) && (subscriptionToken.getApplication().key().equalsIgnoreCase(requestedApplication.key()));
        boolean isAdmin = Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities());

        return isPublic || isSubscriber || isAdmin;
    }

    private boolean verifyUpdatePrivilege(Application requestedApplication, SessionContext ctx) {
        boolean isApplicationAdmin = (ctx.getAuthenticationOrThrow() instanceof SubscriptionToken subscriptionToken)
                && (subscriptionToken.getApplication().key().equalsIgnoreCase(requestedApplication.key()))
                && Authorities.satisfies(Authorities.APPLICATION, ctx.getAuthenticationOrThrow().getAuthorities());
        boolean isAdmin = Authorities.satisfies(Authorities.SYSTEM, ctx.getAuthenticationOrThrow().getAuthorities());

        return isApplicationAdmin || isAdmin;
    }



}
