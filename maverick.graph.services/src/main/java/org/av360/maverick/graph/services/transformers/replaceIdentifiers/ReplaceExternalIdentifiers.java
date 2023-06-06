package org.av360.maverick.graph.services.transformers.replaceIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.transformers.Transformer;
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
@Slf4j(topic = "graph.srvc.trans.ids.ext")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceGlobalIdentifiers", havingValue = "true")
public class ReplaceExternalIdentifiers extends AbstractIdentifierReplace implements Transformer {

    protected final IdentifierServices identifierServices;


    public ReplaceExternalIdentifiers(IdentifierServices identifierServices) {
        this.identifierServices = identifierServices;
    }


    @Override
    public Mono<? extends Model> handle(Model triples, Map<String, String> parameters) {

        return this.buildIdentifierMappings(triples)
                .collect(Collectors.toSet())
                .flatMap(mappings -> replaceIdentifiers(mappings, triples))
                .flatMap(mappings -> preserveExternalIdentifiers(mappings, triples))
                .doOnNext(mappings -> {
                    if (mappings.size() > 0) {
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
                        identifierServices.asReproducibleIRI(Local.Entities.NAMESPACE, iri)
                                .map(generated -> new IdentifierMapping(iri, generated))
                );


    }


    public IdentifierServices getIdentifierService() {
        return this.identifierServices;
    }
}
