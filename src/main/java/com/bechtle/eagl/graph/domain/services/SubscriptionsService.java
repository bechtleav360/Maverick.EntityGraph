package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.ADM;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.repository.SubscriptionsStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
public class SubscriptionsService {

    private final SubscriptionsStore store;
    private final ValueFactory valueFactory;

    public SubscriptionsService(SubscriptionsStore store) {
        this.store = store;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    public Mono<String> createSubscription() {
        // generate subscription identifier
        String subscriptionIdentifier = GeneratedIdentifier.generateRandomKey(16);

        // store subscription
        ModelBuilder modelBuilder = new ModelBuilder();
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, subscriptionIdentifier);

        modelBuilder.add(subject, RDF.TYPE, ADM.SUBSCRIPTION);
        modelBuilder.add(subject, ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier);

        return this.store.store(modelBuilder.build())
                .then(Mono.just(subscriptionIdentifier));


    }

    public Mono<ApiKeyDefinition> getKey(String keyIdentifier) {
        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");
        Variable subscriptionIdentifier = SparqlBuilder.var("b");


        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ADM.KEY_HAS_IDENTIFIER, keyIdentifier)
                        .andHas(ADM.KEY_HAS_NAME, keyName)
                        .andHas(ADM.KEY_CREATION_DATE, keyDate)
                        .andHas(ADM.KEY_IS_ACTIVE, keyActive)
                        .andHas(ADM.KEY_OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier))
                );
        String qs = q.getQueryString();

        return this.store.select(q)

                .flatMap(result -> {
                    if (!result.hasNext()) return Mono.empty();

                    List<BindingSet> bindingSets = result.stream().toList();
                    Assert.isTrue(bindingSets.size() == 1, "Found multiple key definitions for id " + keyIdentifier);
                    return Mono.just(bindingSets.get(0));
                })
                .map(bindings ->
                        new ApiKeyDefinition(
                                new NamedKey(
                                        bindings.getValue(keyName.getVarName()).stringValue(),
                                        keyIdentifier
                                ),
                                Literals.getBooleanValue(bindings.getValue(keyActive.getVarName()), false),
                                bindings.getValue(keyDate.getVarName()).stringValue(),
                                bindings.getValue(subscriptionIdentifier.getVarName()).stringValue()
                        )
                );
    }

    public Flux<ApiKeyDefinition> getKeysForSubscription(String subscriptionIdentifier) {
        Variable nodeKey = SparqlBuilder.var("n1");
        Variable nodeSubscription = SparqlBuilder.var("n2");

        Variable keyIdentifier = SparqlBuilder.var("b");
        Variable keyDate = SparqlBuilder.var("c");
        Variable keyActive = SparqlBuilder.var("d");
        Variable keyName = SparqlBuilder.var("e");

        /**
         *                     modelBuilder.add(subject, RDF.TYPE, ADM.API_KEY);
         *                     modelBuilder.add(subject, ADM.KEY_HAS_IDENTIFIER, key);
         *                     modelBuilder.add(subject, ADM.KEY_HAS_NAME, name);
         *                     modelBuilder.add(subject, ADM.KEY_CREATION_DATE, ZonedDateTime.now());
         *                     modelBuilder.add(subject, ADM.KEY_IS_ACTIVE, true);
         *                     modelBuilder.add(subject, ADM.KEY_OF_SUBSCRIPTION, iri);
         *                     modelBuilder.add(iri, ADM.SUBSCRIPTION_API_KEY, subject);
         */
        SelectQuery q = Queries.SELECT()
                .where(nodeKey.has(ADM.KEY_HAS_IDENTIFIER, keyIdentifier)
                        .andHas(ADM.KEY_HAS_NAME, keyName)
                        .andHas(ADM.KEY_CREATION_DATE, keyDate)
                        .andHas(ADM.KEY_IS_ACTIVE, keyActive)
                        .andHas(ADM.KEY_OF_SUBSCRIPTION, nodeSubscription)
                        .and(nodeSubscription.has(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier))
                );
        String qs = q.getQueryString();

        return this.store.select(q)
                .flatMapMany(Flux::fromIterable)
                .map(bindings ->
                        new ApiKeyDefinition(
                                new NamedKey(
                                        bindings.getValue(keyName.getVarName()).stringValue(),
                                        bindings.getValue(keyIdentifier.getVarName()).stringValue()
                                ),
                                Literals.getBooleanValue(bindings.getValue(keyActive.getVarName()), false),
                                bindings.getValue(keyDate.getVarName()).stringValue(),
                                subscriptionIdentifier
                        )
                );
    }


    public Flux<String> getSubscriptions() {
        Variable subscriptionIdentifier = SparqlBuilder.var("id");
        Variable node = SparqlBuilder.var("n");

        SelectQuery q = Queries.SELECT(subscriptionIdentifier)
                .where(node.isA(ADM.SUBSCRIPTION).andHas(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier))
                .limit(100);

        return this.store.select(q)
                .map(result -> result.stream()
                        .map(bindings -> bindings.getValue("id").stringValue()).toList()
                ).flatMapMany(Flux::fromIterable);
    }

    public Mono<NamedKey> generateApiKey(String subscriptionIdentifier, String name) {
        Variable node = SparqlBuilder.var("node");

        SelectQuery q = Queries.SELECT(node)
                .where(node.isA(ADM.SUBSCRIPTION).andHas(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier))
                .limit(2);


        return this.store.select(q)
                .map(result -> {
                    List<Value> nodes = result.stream().map(bindings -> bindings.getValue("node")).toList();
                    Assert.isTrue(nodes.size() == 1, "Found multiple subscriptions with id " + subscriptionIdentifier);
                    Assert.isTrue(nodes.get(0).isIRI(), "Node with id " + subscriptionIdentifier + " not an IRI");
                    return (IRI) nodes.get(0);
                }).flatMap(iri -> {
                    ModelBuilder modelBuilder = new ModelBuilder();

                    GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE);
                    String key = GeneratedIdentifier.generateRandomKey(16);

                    modelBuilder.add(subject, RDF.TYPE, ADM.API_KEY);
                    modelBuilder.add(subject, ADM.KEY_HAS_IDENTIFIER, key);
                    modelBuilder.add(subject, ADM.KEY_HAS_NAME, name);
                    modelBuilder.add(subject, ADM.KEY_CREATION_DATE, ZonedDateTime.now());
                    modelBuilder.add(subject, ADM.KEY_IS_ACTIVE, true);
                    modelBuilder.add(subject, ADM.KEY_OF_SUBSCRIPTION, iri);
                    modelBuilder.add(iri, ADM.SUBSCRIPTION_API_KEY, subject);


                    return this.store.store(modelBuilder.build())
                            .then(Mono.just(new NamedKey(name, key)));
                });

    }

    public Mono<Void> revokeApiKey(String subscriptionId, String name) {
        return Mono.error(new NotImplementedException());
    }


    public record ApiKeyDefinition(NamedKey key, boolean active, String creationDate,
                                   String subscriptionKey) {

    }

    public record NamedKey(String name, String key) {
    }

    public record SubscriptionDefinition(String key) {
    }


}
