package org.av360.maverick.graph.services.transformers.replaceIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Replaces blank nodes with valid IRIs
 * <p>
 * See also: https://www.w3.org/2011/rdf-wg/wiki/Skolemisation
 * <p>
 * <p>
 * Systems wishing to skolemise bNodes, and expose those skolem constants to external systems (e.g. in query results) SHOULD mint a "fresh" (globally unique) URI for each bNode.
 * All systems performing skolemisation SHOULD do so in a way that they can recognise the constants once skolemised, and map back to the source bNodes where possible.
 */
@Slf4j(topic = "graph.srvc.transformer.id.anon")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceAnonymousIdentifiers", havingValue = "true")
public class ReplaceAnonymousIdentifiers extends AbstractIdentifierReplace implements Transformer  {

    private final IdentifierFactory identifierFactory;


    public ReplaceAnonymousIdentifiers(IdentifierFactory identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    public IdentifierFactory getIdentifierFactory() {
        return identifierFactory;
    }

    /*
    public Mono<? extends TripleModel> handleOld(TripleModel triples, Map<String, String> parameters) {
        return Mono.create(sink -> {

            try {
                Map<BNode, IRI> mappings = new HashMap<>();

                for (Resource obj : new ArrayList<>(triples.getModel().subjects())) {
                    if (obj.isBNode()) {
                        // generate a new qualified identifier if it is an anonymous node
                        IRI newIdentifier = this.skolemize(obj, triples);
                        mappings.put((BNode) obj, newIdentifier);
                    }
                }
                if (mappings.size() > 0) {
                    log.debug("Generated local identifiers for anonymous identifiers in incoming model.");
                    mappings.forEach((bn, iri) -> log.trace("Mapping {} to {}", bn, iri));
                }


                sink.success(triples);
            } catch (Exception | InvalidEntityModel e) {
                sink.error(e);
            }
        });

    }*/

    public Mono<TripleModel> handle(TripleModel triples, Map<String, String> parameters) {
        return this.handle(triples.getModel(), parameters).map(TripleModel::new);
    }

    public Mono<Model> handle(Model triples, Map<String, String> parameters) {
        return this.buildIdentifierMappings(triples)
                .collect(Collectors.toSet())
                .flatMap(mappings -> replaceIdentifiers(mappings, triples))
                .doOnNext(mappings -> {
                    if(mappings.size() > 0) {
                        log.debug("Replaced anonymous identifiers in incoming model with local identifiers.");
                        mappings.forEach(mapping -> log.trace("Mapping from {} to {}", mapping.oldIdentifier().stringValue(), mapping.newIdentifier().stringValue()));
                    }
                })
                .then(Mono.just(triples))
                .doOnSubscribe(c -> log.debug("Check if model contains replaceable anonymous identifiers"))
                .doFinally(signalType -> log.trace("Finished checks for anonymous identifiers"));
    }

    public Flux<IdentifierMapping> buildIdentifierMappings(Model model) {
        Set<BNode> collect = new HashSet<>();
        model.subjects().stream().filter(Value::isBNode).map(val -> (BNode) val).forEach(collect::add);
        model.objects().stream().filter(Value::isBNode).map(val -> (BNode) val).forEach(collect::add);

        return Flux.fromIterable(collect)
                .flatMap(val ->
                        createLocalIdentifierFrom(val, model)
                            .map(localIdentifier -> new IdentifierMapping(val, localIdentifier))
                );
    }

    protected Mono<LocalIdentifier> createLocalIdentifierFrom(BNode subj, Model model) {

        Optional<Value> charProp = this.findCharacteristicProperty(subj, model);
        Optional<Value> entityType = model.filter(subj, RDF.TYPE, null).stream().map(Statement::getObject).findFirst();
        LocalIdentifier identifier;
        if (charProp.isPresent() && entityType.isPresent()) {
            // we build the identifier from entity type and value
            identifier = identifierFactory.createReproducibleIdentifier(Local.Entities.NS, entityType.get(), charProp.get());

        } else {
            identifier = identifierFactory.createRandomIdentifier(Local.Entities.NS);
        }

        return Mono.just(identifier);
    }

    protected Optional<Value> findCharacteristicProperty(Resource subj, Model model) {
        List<Optional<Value>> props = new ArrayList<>();
        props.add(model.filter(subj, RDFS.LABEL, null).stream().map(Statement::getObject).findFirst());
        props.add(model.filter(subj, DC.IDENTIFIER, null).stream().map(Statement::getObject).findFirst());
        props.add(model.filter(subj, DCTERMS.IDENTIFIER, null).stream().map(Statement::getObject).findFirst());
        props.add(model.filter(subj, SKOS.PREF_LABEL, null).stream().map(Statement::getObject).findFirst());
        props.add(model.filter(subj, SDO.IDENTIFIER, null).stream().map(Statement::getObject).findFirst());
        props.add(model.filter(subj, SDO.TERM_CODE, null).stream().map(Statement::getObject).findFirst());


        return props.stream().filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    /*
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
        if (value.isPresent() && entityType.isPresent()) {
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
    */

}
