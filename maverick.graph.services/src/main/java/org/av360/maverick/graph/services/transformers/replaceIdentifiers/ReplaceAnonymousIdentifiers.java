package org.av360.maverick.graph.services.transformers.replaceIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
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
@Slf4j(topic = "graph.srvc.transformer.identifiers.anonymous")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceAnonymousIdentifiers", havingValue = "true")
public class ReplaceAnonymousIdentifiers extends AbstractIdentifierReplace implements Transformer  {


    private final IdentifierServices identifierServices;

    public ReplaceAnonymousIdentifiers(IdentifierServices identifierServices) {
        this.identifierServices = identifierServices;
    }



    public Mono<TripleModel> handle(TripleModel triples, Map<String, String> parameters) {
        return this.handle(triples.getModel(), parameters).map(TripleModel::new);
    }

    public Mono<Model> handle(Model triples, Map<String, String> parameters) {
        return this.buildIdentifierMappings(triples)
                .collect(Collectors.toSet())
                .flatMap(mappings -> replaceIdentifiers(mappings, triples))
                .doOnSuccess(mappings -> {
                    if(mappings.size() > 0) {
                        log.debug("Replaced anonymous identifiers in incoming model with local identifiers.");
                        mappings.forEach(mapping -> log.trace("Mapped: [{}] > [{}]", mapping.oldIdentifier().stringValue(), mapping.newIdentifier().stringValue()));
                    }
                })
                .then(Mono.just(triples))
                .doOnSubscribe(c -> log.debug("(Start) Checking if model contains replaceable anonymous identifiers"))
                .doFinally(signalType -> log.trace("(End) Finished checks for anonymous identifiers"));
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

    protected Mono<IRI> createLocalIdentifierFrom(BNode subj, Model model) {

        Optional<Value> charProp = this.findCharacteristicProperty(subj, model);
        Optional<Value> entityType = model.filter(subj, RDF.TYPE, null).stream().map(Statement::getObject).findFirst();
        LocalIdentifier identifier;
        if (charProp.isPresent() && entityType.isPresent()) {
            // we build the identifier from entity type and value
            return identifierServices.asReproducibleIRI(Local.Entities.NS, entityType.get(), charProp.get());
        } else {
            return identifierServices.asRandomIRI(Local.Entities.NS);
        }
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


}
