package com.bechtle.cougar.graph.domain.services.handler;

import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.domain.services.QueryServices;
import com.bechtle.cougar.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.management.Query;
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
    public void registerEntityService(EntityServices entityServices) {
        this.transformers.forEach(transformer -> transformer.registerEntityService(entityServices));
    }

    @Override
    public void registerQueryService(QueryServices queryServices) {
        this.transformers.forEach(transformer -> transformer.registerQueryService(queryServices));
    }

    @Override
    public Mono<? extends AbstractModel> handle(AbstractModel triples, Map<String, String> parameters, Authentication authentication) {
        if (this.transformers == null) {
            log.trace("No transformers registered, skip.");
            return Mono.just(triples);
        }

        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) ->
                            modelMono
                                    .map(model -> transformer.handle(model, parameters, authentication))
                                    .flatMap(mono -> mono))
                .flatMap(mono -> mono);
    }
}
