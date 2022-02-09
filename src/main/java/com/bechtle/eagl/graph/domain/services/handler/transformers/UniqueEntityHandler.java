package com.bechtle.eagl.graph.domain.services.handler.transformers;

import com.bechtle.eagl.graph.domain.model.vocabulary.SDO;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.domain.services.handler.AbstractTypeHandler;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Mono;

import java.util.Map;

public class UniqueEntityHandler extends AbstractTypeHandler {
    @Override
    public boolean handlesType(Resource object) {
        return object.isIRI() && ((IRI) object).equals(SDO.DEFINED_TERM);
    }

    @Override
    public Mono<? extends AbstractModelWrapper> handle(EntityStore graph, Mono<? extends AbstractModelWrapper> model, Map<String, String> parameters) {
        return model;
    }




}
