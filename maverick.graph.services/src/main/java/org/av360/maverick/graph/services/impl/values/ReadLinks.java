package org.av360.maverick.graph.services.impl.values;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

public class ReadLinks {
    private final ValueServicesImpl ctrl;

    public ReadLinks(ValueServicesImpl valueServices) {
        this.ctrl = valueServices;
    }

    public Mono<RdfEntity> list(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                ctrl.schemaServices.resolveLocalName(entityKey)
                        .flatMap(entityIdentifier -> ctrl.entityServices.get(entityIdentifier, 0, true, ctx)),
                ctrl.schemaServices.resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfEntity entity = pair.getT1();
            IRI property = pair.getT2();

            entity.reduce((st) -> {
                boolean isTypeDefinition = st.getSubject().equals(entity.getIdentifier()) && st.getPredicate().equals(RDF.TYPE);
                boolean isProperty = st.getPredicate().equals(property);
                return isTypeDefinition || isProperty;
            });

            return entity;
        });
    }
}
