package org.av360.maverick.graph.services.validators;

import org.av360.maverick.graph.model.errors.runtime.MissingType;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j(topic = "graph.srvc.validator.type")
@Component
@ConditionalOnProperty(name = "application.features.validators.checkRequiredType", havingValue = "true")
public class CheckRequiredType implements Validator {

    @Override
    public Mono<? extends TripleModel> handle(EntityServices entityServicesImpl, TripleModel model, Map<String, String> parameters) {
        log.trace("Checking if type is defined");

        for (Resource obj : model.getModel().subjects()) {
            /* check if each node object has a valid type definition */
            if (!model.getModel().contains(obj, RDF.TYPE, null)) {
                log.warn("The object {} is missing a type", obj);
                return Mono.error(new MissingType(obj));
            }
        }
        return Mono.just(model);
    }

}