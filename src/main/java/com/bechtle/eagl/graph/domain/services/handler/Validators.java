package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.domain.services.handler.validators.Validator;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class Validators {
    private List<Validator> validators;

    @Autowired
    public void setRegisteredBeans(List<Validator> validators) {
        this.validators = validators;
    }

    public Mono<? extends AbstractModel> delegate(AbstractModel triples, EntityStore graph, Map<String, String> parameters) {
        if(this.validators == null) return Mono.just(triples);

        return Flux.fromIterable(validators)
                .reduce(Mono.just(triples), (modelMono, validator) ->
                        modelMono.map(model ->
                                validator.handle(graph, model, parameters)).flatMap(mono -> mono))
                .flatMap(mono -> mono);

    }
}
