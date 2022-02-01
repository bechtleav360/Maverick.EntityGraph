package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.Identifier;
import com.bechtle.eagl.graph.model.Triples;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InvalidObjectException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class EntityServices {

    private final Graph graph;
    private final SecureRandom secureRandom;

    public EntityServices(Graph graph) {
        this.secureRandom = new SecureRandom();
        this.graph = graph;
    }


    public Flux<Identifier> createEntity(Triples of) throws InvalidObjectException {

        for(Resource obj : of.getModel().subjects()) {
            if(! of.getModel().contains(obj, RDF.TYPE, null)) {
                log.error("The object {} is missing a type", obj);
                throw new InvalidObjectException("Missing type definition for object");
            }
        };

        List<Statement> statements = of.getModel().filter(null, RDF.TYPE, null).stream().toList();
        statements.forEach(sts -> log.trace("statement {}", sts));

        // mutate anonymous node into proper id




        return graph.store(of);
    }

    public Mono<Triples> readEntity(Identifier identifier) {
        return  graph.get(identifier);
    }

    private String generateRandomBase64Token() {
        byte[] token = new byte[16];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }


}
