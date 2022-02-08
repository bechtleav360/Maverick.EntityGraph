package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.repository.EntityStore;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class AdminServices {

    private final EntityStore graph;

    public AdminServices(EntityStore graph) {
        this.graph = graph;
    }


    public Mono<Void> reset() {
        return this.graph.reset();
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype) {
        return this.graph.importStatements(bytes, mimetype);



    }
}
