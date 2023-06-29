package org.av360.maverick.graph.services.transformers.types;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Slf4j(topic = "graph.srvc.trans.types")
@Component
@ConditionalOnProperty(name = "application.features.transformers.typeCoercion", havingValue = "true")
public class AssignLocalTypes implements Transformer {


    private SchemaServices schemaServices;

    static ValueFactory valueFactory = SimpleValueFactory.getInstance();


    @Override
    public void registerSchemaService(SchemaServices schemaServices) {
        this.schemaServices = schemaServices;

    }

    @Override
    public Mono<? extends Model> handle(Model model, Map<String, String> parameters, Environment environment) {
        Model result = new LinkedHashModel(model);

        return Flux.fromIterable(Collections.unmodifiableSet(model.subjects()))
                .filter(sub -> assignIndividuals(sub, model, result))
                .filter(sub -> assignShared(sub, model, result))
                .filter(sub -> assignEmbedded(sub, model, result))
                .doOnNext(sub -> {
                    log.warn("Subject with the following statements could not be identified for local type: \n {}", model.stream().toList());
                })
                .then(Mono.just(result));
                //.doOnSubscribe(c -> log.trace("Checking if internal types have to be added."))
                //.doFinally(signalType -> log.trace("Finished checks for internal types."));
    }



    private boolean assignEmbedded(Resource subject, Model fragment, Model model) {
        Optional<Statement> statement = this.handleEmbedded(subject, fragment);
        return statement.map(model::add).isEmpty();
    }


    private boolean assignIndividuals(Resource subject, Model source, Model model) {
        Optional<Statement> statement = this.handleIndividual(subject, source);
        return statement.map(model::add).isEmpty();
    }

    private boolean assignShared(Resource subject, Model fragment, Model model) {
        Optional<Statement> statement = this.handleClassifier(subject, fragment);
        return statement.map(model::add).isEmpty();
    }

    private Optional<Statement> handleEmbedded(Resource subject, Model fragment) {

        // only check if it has a type definition
        boolean hasTypeDefinition = hasTypeDefinition(subject, fragment);

        if (hasTypeDefinition) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.TYPE_EMBEDDED);
            log.trace("Fragment for subject '{}' typed as Embedded.", subject);
            return Optional.of(statement);
        } else return Optional.empty();

    }



    private Optional<Statement> handleIndividual(Resource subject, Model fragment) {

        // Check 1: check if this fragment has a type definition known to be an individual
        boolean hasIndividualsType = hasIndividualsType(subject, fragment);

        // Check 2: check if this fragment has at least one known characteristic y (but is not a classifier)
        boolean hasCharacteristicProperty = hasIndividualsType || (hasCharacteristicProperty(subject, fragment) && ! hasClassifierType(subject, fragment));

        // Check three: check if this fragment has a property matching a specific pattern (denoting a characteristic property)
        boolean hasNamingProperty = (hasIndividualsType || hasCharacteristicProperty) || (hasPropertyMatchingPattern(fragment) && ! hasClassifierType(subject, fragment));

        if (hasIndividualsType || hasCharacteristicProperty || hasNamingProperty) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.TYPE_INDIVIDUAL);
            log.trace("Fragment for subject '{}' typed as Individual.", subject);
            return Optional.of(statement);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Statement> handleClassifier(Resource subject, Model fragment) {
        // Check one: check if this fragment has a type definition known to be a classifier
        boolean hasClassifierType = this.hasClassifierType(subject, fragment);

        if (hasClassifierType) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.TYPE_CLASSIFIER);
            log.trace("Fragment for subject {} typed as Classifier.", subject);
            return Optional.of(statement);
        } else return Optional.empty();
    }

    private boolean hasPropertyMatchingPattern(Model fragment) {
        return fragment.predicates().stream().anyMatch(iri -> iri.getLocalName().matches("(?i).*(name|title|label|id|key|code).*"));
    }

    private boolean hasClassifierType(Resource subject, Model fragment) {
        return StreamSupport.stream(fragment.getStatements(subject, RDF.TYPE, null).spliterator(), true)
                .map(Statement::getObject)
                .filter(Value::isIRI)
                .map(value -> (IRI) value)
                .anyMatch(this.schemaServices::isClassifierType);
    }

    private boolean hasTypeDefinition(Resource subject, Model fragment) {
        return StreamSupport.stream(fragment.getStatements(subject, RDF.TYPE, null).spliterator(), true)
                .map(Statement::getObject)
                .filter(Value::isIRI)
                .map(value -> (IRI) value)
                .findFirst()
                .isPresent();
    }

    private boolean hasIndividualsType(Resource subject, Model fragment) {
        return StreamSupport.stream(fragment.getStatements(subject, RDF.TYPE, null).spliterator(), true)
                .map(Statement::getObject)
                .filter(Value::isIRI)
                .map(value -> (IRI) value)
                .anyMatch(this.schemaServices::isIndividualType);
    }

    private boolean hasCharacteristicProperty(Resource subject, Model fragment) {
        return StreamSupport.stream(fragment.getStatements(subject, null, null).spliterator(), true)
                .map(Statement::getPredicate)
                .filter(Value::isIRI)
                .anyMatch(this.schemaServices::isCharacteristicProperty);
    }





}
