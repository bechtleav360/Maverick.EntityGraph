package org.av360.maverick.graph.services.preprocessors.mergeDuplicates;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.identifier.ChecksumIdentifier;
import org.av360.maverick.graph.services.preprocessors.ModelPreprocessor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.srvc.trans.dedup")
@Component
/**
 * Checks whether duplicates exist in the incoming model. Does not check within the repository (this is delegated to a
 * scheduled job or postprocessor) 
 */
@ConditionalOnProperty(name = "application.features.transformers.mergeDuplicates", havingValue = "true")
public class MergeDuplicates implements ModelPreprocessor {


    // FIXME: should only operate on local model -> the rerouting to existing entity should happen through scheduler
    @Override
    public int getOrder() {
        return 200;
    }
    /**
     * Records with
     * - type: the type of the linked entity
     * - label: the label associated with linked entity
     * - localIdentifier: the identifier within the incoming model
     */
    private record LocalEntity(Value type, Value label, Resource localIdentifier) {
    }

    /**
     * Records with
     * - duplicate: parameters of the duplicate in current model
     * - origin: the entity which should be used instead
     */
    private record ResolvedDuplicate(LocalEntity duplicate, Value origin) {
    }


    @Override
    public Mono<? extends Model> handle(Model model, Map<String, String> parameters, Environment environment) {

        return Mono.just(model)
                .doOnSubscribe(c -> log.debug("Check if linked entities already exist and merge if required."))
                .doFinally(signalType -> log.trace("Finished checks for unique entity constraints"))
                .filter(this::checkForEmbeddedAnonymousEntities)
                .flatMap(this::mergeDuplicatedWithinModel)
                .switchIfEmpty(Mono.just(model))    // reset to model parameter if no anomyous existed
                .filter(this::checkForEmbeddedNamedEntities)
                .flatMap(this::checkIfLinkedNamedEntityExistsInGraph)
                .switchIfEmpty(Mono.just(model));
    }





    /**
     * Checks whether we have named embedded entities. The named entity is either in payload or already in graph
     *
     * @return true, if named embedded entities are in payload
     */
    private boolean checkForEmbeddedNamedEntities(Model model) {
        return referencedResourcesInModel(model).stream()
                .anyMatch(object -> object.isIRI() && (!(object instanceof ChecksumIdentifier)));
    }

    private Set<Resource> referencedResourcesInModel(Model model) {
        Set<Resource> result = new HashSet<>();
        model.unmodifiable().objects().forEach(object -> {
                    model.stream()
                            .filter(statement -> statement.getObject().equals(object))
                            .filter(statement -> !statement.getPredicate().equals(RDF.TYPE))
                            .findFirst()
                            .ifPresent(statement -> result.add(statement.getSubject()));
                }
        );
        return result;
    }

    /**
     * Checks whether we have embedded entities with anonymous identifiers (which means the embedded entity has to be within the payload)
     *
     * @return true, if anonymous embedded entities are in payload
     */
    private boolean checkForEmbeddedAnonymousEntities(Model triples) {
        return referencedResourcesInModel(triples)
                .stream()
                .anyMatch(object -> object.isBNode() || object instanceof ChecksumIdentifier);
    }


    private boolean isResourceAnonymous(Value resource) {
        return resource.isBNode() || resource instanceof ChecksumIdentifier;
    }

    /**
     * We assume that entities with a generated Id (or bnode), the same type and the same rdfs:label
     * (or even better rdfs:prefLabel) should merge to one.
     * <p>
     * Scenario: Request contains multiple entities, each with share embedded and anonymous entities
     * <p>
     * If duplicates exist in the model, they are converged (the duplicate is removed)
     *
     * @param triples
     */
    public Mono<Model> mergeDuplicatedWithinModel(Model triples) {

        Model unmodifiable = new LinkedHashModel(triples).unmodifiable();

        /*
            if ?obj <> ?anon
            and ?anon RDFS.label ?label
            and ?anon RDF.type ?type
            > 1

         */

        Set<Resource> anonymousObjects = unmodifiable.objects().stream()
                .filter(Value::isResource)
                .filter(this::isResourceAnonymous)
                .map(value -> (Resource) value)
                .collect(Collectors.toSet());


        // stores triple: identifier, type, label
        Set<Triple<Resource, Value, Value>> foundTypeAndLabel = new HashSet<>();

        int count = 0;
        for (Resource anonymous : anonymousObjects) {
            Iterator<Statement> typeStatement = unmodifiable.getStatements(anonymous, RDF.TYPE, null).iterator();
            if (!typeStatement.hasNext()) {
                log.error("Missing type definition for node with id: "+anonymous);
                continue;
            }
            Value typeValue = typeStatement.next().getObject();

            Iterator<Statement> labelStatement = unmodifiable.getStatements(anonymous, RDFS.LABEL, null).iterator();
            if (!labelStatement.hasNext()) continue;
            Value labelValue = labelStatement.next().getObject();


            Triple<Resource, Value, Value> possibleDuplicate = Triple.of(anonymous, typeValue, labelValue);
            Optional<Triple<Resource, Value, Value>> original = foundTypeAndLabel.stream()
                    .filter(triple -> triple.getMiddle().equals(possibleDuplicate.getMiddle()) && triple.getRight().equals(possibleDuplicate.getRight())).findAny();

            if (original.isPresent()) {
                log.debug("Duplicate '{}'  with shared type '{}' and label '{}' identified, removing it and rerouting all links to origin '{}' ",
                        possibleDuplicate.getLeft(), possibleDuplicate.getMiddle().stringValue(), possibleDuplicate.getRight().stringValue(),
                        original.get().getLeft());
                this.reroute(triples, possibleDuplicate.getLeft(), original.get().getLeft());
                count++;
            } else {
                foundTypeAndLabel.add(possibleDuplicate);
            }

        }
        if(count == 0 && log.isTraceEnabled()) {
            log.trace("{} anonymous embedded entities merged in model with {} statements", count, triples.size());
        } else {
            log.debug("{} anonymous embedded entities merged in model with {} statements", count, triples.size());
        }


        return Mono.just(triples);

    }

    public void reroute(Model triples, Resource duplicateIdentifier, Resource originalIdentifier) {

        Model unmodifiable = new LinkedHashModel(triples).unmodifiable();

        // remove all statement from the possibleDuplicate (since we keep the original)
        unmodifiable.getStatements(duplicateIdentifier, null, null).forEach(triples::remove);

        // change link to from possibleDuplicate to original
        unmodifiable.getStatements(null, null, duplicateIdentifier).forEach(statement -> {
            triples.remove(statement);
            triples.add(statement.getSubject(), statement.getPredicate(), originalIdentifier);
        });

        if (log.isTraceEnabled())
            log.trace("{} statements in the model after rerouting", triples.size());

    }




    /**
     * Scenario: Request contains embedded entity
     *
     * @param triples
     */
    public Mono<Model> checkIfLinkedNamedEntityExistsInGraph(Model triples) {
        log.trace("Checking for duplicates in graph skipped");

        return Mono.just(triples);
    }


}
