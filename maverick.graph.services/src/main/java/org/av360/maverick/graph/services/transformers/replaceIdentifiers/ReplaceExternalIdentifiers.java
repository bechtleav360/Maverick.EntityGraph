package org.av360.maverick.graph.services.transformers.replaceIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
@Slf4j(topic = "graph.srvc.transformer.ids.ext")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceGlobalIdentifiers", havingValue = "true")
public class ReplaceExternalIdentifiers extends AbstractIdentifierReplace implements Transformer {

    private final IdentifierFactory identifierFactory;



    public IdentifierFactory getIdentifierFactory() {
        return identifierFactory;
    }



    public ReplaceExternalIdentifiers(IdentifierFactory identifierFactory) {
        this.identifierFactory = identifierFactory;
    }


    @Override
    public Mono<? extends TripleModel> handle(TripleModel triples, Map<String, String> parameters) {

        return this.buildIdentifierMappings(triples.getModel())
                .collect(Collectors.toSet())
                .flatMap(mappings -> replaceIdentifiers(mappings, triples.getModel()))
                .flatMap(mappings -> preserveExternalIdentifiers(mappings, triples.getModel()))
                .doOnNext(mappings -> {
                    if(mappings.size() > 0) {
                        log.debug("Replaced global identifiers in incoming model with local identifiers.");
                        mappings.forEach(mapping -> log.trace("Mapping from {} to {}", mapping.oldIdentifier().stringValue(), mapping.newIdentifier().stringValue()));
                    }
                })
                .then(Mono.just(triples))
                .doOnSubscribe(c -> log.debug("Check if model contains replaceable external identifiers."))
                .doFinally(signalType -> log.trace("Finished checks for external identifiers"));
    }



    public Flux<IdentifierMapping> buildIdentifierMappings(Model model) {

        /*
        return Flux.create(sink -> {
            Set<Value> collect = new HashSet<>();
            model.subjects().stream().filter(Value::isIRI).filter(iri -> !iri.stringValue().startsWith(Local.URN_PREFIX)).forEach(collect::add);
            model.objects().stream().filter(Value::isIRI).filter(iri -> !iri.stringValue().startsWith(Local.URN_PREFIX)).forEach(collect::add);

            collect.forEach(val -> {
                createLocalIdentifierFrom()
            });


        });*/

        return Flux.fromIterable(model.subjects())
                .filter(Value::isIRI)
                .map(val -> (IRI) val)
                .filter(iri -> !iri.stringValue().startsWith(Local.URN_PREFIX))
                .flatMap(iri ->
                    createLocalIdentifierFrom(iri, model)
                            .map(localIdentifier -> new IdentifierMapping(iri, localIdentifier))
                );



    }

    protected Mono<LocalIdentifier> createLocalIdentifierFrom(IRI iri, Model model) {
        return Mono.just(identifierFactory.createReproducibleIdentifier(Local.Entities.NS, iri));
    }







}
