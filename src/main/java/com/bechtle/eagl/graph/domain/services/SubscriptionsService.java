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
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionsService {

    private final SubscriptionsStore store;
    private final ValueFactory valueFactory;

    public SubscriptionsService(SubscriptionsStore store) {
        this.store = store;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    public Mono<String> create() {
        // generate subscription identifier
        UUID uuid = UUID.randomUUID();

        // store subscription
        ModelBuilder modelBuilder = new ModelBuilder();
        GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE, uuid.toString());

        modelBuilder.add(subject, RDF.TYPE, ADM.SUBSCRIPTION);
        modelBuilder.add(subject, ADM.SUBSCRIPTION_HAS_IDENTIFIER, uuid.toString());

        return this.store.store(modelBuilder.build())
                .then(Mono.just(uuid.toString()));



    }

    public Flux<String> getKeysForSubscription(String subscriptionIdentifier) {
        Variable node = SparqlBuilder.var("node");
        Variable key = SparqlBuilder.var("key");

        SelectQuery q = Queries.SELECT(node)
                .where(node.isA(ADM.SUBSCRIPTION)
                        .andHas(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier)
                        .andHas(ADM.SUBSCRIPTION_API_KEY, key));
        return this.store.select(q)
                .map(result -> result.stream()
                        .map(bindings -> bindings.getValue("key").stringValue()).toList()
                ).flatMapMany(Flux::fromIterable);
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

    public Mono<String> generateApiKey(String subscriptionIdentifier, String name) {
        Variable node = SparqlBuilder.var("node");

        SelectQuery q = Queries.SELECT(node)
                .where(node.isA(ADM.SUBSCRIPTION).andHas(ADM.SUBSCRIPTION_HAS_IDENTIFIER, subscriptionIdentifier))
                .limit(2);


        return this.store.select(q)
                .map(result -> {
                    List<Value> nodes = result.stream().map(bindings -> bindings.getValue("node")).toList();
                    Assert.isTrue(nodes.size() == 1, "Found multiple subscriptions with id "+subscriptionIdentifier);
                    Assert.isTrue(nodes.get(0).isIRI(), "Node with id "+subscriptionIdentifier+" not an IRI");
                    return (IRI) nodes.get(0);
                }).flatMap(iri -> {
                    ModelBuilder modelBuilder = new ModelBuilder();

                    GeneratedIdentifier subject = new GeneratedIdentifier(Local.Subscriptions.NAMESPACE);
                    String key = GeneratedIdentifier.generateRandomKey(16);

                    modelBuilder.add(subject, RDF.TYPE, ADM.API_KEY);
                    modelBuilder.add(subject, ADM.KEY_HAS_IDENTIFIER, key);
                    modelBuilder.add(subject, ADM.KEY_CREATION_DATE, ZonedDateTime.now());
                    modelBuilder.add(iri, ADM.SUBSCRIPTION_API_KEY, subject);
                    return this.store.store(modelBuilder.build()).then(Mono.just(key));
                });

    }

    public Mono<Void> revokeApiKey(String subscriptionId, String name) {
        return Mono.error(new NotImplementedException());
    }


}
