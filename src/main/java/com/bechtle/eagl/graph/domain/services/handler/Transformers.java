package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.domain.services.handler.transformers.Transformer;
import com.bechtle.eagl.graph.domain.services.handler.validators.Validator;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class Transformers {

    private List<Transformer> transformers;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Transformer> transformers) {
        this.transformers = transformers;
    }

    public Mono<? extends AbstractModel> delegate(AbstractModel triples, EntityStore graph, Map<String, String> parameters) {
        if(this.transformers == null) return Mono.just(triples);

        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) ->
                        modelMono.map(model ->
                                transformer.handle(graph, model, parameters)).flatMap(mono -> mono))
                .flatMap(mono -> mono);

    }
}
