package com.bechtle.cougar.graph.features.multitenancy.domain;

import com.bechtle.cougar.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.cougar.graph.features.multitenancy.domain.model.ApiKey;
import com.bechtle.cougar.graph.features.multitenancy.domain.model.Application;
import com.bechtle.cougar.graph.repository.ApplicationsStore;
import com.bechtle.cougar.graph.repository.rdf4j.extensions.BindingsAccessor;
import com.bechtle.cougar.graph.api.security.errors.RevokedApiKeyUsed;
import com.bechtle.cougar.graph.api.security.errors.UnknownApiKey;
import com.bechtle.cougar.graph.domain.model.errors.DuplicateRecordsException;
import com.bechtle.cougar.graph.domain.model.vocabulary.Local;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@Slf4j(topic = "cougar.graph.feature.applications")
public class ApplicationsService {

    private final ApplicationsStore store;
    private final ValueFactory valueFactory;

    public ApplicationsService(ApplicationsStore store) {
        this.store = store;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    public Mono<Application> createSubscription(String label, boolean persistent) {
        log.debug("(Service) Creating a new subscription with label '{}' and persistence set to '{}' ", label, persistent);
        // generate subscription identifier
        String subscriptionIdentifier = GeneratedIdentifier.generateRandomKey(16);
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, subscriptionIdentifier);

        Application subscription = new Application(
                subject,
                label,
                subscriptionIdentifier,
                persistent
        );

        // store subscription
        ModelBuilder modelBuilder = new ModelBuilder();

        modelBuilder.subject(subscription.iri());
        modelBuilder.add(RDF.TYPE, Application.TYPE);
        modelBuilder.add(Application.HAS_KEY, subscription.key());
        modelBuilder.add(Application.HAS_LABEL, subscription.label());
        modelBuilder.add(Application.IS_PERSISTENT, subscription.persistent());


        return this.store.insert(modelBuilder.build())
                .then(Mono.just(subscription));


    }

    public Mono<ApiKey> getKey(String keyIdentifier) {
        log.debug("(Service) Requesting application details for subscription key '{}'", keyIdentifier);

        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subscriptionIdentifier = SparqlBuilder.var("b");
        Variable subActive = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ApiKey.HAS_KEY, keyIdentifier)
                        .andHas(ApiKey.HAS_LABEL, keyName)
                        .andHas(ApiKey.HAS_ISSUE_DATE, keyDate)
                        .andHas(ApiKey.IS_ACTIVE, keyActive)
                        .andHas(ApiKey.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(Application.HAS_KEY, subscriptionIdentifier)
                                .andHas(Application.IS_PERSISTENT, subActive)
                                .andHas(Application.HAS_LABEL, sublabel)
                        )
                );
        String qs = q.getQueryString();

        return this.store.select(q)

                .flatMap(result -> {
                    if (!result.hasNext()) return Mono.empty();

                    List<BindingSet> bindingSets = result.stream().toList();
                    Assert.isTrue(bindingSets.size() == 1, "Found multiple key definitions for id " + keyIdentifier);
                    return Mono.just(bindingSets.get(0));
                })
                .map(BindingsAccessor::new)
                .map(ba ->
                        new ApiKey(
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
                .filter(ApiKey::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(keyIdentifier)));


    }

    private IRI asIRI(BindingSet bindings, Variable var) {
        return (IRI) bindings.getValue(var.getVarName());
    }

    public Flux<ApiKey> getKeysForSubscription(String subscriptionIdentifier) {
        log.debug("(Service) Requesting all API Keys for application with key '{}'", subscriptionIdentifier);

        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyIdentifier = SparqlBuilder.var("b");
        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ApiKey.HAS_KEY, keyIdentifier)
                        .andHas(ApiKey.HAS_LABEL, keyName)
                        .andHas(ApiKey.HAS_ISSUE_DATE, keyDate)
                        .andHas(ApiKey.IS_ACTIVE, keyActive)
                        .andHas(ApiKey.OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(Application.HAS_KEY, subscriptionIdentifier)
                                .andHas(Application.IS_PERSISTENT, subPersistent)
                                .andHas(Application.HAS_LABEL, sublabel)
                        )

                );
        String qs = q.getQueryString();

        return this.store.select(q)
                .flatMapMany(Flux::fromIterable)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new ApiKey(
                                ba.asIRI(nodeKey),
                                ba.asString(keyName),
                                ba.asString(keyIdentifier),
                                ba.asBoolean(keyActive),
                                ba.asString(keyDate),
                                new Application(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(sublabel),
                                        subscriptionIdentifier,
                                        ba.asBoolean(subPersistent)

                                )
                        )
                );
    }


    public Flux<Application> getSubscriptions() {
        log.debug("(Service) Requesting all subscriptions");

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

        return this.store.select(q)
                .flatMapMany(Flux::fromIterable)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(label),
                                ba.asString(key),
                                ba.asBoolean(persistent)
                        )
                );

    }

    public Mono<ApiKey> generateApiKey(String subscriptionIdentifier, String name) {
        log.debug("(Service) Generating new api key for subscriptions '{}'", subscriptionIdentifier);

            Variable node = SparqlBuilder.var("node");
            Variable subPersistent = SparqlBuilder.var("f");
            Variable sublabel = SparqlBuilder.var("g");

            SelectQuery q = Queries.SELECT()
                    .where(node.isA(Application.TYPE)
                            .andHas(Application.HAS_KEY, subscriptionIdentifier)
                            .andHas(Application.HAS_LABEL, sublabel)
                            .andHas(Application.IS_PERSISTENT, subPersistent)
                    )

                    .limit(2);


        return this.store.select(q)
                .flatMap(this::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Application(
                                ba.asIRI(node),
                                ba.asString(sublabel),
                                subscriptionIdentifier,
                                ba.asBoolean(subPersistent)
                        )
                )
                .map(subscription ->
                        new ApiKey(
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
                    modelBuilder.add(RDF.TYPE, ApiKey.TYPE);
                    modelBuilder.add(ApiKey.HAS_KEY, apiKey.key());
                    modelBuilder.add(ApiKey.HAS_LABEL, apiKey.label());
                    modelBuilder.add(ApiKey.HAS_ISSUE_DATE, apiKey.issueDate());
                    modelBuilder.add(ApiKey.IS_ACTIVE, apiKey.active());
                    modelBuilder.add(ApiKey.OF_SUBSCRIPTION, apiKey.subscription().key());
                    modelBuilder.add(apiKey.subscription().iri(), Application.HAS_API_KEY, apiKey.iri());

                    return this.store.insert(modelBuilder.build()).then(Mono.just(apiKey));
                });
    }

    private Mono<BindingSet> getUniqueBindingSet(TupleQueryResult result) {
        if (!result.hasNext()) return Mono.empty();

        List<BindingSet> bindingSets = result.stream().toList();

        if (bindingSets.size() > 1) {
            log.error("Found multiple results when expected exactly one");
            return Mono.error(new DuplicateRecordsException());
        }

        return Mono.just(bindingSets.get(0));

    }

    public Mono<Void> revokeApiKey(String subscriptionId, String name) {
        log.debug("(Service) Revoking api key for subscription '{}'", subscriptionId);

        return Mono.error(new NotImplementedException());
    }


}
