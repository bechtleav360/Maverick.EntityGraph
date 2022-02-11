package com.bechtle.eagl.graph.domain.services.handler.transformers;

import com.bechtle.eagl.graph.domain.model.errors.MissingType;
import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.domain.services.handler.AbstractTypeHandler;
import com.bechtle.eagl.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class UniqueEntityHandler extends AbstractTypeHandler {

    @Override
    public Mono<? extends AbstractModel> handle(EntityStore graph, Mono<? extends AbstractModel> model, Map<String, String> parameters) {
        return model.map(triples -> {
            log.debug("(Transformer) Check if linked entities already exist and reroute if needed");

            if (this.checkForEmbeddedAnonymousEntities(triples)) {
                this.checkByLabelAndType(triples);
                this.checkIfLinkedEntityExistsInGraph(graph, triples);
            }

            if (this.checkForEmbeddedNamedEntities(triples)) {
                this.checkIfLinkedNamedEntityExistsInGraph(graph, triples);
            }


            return triples;
        });

    }

    /**
     * Checks whether we have named embedded entities. The named entity is either in payload or already in graph
     *
     * @return true, if named embedded entities are in payload
     */
    private boolean checkForEmbeddedNamedEntities(AbstractModel triples) {
        return triples.getModel().stream()
                .filter(statement -> {
                    boolean objectIsNamed = (statement.getObject().isIRI()) && (!(statement.getObject() instanceof GeneratedIdentifier));
                    boolean objectIsNotType = statement.getPredicate() != RDF.TYPE;
                    return objectIsNamed && objectIsNotType;
                })
                .peek(object -> log.trace("Found linked named entity: {}", object)).findAny().isPresent();
    }

    /**
     * Checks whether we have embedded entities with anonymous identifiers (which means the embedded entity has to be within the payload)
     *
     * @return true, if anonymous embedded entities are in payload
     */
    private boolean checkForEmbeddedAnonymousEntities(AbstractModel triples) {

        return triples.getModel().stream()
                .anyMatch(statement -> {
                    return statement.getObject().isBNode() || statement.getObject() instanceof GeneratedIdentifier;
                });

    }


    private boolean isResourceAnonymous(Value resource) {
        return resource.isBNode() || resource instanceof GeneratedIdentifier;
    }

    /**
     * We assume that entities with a generated Id (or bnode), the same type and the same rdfs:label
     * (or even better rdfs:prefLabel) should merge to one.
     * <p>
     * Scenario: Request contains multiple entities, each with share embedded and anonymous entities
     *
     * @param triples
     */
    public void checkByLabelAndType(AbstractModel triples) {
        log.trace("(Transformer) Anonymous embedded entities detected, checking for redundant definitions");
        Model unmodifiable = new LinkedHashModel(triples.getModel()).unmodifiable();

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

        for (Resource anonymous : anonymousObjects) {
            Iterator<Statement> typeStatement = unmodifiable.getStatements(anonymous, RDF.TYPE, null).iterator();
            if (! typeStatement.hasNext()) throw new MissingType(anonymous);

            Iterator<Statement> labelStatement = unmodifiable.getStatements(anonymous, RDFS.LABEL, null).iterator();
            if (! labelStatement.hasNext()) continue;

            Triple<Resource, Value, Value> current = Triple.of(anonymous, typeStatement.next().getObject(), labelStatement.next().getObject());
            Optional<Triple<Resource, Value, Value>> duplicate = foundTypeAndLabel.stream()
                    .filter(triple -> triple.getMiddle().equals(current.getMiddle()) && triple.getRight().equals(current.getRight())).findAny();

            if(duplicate.isPresent()) {
                log.debug("Duplicate identified, rerouting the entity '{}' with type {} and label {}", current.getLeft(), current.getMiddle(), current.getRight());

                // remove all statement from the current (since we keep the duplicate)
                unmodifiable.getStatements(current.getLeft(), null, null).forEach(statement -> triples.getModel().remove(statement));

                // change link to from current to duplicate
                unmodifiable.getStatements(null, null, current.getLeft()).forEach(statement -> {
                    triples.getModel().remove(statement);
                    triples.getModel().add(statement.getSubject(), statement.getPredicate(), duplicate.get().getLeft());
                });
            } else {
                foundTypeAndLabel.add(current);

            }

        }
        log.trace("(Transformer) Anonymous embedded entities merged");

    }

    /**
     * Scenario: Request contains embedded entity
     *
     * @param graph
     * @param model
     */
    public void checkIfLinkedEntityExistsInGraph(EntityStore graph, AbstractModel model) {
        log.trace("(Transformer) Handle anonymous embedded entity, check if it supposed to be unique and already exists in graph.");
    }


    /**
     * Scenario: Request contains embedded entity
     *
     * @param graph
     * @param model
     */
    public void checkIfLinkedNamedEntityExistsInGraph(EntityStore graph, AbstractModel model) {
        log.trace("(Transformer) Handle linked named entity, check if is supposed to be unique and already exists in graph.");
    }

}
