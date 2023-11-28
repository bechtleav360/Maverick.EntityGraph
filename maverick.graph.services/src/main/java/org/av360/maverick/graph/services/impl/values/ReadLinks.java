package org.av360.maverick.graph.services.impl.values;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

public class ReadLinks {
    private final ValueServicesImpl ctrl;

    public ReadLinks(ValueServicesImpl valueServices) {
        this.ctrl = valueServices;
    }

    public Mono<Triples> list(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                ctrl.identifierServices.asLocalIRI(entityKey, ctx.getEnvironment())
                        .flatMap(entityIdentifier -> ctrl.entityServices.get(entityIdentifier, true, 0, ctx)),
                ctrl.schemaServices.resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfFragment entity = pair.getT1();
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
