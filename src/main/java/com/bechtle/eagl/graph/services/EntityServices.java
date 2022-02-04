package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.IncomingModel;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
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

    public EntityServices(Graph graph) {
        this.graph = graph;
    }

    public Flux<NamespaceAwareStatement> readEntity(IRI identifier) {
        return  graph.get(identifier);
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





}
