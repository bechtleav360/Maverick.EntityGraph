package io.av360.maverick.graph.feature.applications.domain;

import io.av360.maverick.graph.feature.applications.api.dto.Requests;
import io.av360.maverick.graph.feature.applications.domain.model.ApplicationApiKey;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.model.errors.DuplicateRecordsException;
import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.feature.applications.store.ApplicationsStore;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.av360.maverick.graph.api.security.errors.RevokedApiKeyUsed;
import io.av360.maverick.graph.api.security.errors.UnknownApiKey;

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

    public ApplicationsService(ApplicationsStore store) {
        this.applicationsStore = store;
    }

    /**
     * Creates a new application.
     * @param label Label for the application
     * @param persistent True if data should be stored on disk
     * @param authentication Current authentication information
     * @return New application as mono
     */
    public Mono<Application> createApplication(String label, boolean persistent, Authentication authentication) {

        // generate application identifier
        String applicationIdentifier = GeneratedIdentifier.generateRandomKey(16);
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, applicationIdentifier);

        Application application = new Application(
                subject,
                label,
                applicationIdentifier,
                persistent
        );

        // store application
        ModelBuilder modelBuilder = new ModelBuilder();

        modelBuilder.subject(application.iri());
        modelBuilder.add(RDF.TYPE, Application.TYPE);
        modelBuilder.add(Application.HAS_KEY, application.key());
        modelBuilder.add(Application.HAS_LABEL, application.label());
        modelBuilder.add(Application.IS_PERSISTENT, application.persistent());


        return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.SYSTEM)
                .then(Mono.just(application))
                .doOnSubscribe(subs -> log.debug("Creating a new application with label '{}' and persistence set to '{}' ", label, persistent));


    }

    public Mono<ApplicationApiKey> getKey(String keyIdentifier, Authentication authentication) {

        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subscriptionIdentifier = SparqlBuilder.var("b");
        Variable subActive = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ApplicationApiKey.HAS_KEY, keyIdentifier)
                        .andHas(ApplicationApiKey.HAS_LABEL, keyName)
                        .andHas(ApplicationApiKey.HAS_ISSUE_DATE, keyDate)
                        .andHas(ApplicationApiKey.IS_ACTIVE, keyActive)
                        .andHas(ApplicationApiKey.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(Application.HAS_KEY, subscriptionIdentifier)
                                .andHas(Application.IS_PERSISTENT, subActive)
                                .andHas(Application.HAS_LABEL, sublabel)
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
                        new ApplicationApiKey(
                                ba.asIRI(nodeKey),
                                ba.asString(keyName),
                                keyIdentifier,
                                ba.asBoolean(keyActive),
                                ba.asString(keyDate),
                                new Application(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(sublabel),
                                        ba.asString(subscriptionIdentifier),
                                        ba.asBoolean(subActive)
                                )
                        )
                )
                .switchIfEmpty(Mono.error(new UnknownApiKey(keyIdentifier)))
                .filter(ApplicationApiKey::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(keyIdentifier)))
                .doOnSubscribe(subs -> log.debug("Requesting application details for application key '{}'", keyIdentifier));


    }

    private IRI asIRI(BindingSet bindings, Variable var) {
        return (IRI) bindings.getValue(var.getVarName());
    }

    public Flux<ApplicationApiKey> getKeysForApplication(String applicationIdentifier, Authentication authentication) {


        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyIdentifier = SparqlBuilder.var("b");
        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ApplicationApiKey.HAS_KEY, keyIdentifier)
                        .andHas(ApplicationApiKey.HAS_LABEL, keyName)
                        .andHas(ApplicationApiKey.HAS_ISSUE_DATE, keyDate)
                        .andHas(ApplicationApiKey.IS_ACTIVE, keyActive)
                        .andHas(ApplicationApiKey.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(Application.HAS_KEY, applicationIdentifier)
                                .andHas(Application.IS_PERSISTENT, subPersistent)
                                .andHas(Application.HAS_LABEL, sublabel)
                        )

                );
        String qs = q.getQueryString();

        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new ApplicationApiKey(
                                ba.asIRI(nodeKey),
                                ba.asString(keyName),
                                ba.asString(keyIdentifier),
                                ba.asBoolean(keyActive),
                                ba.asString(keyDate),
                                new Application(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(sublabel),
                                        applicationIdentifier,
                                        ba.asBoolean(subPersistent)

                                )
                        )
                )
                .doOnSubscribe(sub -> log.debug("Requesting all API Keys for application with key '{}'", applicationIdentifier));
    }


    public Flux<Application> getApplications(Authentication authentication) {


        Variable node = SparqlBuilder.var("n");
        Variable key = SparqlBuilder.var("a");
        Variable label = SparqlBuilder.var("b");
        Variable persistent = SparqlBuilder.var("c");


        SelectQuery q = Queries.SELECT()
                .where(node.isA(Application.TYPE)
                        .andHas(Application.HAS_KEY, key)
                        .andHas(Application.HAS_LABEL, label)
                        .andHas(Application.IS_PERSISTENT, persistent)
                )
                .limit(100);

        return this.applicationsStore.query(q, authentication, Authorities.SYSTEM)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(label),
                                ba.asString(key),
                                ba.asBoolean(persistent)
                        )
                )
                .doOnSubscribe(sub -> log.debug("Requesting all applications"));

    }

    public Mono<ApplicationApiKey> generateApiKey(String applicationIdentifier, String name, Authentication authentication) {


        Variable node = SparqlBuilder.var("node");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(node.isA(Application.TYPE)
                        .andHas(Application.HAS_KEY, applicationIdentifier)
                        .andHas(Application.HAS_LABEL, sublabel)
                        .andHas(Application.IS_PERSISTENT, subPersistent)
                )

                .limit(2);


        return this.applicationsStore.query(q, authentication, Authorities.APPLICATION)
                .collectList()
                .flatMap(this::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(sublabel),
                                applicationIdentifier,
                                ba.asBoolean(subPersistent)
                        )
                )
                .map(subscription ->
                        new ApplicationApiKey(
                                new GeneratedIdentifier(Local.Subscriptions.NAMESPACE),
                                name,
                                GeneratedIdentifier.generateRandomKey(16),
                                true,
                                ZonedDateTime.now().toString(),
                                subscription
                        )
                )
                .flatMap(apiKey -> {
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.subject(apiKey.iri());
                    modelBuilder.add(RDF.TYPE, ApplicationApiKey.TYPE);
                    modelBuilder.add(ApplicationApiKey.HAS_KEY, apiKey.key());
                    modelBuilder.add(ApplicationApiKey.HAS_LABEL, apiKey.label());
                    modelBuilder.add(ApplicationApiKey.HAS_ISSUE_DATE, apiKey.issueDate());
                    modelBuilder.add(ApplicationApiKey.IS_ACTIVE, apiKey.active());
                    modelBuilder.add(ApplicationApiKey.OF_SUBSCRIPTION, apiKey.application().key());
                    modelBuilder.add(apiKey.application().iri(), Application.HAS_API_KEY, apiKey.iri());

                    return this.applicationsStore.insert(modelBuilder.build(), authentication, Authorities.APPLICATION).then(Mono.just(apiKey));
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

    public Mono<Void> revokeApiKey(String subscriptionId, String name, Authentication authentication) {
        log.debug("(Service) Revoking api key for application '{}'", subscriptionId);

        return Mono.error(new NotImplementedException());
    }

    public Mono<?> updateApplicationConfig(String applicationId, String s3Host, String s3Bucket, String exportFrequency, Authentication authentication) {
        log.debug("(Service) Updating application config for application '{}'", applicationId);
        //TODO: Implement
        return Mono.error(new NotImplementedException());
    }

    public Mono<?> getApplicationConfig(String applicationId, Authentication authentication) {
        log.debug("(Service) Getting application config for application '{}'", applicationId);
        //TODO: Implement
        return Mono.error(new NotImplementedException());
    }

    public Mono<?> exportApplication(String applicationId, Authentication authentication) {
        log.debug("(Service) Exporting application '{}'", applicationId);
        // TODO: Implement
        // Select All Sparql Query for application
        // Export to S3


        return Mono.error(new NotImplementedException());
    }

    public Mono<?> getExport(String applicationId, String exportId, Authentication authentication) {
        log.debug("(Service) Getting export '{}' for application '{}'", exportId, applicationId);
        //TODO: Implement
        return Mono.error(new NotImplementedException());
    }
}
