package cougar.graph.services.transformers.mergeDuplicates;

import cougar.graph.model.rdf.GeneratedIdentifier;
import cougar.graph.model.errors.MissingType;
import cougar.graph.store.rdf.models.AbstractModel;
import cougar.graph.services.services.QueryServices;
import cougar.graph.services.services.handler.Transformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.transformer.deduplication")
@Component
/**
 * Checks whether duplicates exist in the incoming model. Does not check within the repository (this is delegated to a
 * scheduled job)
 */
@ConditionalOnProperty(name = "application.features.transformers.mergeDuplicates", havingValue = "true")
public class MergeDuplicates implements Transformer {

    private QueryServices queryServices;

    @Override
    public void registerQueryService(QueryServices queryServices) {
        this.queryServices = queryServices;

    }

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
    public Mono<? extends AbstractModel> handle(AbstractModel model, Map<String, String> parameters, Authentication authentication) {

        return Mono.just(model)
                .doOnSubscribe(c -> log.debug("Check if linked entities already exist and merge if required."))
                .doFinally(signalType -> log.trace("Finished checks for unique entity constraints"))
                .filter(this::checkForEmbeddedAnonymousEntities)
                .flatMap(this::mergeDuplicatedWithinModel)
                .flatMap(triples -> mergeDuplicatesInEntityGraph(triples, authentication))
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
    private boolean checkForEmbeddedNamedEntities(AbstractModel triples) {
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
        log.trace("Merging duplicates within the model with {} statements", triples.streamStatements().count());
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

        int count = 0;
        for (Resource anonymous : anonymousObjects) {
            Iterator<Statement> typeStatement = unmodifiable.getStatements(anonymous, RDF.TYPE, null).iterator();
            if (!typeStatement.hasNext()) throw new MissingType(anonymous);
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
        log.trace("{} anonymous embedded entities merged", count);
        return Mono.just(triples);

    }

    public void reroute(AbstractModel triples, Resource duplicateIdentifier, Resource originalIdentifier) {

        Model unmodifiable = new LinkedHashModel(triples.getModel()).unmodifiable();

        // remove all statement from the possibleDuplicate (since we keep the original)
        unmodifiable.getStatements(duplicateIdentifier, null, null).forEach(statement -> triples.getModel().remove(statement));

        // change link to from possibleDuplicate to original
        unmodifiable.getStatements(null, null, duplicateIdentifier).forEach(statement -> {
            triples.getModel().remove(statement);
            triples.getModel().add(statement.getSubject(), statement.getPredicate(), originalIdentifier);
        });

        if (log.isTraceEnabled()) log.trace("{} statements in the model after rerouting", triples.streamStatements().count());

    }

    /**
     * Scenario: Request contains embedded entity, which already exists in Graph
     * <p>
     * Two entities are duplicates, if they have equal Type (RDF) and Label (RDFS). If the incoming model has a definition,
     * which already exists in the graph, it will be removed (and the edge rerouted to the entity in the graph)
     *
     * @param model, the current model
     */
    public Mono<AbstractModel> mergeDuplicatesInEntityGraph(AbstractModel model, Authentication authentication) {
        return Mono.just(model)
                .doOnSuccess(subscription -> log.trace("Checked if anonymous embedded entities already exist in graph."))
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
                    return Mono.zip(Mono.just(localEntity), queryServices.queryValues(all, authentication).collectList());
                })
                .doOnNext(pair -> {
                    // if we found query results, relink local entity and remove duplicate from model
                    LocalEntity localEntity = pair.getT1();
                    List<BindingSet> queryResult = pair.getT2();
                    if (queryResult.size() > 1) {
                        log.debug("Linked entity exists already in graph, merging with existing item in graph.");
                        this.reroute(model, localEntity.localIdentifier(), (Resource) queryResult.get(0).getValue("id"));
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
     * @param triples
     */
    public Mono<AbstractModel> checkIfLinkedNamedEntityExistsInGraph(AbstractModel triples) {
        log.trace("(Transformer) Checking for duplicates in graph skipped");

        return Mono.just(triples);
    }


}
