package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.Identifier;
import com.bechtle.eagl.graph.model.Triples;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class EntityServices {

    private final Graph graph;
    private final SecureRandom secureRandom;

    public EntityServices(Graph graph) {
        this.secureRandom = new SecureRandom();
        this.graph = graph;
    }


    public Flux<Identifier> createEntity(Triples of) {
        return graph.store(of);
    }

    public Mono<Triples> readEntity(Identifier identifier) {
        return  graph.get(identifier);
    }



}
