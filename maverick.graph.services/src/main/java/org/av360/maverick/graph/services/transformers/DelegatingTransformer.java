package org.av360.maverick.graph.services.transformers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "graph.srvc.transformers.delegator")
public class DelegatingTransformer implements Transformer {

    private List<Transformer> transformers;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Transformer> transformers) {
        this.transformers = transformers;
    }


    public void registerEntityService(EntityServices entityServicesImpl) {
        getRegisteredTransformers().forEach(transformer -> transformer.registerEntityService(entityServicesImpl));
    }

    @Override
    public void registerQueryService(QueryServices queryServices) {
        getRegisteredTransformers().forEach(transformer -> transformer.registerQueryService(queryServices));
    }

    public List<Transformer> getRegisteredTransformers() {
        if (this.transformers == null || this.transformers.isEmpty()) {
            log.warn("Default transformers are missing (not injected), check your spring configuration");
            return List.of();
        } else return this.transformers;
    }

    @Override
    public Mono<? extends TripleModel> handle(TripleModel triples, Map<String, String> parameters) {
        if (this.transformers == null) {
            log.trace("No transformers registered, skip.");
            return Mono.just(triples);
        }

        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) ->
                        modelMono
                                .map(model -> transformer.handle(model, parameters))
                                .flatMap(mono -> mono))
                .flatMap(mono -> mono);
    }
}
