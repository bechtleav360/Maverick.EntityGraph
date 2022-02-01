package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.model.Identifier;
import com.bechtle.eagl.graph.model.Triples;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Graph {


    /**
     * Stores the triples.
     *
     * Can store multiple entities.
     *
     * @param triples the statements to store
     * @return  Returns the identifier (IRI) of the created entity.
     */
    Flux<Identifier> store(Triples triples);


    Mono<Triples> get(Identifier id);
}
