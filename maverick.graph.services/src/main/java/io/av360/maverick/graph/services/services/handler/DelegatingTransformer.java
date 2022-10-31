package io.av360.maverick.graph.services.services.handler;

import io.av360.maverick.graph.store.rdf.models.AbstractModel;
import io.av360.maverick.graph.services.services.EntityServices;
import io.av360.maverick.graph.services.services.QueryServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "graph.transformer.delegator")
public class DelegatingTransformer implements Transformer {

    private List<Transformer> transformers;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Transformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public void registerEntityService(EntityServices entityServices) {
        getRegisteredTransformers().forEach(transformer -> transformer.registerEntityService(entityServices));
    }

    @Override
    public void registerQueryService(QueryServices queryServices) {
        getRegisteredTransformers().forEach(transformer -> transformer.registerQueryService(queryServices));
    }

    public List<Transformer> getRegisteredTransformers() {
        if(this.transformers == null || this.transformers.isEmpty()) {
            log.warn("Default transformers are missing (not injected), check your spring configuration");
            return List.of();
        }
        else return this.transformers;
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
