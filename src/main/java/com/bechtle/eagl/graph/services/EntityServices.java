package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.SimpleIRI;
import com.bechtle.eagl.graph.model.IncomingModel;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.Graph;
import com.bechtle.eagl.graph.repository.Schema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleStatement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;

@Slf4j
@Service
public class EntityServices {

    private final Graph graph;
    private final Schema schema;

    public EntityServices(Graph graph, Schema schema) {
        this.graph = graph;
        this.schema = schema;
    }

    public Flux<NamespaceAwareStatement> readEntity(String identifier) {
        return  graph.get(SimpleIRI.withDefaultNamespace(identifier));
    }



    public Flux<NamespaceAwareStatement> createEntity(IncomingModel triples) throws IOException {

        for(Resource obj : new ArrayList<>(triples.getModel().subjects())) {

            /* check if each node object has a valid type definition */
            if(! triples.getModel().contains(obj, RDF.TYPE, null)) {
                log.error("The object {} is missing a type", obj);
                throw new InvalidObjectException("Missing type definition for object");
            }

            /* TODO: check if create of resource of given type is supported or is it delegated to connector */

            /* Handle Ids */
            if(obj.isBNode()) {
                // generate a new qualified identifier if it is an anonymous node

                triples.generateName(obj);
            } else {
                // TODO: otherwise check if id already exists in graph
            }



            /* TODO: separate into different contexts by prefix */

        };

        return graph.store(triples.getModel());
    }

    /**
     * Saves a value, return an transaction
     * @param id entity id
     * @param prefixedKey identifier for relation
     * @param value value
     * @return transaction model
     */
    public Flux<NamespaceAwareStatement> setValue(String id, String predicatePrefix, String predicateKey, String value) {
        ValueFactory vf = this.graph.getValueFactory();
        SimpleIRI entityIdentifier = SimpleIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow().getName();
        return this.graph.store(
                entityIdentifier,
                SimpleIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value)
        );


    }
}
