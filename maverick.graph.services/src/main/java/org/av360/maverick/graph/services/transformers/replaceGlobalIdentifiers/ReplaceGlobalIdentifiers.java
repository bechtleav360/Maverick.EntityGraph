package org.av360.maverick.graph.services.transformers.replaceGlobalIdentifiers;

import org.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import org.av360.maverick.graph.model.rdf.NamespacedModelBuilder;
import org.av360.maverick.graph.model.shared.ChecksumIdentifier;
import org.av360.maverick.graph.model.shared.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Will check any entity IRI in the model, if it doesn't conform to the internal schema, a new identifier is generated (we keep the old one)
 * <p>
 * Potential for conflict:
 * <p>
 * the new identifier is a hash of the old identifier to have reproducible results. If the identifier is reused (e.g. example.org/identifier), all
 * new statements are aggregated to one big entity. We could add a random seed into it, but that means we cannot reproduce it anymore
 */
@Slf4j(topic = "graph.srvc.transformer.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceGlobalIdentifiers", havingValue = "true")
public class ReplaceGlobalIdentifiers implements Transformer {

    @Override
    public Mono<? extends TripleModel> handle(TripleModel triples, Map<String, String> parameters, Authentication authentication) {




        List<NamespaceAwareStatement> copy = triples.streamNamespaceAwareStatements().toList();

        Map<Resource, LocalIdentifier> mappings = triples.getModel().subjects().stream()
                .filter(Value::isIRI)
                .filter(iri -> !LocalIdentifier.is((IRI) iri, Local.Entities.NAMESPACE))
                // TODO: here we should extract characteristic properties and include them in the identifier generation
                .map(iri -> Pair.of(iri, new ChecksumIdentifier(Local.Entities.NAMESPACE, (IRI) iri)))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        triples.reset();

        if(mappings.size() > 0) {
            log.debug("Replaced global identifiers in incoming model with local identifiers.");
            mappings.forEach((oldId, newId) -> log.trace("Mapping from {} to {}", oldId.stringValue(), newId.stringValue()));
        }


        NamespacedModelBuilder builder = triples.getBuilder();

        for (Statement st : copy) {
            if (mappings.containsKey(st.getSubject())) {
                builder.add(mappings.get(st.getSubject()), st.getPredicate(), st.getObject());
            } else if (st.getObject().isIRI() && mappings.containsKey((IRI) st.getObject())) {
                builder.add(st.getSubject(), st.getPredicate(), mappings.get((IRI) st.getObject()));
            } else {
                builder.add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }

        // preserve old ids
        mappings.forEach((key, value) -> {
            builder.add(value, Local.ORIGINAL_IDENTIFIER, key);
        });

        return Mono.just(triples);

    }

    @Override
    public void registerEntityService(EntityServices entityServicesImpl) {

    }

    @Override
    public void registerQueryService(QueryServices queryServices) {

    }
}
