package org.av360.maverick.graph.services.transformers.replaceAnonymousIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModel;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Replaces blank nodes with valid IRIs
 * <p>
 * See also: https://www.w3.org/2011/rdf-wg/wiki/Skolemisation
 * <p>
 * <p>
 * Systems wishing to skolemise bNodes, and expose those skolem constants to external systems (e.g. in query results) SHOULD mint a "fresh" (globally unique) URI for each bNode.
 * All systems performing skolemisation SHOULD do so in a way that they can recognise the constants once skolemised, and map back to the source bNodes where possible.
 */
@Slf4j(topic = "graph.srvc.transformer.skolemizer")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceAnonymousIdentifiers", havingValue = "true")
public class ReplaceAnonymousIdentifiers implements Transformer {

    private final IdentifierFactory identifierFactory;

    public ReplaceAnonymousIdentifiers(IdentifierFactory identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    @Override
    public Mono<? extends TripleModel> handle(TripleModel triples, Map<String, String> parameters, Authentication authentication) {
        return Mono.create(sink -> {

            try {
                Map<BNode, IRI> mappings = new HashMap<>();

                for (Resource obj : new ArrayList<>(triples.getModel().subjects())) {
                    /* Handle Ids */
                    if (obj.isBNode()) {
                        // generate a new qualified identifier if it is an anonymous node
                        IRI newIdentifier = this.skolemize(obj, triples);
                        mappings.put((BNode) obj, newIdentifier);
                    }
                }
                if(mappings.size() > 0) {
                    log.debug("Generated local identifiers for anonymous identifiers in incoming model.");
                    mappings.forEach((bn, iri) -> log.trace("Mapping {} to {}", bn, iri));
                }


                sink.success(triples);
            } catch (Exception | InvalidEntityModel e) {
                sink.error(e);
            }
        });



    }

    public IRI skolemize(Resource subj, TripleModel triples) throws InvalidEntityModel {



        List<Optional<Value>> props = new ArrayList<>();
        props.add(triples.findDistinctValue(subj, RDFS.LABEL));
        props.add(triples.findDistinctValue(subj, DC.IDENTIFIER));
        props.add(triples.findDistinctValue(subj, DCTERMS.IDENTIFIER));
        props.add(triples.findDistinctValue(subj, SKOS.PREF_LABEL));
        props.add(triples.findDistinctValue(subj, SDO.IDENTIFIER));
        props.add(triples.findDistinctValue(subj, SDO.TERM_CODE));

        Optional<Value> value = props.stream().filter(Optional::isPresent).findFirst().orElse(Optional.empty());
        Optional<Value> entityType = triples.findDistinctValue(subj, RDF.TYPE);
        IRI identifier;
        if(value.isPresent() && entityType.isPresent()) {
            // we build the identifier from entity type and value
            identifier = identifierFactory.createReproducibleIdentifier(Local.Entities.NS, entityType.get(), value.get());

        } else {
            identifier = identifierFactory.createRandomIdentifier(Local.Entities.NS);
        }

        List<NamespaceAwareStatement> copy = triples.streamNamespaceAwareStatements().toList();

        triples.reset();

        for (Statement st : copy) {
            if (st.getSubject().equals(subj)) {
                triples.getBuilder().add(identifier, st.getPredicate(), st.getObject());
            } else if (st.getObject().equals(subj)) {
                triples.getBuilder().add(st.getSubject(), st.getPredicate(), identifier);
            } else {
                triples.getBuilder().add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }

        return identifier;
    }


}
