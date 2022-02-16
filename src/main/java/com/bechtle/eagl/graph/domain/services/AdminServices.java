package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.services.handler.Transformers;
import com.bechtle.eagl.graph.domain.services.handler.Validators;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class AdminServices {

    private final EntityStore graph;
    private final Validators validators;
    private final Transformers transformers;

    public AdminServices(EntityStore graph, Validators validators, Transformers transformers) {
        this.graph = graph;
        this.validators = validators;
        this.transformers = transformers;
    }


    public Mono<Void> reset() {
        return this.graph.reset();
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype) {
        return this.graph.importStatements(bytes, mimetype);



    }
}
