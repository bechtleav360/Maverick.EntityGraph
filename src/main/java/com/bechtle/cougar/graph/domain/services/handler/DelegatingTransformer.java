package com.bechtle.cougar.graph.domain.services.handler;

import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "cougar.graph.transformers.delegator")
public class DelegatingTransformer implements Transformer {

    private List<Transformer> transformers;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Transformer> transformers) {
        this.transformers = transformers;
    }


    @Override
    public Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel triples, Map<String, String> parameters) {
        if (this.transformers == null) {
            log.trace("No transformers registered, skip.");
            return Mono.just(triples);
        }

        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) ->
                            modelMono
                                    .map(model -> transformer.handle(graph, model, parameters))
                                    .flatMap(mono -> mono))
                .flatMap(mono -> mono);
    }
}
