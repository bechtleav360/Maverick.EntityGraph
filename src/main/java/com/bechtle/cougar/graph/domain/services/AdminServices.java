package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class AdminServices {

    private final EntityStore graph;

    public AdminServices(EntityStore graph) {
        this.graph = graph;
    }


    public Mono<Void> reset() {
        return this.graph.reset();
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype) {
        log.trace("Importing statements of type '{}' through admin services", mimetype);
        return this.graph.importStatements(bytes, mimetype);



    }
}
