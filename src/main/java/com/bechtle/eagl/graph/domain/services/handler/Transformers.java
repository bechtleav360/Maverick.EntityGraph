package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.domain.services.handler.transformers.Transformer;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class Transformers {

    @Autowired
    private List<Transformer> transformers;

    public Mono<? extends AbstractModel> delegate(AbstractModel triples, EntityStore graph, Map<String, String> parameters) {
        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) ->
                        modelMono.map(model ->
                                transformer.handle(graph, model, parameters)).flatMap(mono -> mono))
                .flatMap(mono -> mono);

    }
}
