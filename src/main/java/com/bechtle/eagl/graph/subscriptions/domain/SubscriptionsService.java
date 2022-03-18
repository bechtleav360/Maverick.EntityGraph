package com.bechtle.eagl.graph.subscriptions.domain;

import com.bechtle.eagl.graph.domain.model.errors.DuplicateRecordsException;
import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.repository.rdf4j.extensions.BindingsAccessor;
import com.bechtle.eagl.graph.subscriptions.domain.model.ApiKey;
import com.bechtle.eagl.graph.subscriptions.domain.model.Subscription;
import com.bechtle.eagl.graph.subscriptions.repository.SubscriptionsStore;
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
@Slf4j
public class SubscriptionsService {

    private final SubscriptionsStore store;
    private final ValueFactory valueFactory;

    public SubscriptionsService(SubscriptionsStore store) {
        this.store = store;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    public Mono<Subscription> createSubscription(String label, boolean persistent) {
        // generate subscription identifier
        String subscriptionIdentifier = GeneratedIdentifier.generateRandomKey(16);
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, subscriptionIdentifier);

        Subscription subscription = new Subscription(
                subject,
                label,
                subscriptionIdentifier,
                persistent
        );

        // store subscription
        ModelBuilder modelBuilder = new ModelBuilder();

        modelBuilder.subject(subscription.iri());
        modelBuilder.add(RDF.TYPE, Subscription.TYPE);
        modelBuilder.add(Subscription.HAS_KEY, subscription.key());
        modelBuilder.add(Subscription.HAS_LABEL, subscription.label());
        modelBuilder.add(Subscription.IS_PERSISTENT, subscription.persistent());


        return this.store.insert(modelBuilder.build())
                .then(Mono.just(subscription));


    }

    public Mono<ApiKey> getKey(String keyIdentifier) {
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
                        .and(nodeSubscription.has(Subscription.HAS_KEY, subscriptionIdentifier)
                                .andHas(Subscription.IS_PERSISTENT, subActive)
                                .andHas(Subscription.HAS_LABEL, sublabel)
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
                                new Subscription(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(sublabel),
                                        ba.asString(subscriptionIdentifier),
                                        ba.asBoolean(subActive)
                                )
                        )
                );
    }

    private IRI asIRI(BindingSet bindings, Variable var) {
        return (IRI) bindings.getValue(var.getVarName());
    }

    public Flux<ApiKey> getKeysForSubscription(String subscriptionIdentifier) {
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
                        .and(nodeSubscription.has(Subscription.HAS_KEY, subscriptionIdentifier)
                                .andHas(Subscription.IS_PERSISTENT, subPersistent)
                                .andHas(Subscription.HAS_LABEL, sublabel)
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
                                new Subscription(
                                        ba.asIRI(nodeSubscription),
                                        ba.asString(sublabel),
                                        subscriptionIdentifier,
                                        ba.asBoolean(subPersistent)

                                )
                        )
                );
    }


    public Flux<Subscription> getSubscriptions() {
        Variable node = SparqlBuilder.var("n");
        Variable key = SparqlBuilder.var("a");
        Variable label = SparqlBuilder.var("b");
        Variable persistent = SparqlBuilder.var("c");


        SelectQuery q = Queries.SELECT()
                .where(node.isA(Subscription.TYPE)
                        .andHas(Subscription.HAS_KEY, key)
                        .andHas(Subscription.HAS_LABEL, label)
                        .andHas(Subscription.IS_PERSISTENT, persistent)
                )
                .limit(100);

        return this.store.select(q)
                .flatMapMany(Flux::fromIterable)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Subscription(
                                ba.asIRI(node),
                                ba.asString(label),
                                ba.asString(key),
                                ba.asBoolean(persistent)
                        )
                );

    }

    public Mono<ApiKey> generateApiKey(String subscriptionIdentifier, String name) {
        Variable node = SparqlBuilder.var("node");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");

        SelectQuery q = Queries.SELECT()
                .where(node.isA(Subscription.TYPE)
                        .andHas(Subscription.HAS_KEY, subscriptionIdentifier)
                        .andHas(Subscription.HAS_LABEL, sublabel)
                        .andHas(Subscription.IS_PERSISTENT, subPersistent)
                )

                .limit(2);


        return this.store.select(q)
                .flatMap(this::getUniqueBindingSet)
                .map(BindingsAccessor::new)
                .map(ba ->
                        new Subscription(
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
                    modelBuilder.add(apiKey.subscription().iri(), Subscription.HAS_API_KEY, apiKey.iri());

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
        return Mono.error(new NotImplementedException());
    }


}
