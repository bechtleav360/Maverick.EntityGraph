package com.bechtle.eagl.graph.domain.services.handler.transformers;

import com.bechtle.eagl.graph.domain.model.errors.MissingType;
import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
/**
 * Checks whether duplicates exist in the incoming model. Does not check within the repository (this is delegated to a
 * scheduled job)
 */
@ConditionalOnProperty(name = "application.features.transformers.mergeDuplicates", havingValue = "true")
public class MergeDuplicates implements Transformer {

    // FIXME: should only operate on local model -> the rerouting to existing entity should happen through scheduler

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
    public Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel model, Map<String, String> parameters) {

        return Mono.just(model)
                .doOnSubscribe(c -> log.debug("(Transformer/Unique) Check if linked entities already exist and reroute if needed"))
                .doFinally(signalType -> log.trace("(Transformer/Unique) Finished checks for unique entity constraints"))
                .filter(this::checkForEmbeddedAnonymousEntities)
                .flatMap(this::mergeDuplicatedWithinModel)
                .flatMap(triples -> mergeDuplicatesInEntityGraph(graph, triples))
                .switchIfEmpty(Mono.just(model))    // reset to model parameter if no anomyous existed
                .filter(this::checkForEmbeddedNamedEntities)
                .flatMap(triples -> checkIfLinkedNamedEntityExistsInGraph(graph, triples))
                .switchIfEmpty(Mono.just(model));
    }

    /**
     * Checks whether we have named embedded entities. The named entity is either in payload or already in graph
     *
     * @return true, if named embedded entities are in payload
     */
    private boolean     checkForEmbeddedNamedEntities(AbstractModel triples) {
        return triples.embeddedObjects()
                .stream()
                .anyMatch(object -> object.isIRI() && (!(object instanceof GeneratedIdentifier)));
    }

    /**
     * Checks whether we have embedded entities with anonymous identifiers (which means the embedded entity has to be within the payload)
     *
     * @return true, if anonymous embedded entities are in payload
     */
    private boolean checkForEmbeddedAnonymousEntities(AbstractModel triples) {
        return triples.embeddedObjects()
                .stream()
                .anyMatch(object -> object.isBNode() || object instanceof GeneratedIdentifier);


    }


    private boolean isResourceAnonymous(Value resource) {
        return resource.isBNode() || resource instanceof GeneratedIdentifier;
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
    public Mono<AbstractModel> mergeDuplicatedWithinModel(AbstractModel triples) {
        log.trace("(Transformer) Merging duplicates within the model");
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
            if (!typeStatement.hasNext()) throw new MissingType(anonymous);

            Iterator<Statement> labelStatement = unmodifiable.getStatements(anonymous, RDFS.LABEL, null).iterator();
            if (!labelStatement.hasNext()) continue;

            Triple<Resource, Value, Value> possibleDuplicate = Triple.of(anonymous, typeStatement.next().getObject(), labelStatement.next().getObject());
            Optional<Triple<Resource, Value, Value>> original = foundTypeAndLabel.stream()
                    .filter(triple -> triple.getMiddle().equals(possibleDuplicate.getMiddle()) && triple.getRight().equals(possibleDuplicate.getRight())).findAny();

            if (original.isPresent()) {
                this.reroute(triples, possibleDuplicate.getLeft(), original.get().getLeft());
            } else {
                foundTypeAndLabel.add(possibleDuplicate);
            }

        }
        log.trace("(Transformer/Unique) Anonymous embedded entities merged");
        return Mono.just(triples);

    }

    public void reroute(AbstractModel triples, Resource duplicateIdentifier, Resource originalIdentifier) {
        log.debug("(Transformer/Unique) Duplicate '{}' identified, removing it and rerouting all links to origin '{}' ", duplicateIdentifier, originalIdentifier);
        Model unmodifiable = new LinkedHashModel(triples.getModel()).unmodifiable();

        // remove all statement from the possibleDuplicate (since we keep the original)
        unmodifiable.getStatements(duplicateIdentifier, null, null).forEach(statement -> triples.getModel().remove(statement));

        // change link to from possibleDuplicate to original
        unmodifiable.getStatements(null, null, duplicateIdentifier).forEach(statement -> {
            triples.getModel().remove(statement);
            triples.getModel().add(statement.getSubject(), statement.getPredicate(), originalIdentifier);
        });

    }

    /**
     * Scenario: Request contains embedded entity, which already exists in Graph
     * <p>
     * Two entities are duplicates, if they have equal Type (RDF) and Label (RDFS). If the incoming model has a definition,
     * which already exists in the graph, it will be removed (and the edge rerouted to the entity in the graph)
     *
     * @param graph, connection to the entity graph
     * @param model, the current model
     */
    public Mono<AbstractModel> mergeDuplicatesInEntityGraph(EntityStore graph, AbstractModel model) {
        return Mono.just(model)
                .doOnSubscribe(subscription -> log.trace("(Transformer/Unique) Check if anonymous embedded entities already exist in graph."))
                .flatMapMany(triples -> {
                    // collect types and labels of embedded objects
                    return Flux.<LocalEntity>create(c -> {
                        triples.embeddedObjects().forEach(resource -> {

                            Value type = triples.streamStatements(resource, RDF.TYPE, null)
                                    .findFirst()
                                    .orElseThrow(() -> new MissingType(resource)).getObject();
                            triples.streamStatements(resource, RDFS.LABEL, null)
                                    .findFirst()
                                    .ifPresent(statement -> c.next(new LocalEntity(type, statement.getObject(), resource)));
                        });
                        c.complete();
                    });
                })
                .flatMap(localEntity -> {
                    // build sparql query to check, if an entity with this type and label exists already
                    Variable id = SparqlBuilder.var("id");
                    RdfObject type = Rdf.object(localEntity.type());
                    RdfObject label = Rdf.object(localEntity.label());
                    SelectQuery all = Queries.SELECT(id).where(id.isA(type).andHas(RDFS.LABEL, label)).all();
                    return Mono.zip(Mono.just(localEntity), graph.select(all));
                })
                .doOnNext(pair -> {
                    // if we found query results, relink local entity and remove duplicate from model
                    LocalEntity localEntity = pair.getT1();
                    TupleQueryResult queryResult = pair.getT2();
                    if (queryResult.hasNext()) {
                        log.trace("(Transformer) Linked entity exists already in graph, rerouting.");
                        this.reroute(model, localEntity.localIdentifier(), (Resource) queryResult.next().getValue("id"));
                    }
                })
                .then(Mono.just(model));





        /*  Find embedded entities in model
            SELECT * WHERE
                ?entity ?pred ?linkedEntity .
                ?linkedEntity a ?type ;
                       RDFS.label ?label

         */

        // for each embedded object

    }


    /**
     * Scenario: Request contains embedded entity
     *
     * @param graph
     * @param triples
     */
    public Mono<AbstractModel> checkIfLinkedNamedEntityExistsInGraph(EntityStore graph, AbstractModel triples) {
        log.trace("(Transformer) Checking for duplicates in graph skipped");

        return Mono.just(triples);
    }


}
