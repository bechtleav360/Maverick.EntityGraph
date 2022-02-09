package com.bechtle.eagl.graph.domain.services.handler.validators;

import com.bechtle.eagl.graph.domain.model.errors.MissingType;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.domain.services.handler.AbstractTypeHandler;
import com.bechtle.eagl.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class TypeIsRequired extends AbstractTypeHandler {

    @Override
    public Mono<? extends AbstractModelWrapper> handle(EntityStore graph, Mono<? extends AbstractModelWrapper> mono, Map<String, String> parameters) {
        return mono.flatMap(model -> {
            log.trace("(Validator) Checking if type is defined");

            for (Resource obj : model.getModel().subjects()) {
                /* check if each node object has a valid type definition */
                if (!model.getModel().contains(obj, RDF.TYPE, null)) {
                    log.error("(Validator) The object {} is missing a type", obj);
                    return Mono.error(new MissingType("Missing type definition for object"));
                }
            }
            return Mono.just(model);
        });
    }
}
