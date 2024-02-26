package org.av360.maverick.graph.services.preprocessors.replaceIdentifiers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.preprocessors.ModelPreprocessor;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Replaces blank nodes for individuals and classifiers with valid IRIs. Keeps blank nodes for embedded objects (but enforces reproducibility to avoid duplicates)
 * <p>
 * See also: https://www.w3.org/2011/rdf-wg/wiki/Skolemisation
 * <p>
 * <p>
 * Systems wishing to skolemise bNodes, and expose those skolem constants to external systems (e.g. in query results) SHOULD mint a "fresh" (globally unique) URI for each bNode.
 * All systems performing skolemisation SHOULD do so in a way that they can recognise the constants once skolemised, and map back to the source bNodes where possible.
 */
@Slf4j(topic = "graph.srvc.trans.ids.anon")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceAnonymousIdentifiers", havingValue = "true")
public class ReplaceAnonymousIdentifiers extends AbstractIdentifierReplace implements ModelPreprocessor {

    private SchemaServices schemaServices;
    private IdentifierServices identifierServices;

    public ReplaceAnonymousIdentifiers() {

    }

    @Override
    public int getOrder() {
        return 100;
    }




    public Mono<Model> handle(Model triples, Map<String, String> parameters, Environment environment) {
        return this.buildIdentifierMappings(triples, environment)
                .collect(Collectors.toSet())
                .flatMap(mappings -> replaceIdentifiers(mappings, triples))
                .doOnSuccess(mappings -> {
                    if (mappings.size() > 0) {
                        log.debug("Replaced anonymous identifiers in incoming model with local identifiers.");
                        mappings.forEach(mapping -> log.trace("Mapped: [{}] > [{}]", mapping.oldIdentifier().stringValue(), mapping.newIdentifier().stringValue()));
                    }
                })
                .then(Mono.just(triples))
                .doOnSubscribe(c -> log.debug("Checking if model contains replaceable anonymous identifiers"))
                .doFinally(signalType -> log.trace("Finished checks for anonymous identifiers"));
    }

    public Flux<IdentifierMapping> buildIdentifierMappings(Model model, Environment environment) {
        Set<BNode> collect = new HashSet<>();
        model.forEach(statement -> {
            if (statement.getPredicate().equals(Local.ORIGINAL_IDENTIFIER)) return;
            if (statement.getSubject().isBNode()) {
                collect.add((BNode) statement.getSubject());
            }
            if (statement.getObject().isBNode() && !model.contains(null, Local.ORIGINAL_IDENTIFIER, statement.getObject())) {
                // we must not replace identifier, which are stored as intermediate original ids
                collect.add((BNode) statement.getObject());
            }
        });



        return Flux.fromIterable(collect)
                .flatMap(val ->
                        createLocalIdentifierFrom(val, model, environment)
                                .map(localIdentifier -> new IdentifierMapping(val, localIdentifier))
                );
    }



    private Mono<Resource> createLocalIdentifierFrom(BNode subj, Model model, Environment environment) {
        if(model.contains(subj, RDF.TYPE, RDF.STATEMENT)) {
            return Mono.empty();
        }


        Set<Value> characteristicProperties = this.findCharacteristicProperties(subj, model);
        Set<Value> values = model.filter(subj, null, null).stream().map(Statement::getObject).collect(Collectors.toSet());
        Set<Value> externalTypes = model.filter(subj, RDF.TYPE, null).stream().map(Statement::getObject).filter(value -> !value.stringValue().startsWith("urn:pwid:meg")).collect(Collectors.toSet());

        Optional<Value> internalType = model
                .filter(subj, RDF.TYPE, null)
                .stream().map(Statement::getObject).filter(value -> value.stringValue().startsWith("urn:pwid:meg")).findFirst();
        if (internalType.isEmpty())
            return Mono.error(new IOException("An internal type was not set, which is a prerequisite for generating identifiers."));


        LocalIdentifier identifier;
        Collection<Serializable> serializables = new HashSet<>();

        if (internalType.get().equals(Local.Entities.TYPE_INDIVIDUAL)) {
            Validate.notEmpty(externalTypes);
            externalTypes.forEach(val -> serializables.add(val.stringValue()));
            characteristicProperties.forEach(val -> serializables.add(val.stringValue()));
        } else if (internalType.get().equals(Local.Entities.TYPE_CLASSIFIER)) {
            Validate.notEmpty(externalTypes);
            Validate.notEmpty(characteristicProperties, "The entity of type [%s] was marked as classifier, but it lacks a characteristic property.".formatted(externalTypes));
            externalTypes.forEach(val -> serializables.add(val.stringValue()));
            characteristicProperties.forEach(val -> serializables.add(val.stringValue()));
        } else if (internalType.get().equals(Local.Entities.TYPE_EMBEDDED)) {

            // for composites, use subj of parent and predicate as seed
            Optional<Statement> valueStatement = model.filter(null, null, subj).stream().findFirst();
            if(valueStatement.isEmpty()) return Mono.error(new InvalidEntityUpdate(subj, "An unreferenced blank node without type exists in the model."));
            serializables.add(valueStatement.get().getPredicate().stringValue());

            // problem, we cannot assume immutable values. But we also cannot assume uniqueness (the same predicate might have multiple nested objects for the same property)

            Validate.notEmpty(values);
            serializables.addAll(values);
        } else {
            // fallback: use the random bnode string as seed
            serializables.add(subj.getID());
        }


        // we build the identifier from entity type and value
        return identifierServices.asReproducibleLocalIRI(Local.Entities.NS, environment, serializables).map(iri -> (Resource) iri);

    }

    protected Set<Value> findCharacteristicProperties(Resource subj, Model model) {
        return model.filter(subj, null, null)
                .stream()
                .filter(statement -> this.schemaServices.isCharacteristicProperty(statement.getPredicate()))
                .map(Statement::getObject)
                .collect(Collectors.toSet());
    }


    @Override
    public void registerIdentifierService(IdentifierServices identifierServices) {
        this.identifierServices = identifierServices;
    }

    @Override
    public void registerSchemaService(SchemaServices schemaServices) {
        this.schemaServices = schemaServices;
    }

}
