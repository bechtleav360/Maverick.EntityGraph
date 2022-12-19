package io.av360.maverick.graph.services.validators;

import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class DelegatingValidator implements Validator {
    private List<Validator> validators;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Validator> validators) {
        this.validators = validators;
    }

    @Override
    public Mono<? extends TripleModel> handle(EntityServices entityServicesImpl, TripleModel triples, Map<String, String> parameters) {
        if (this.validators == null) return Mono.just(triples);

        return Flux.fromIterable(validators)
                .reduce(Mono.just(triples), (modelMono, validator) ->
                        modelMono.map(model ->
                                validator.handle(entityServicesImpl, model, parameters)).flatMap(mono -> mono))
                .flatMap(mono -> mono);
    }
}
