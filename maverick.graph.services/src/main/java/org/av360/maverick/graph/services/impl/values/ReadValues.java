package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.identifier.ChecksumGenerator;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j(topic = "graph.svc.value.read")
public class ReadValues {


    private final ValueServicesImpl valueServices;


    public ReadValues(ValueServicesImpl valueServices) {

        this.valueServices = valueServices;
    }

    public Mono<TripleModel> listValues(String entityKey, @Nullable String prefixedPoperty, SessionContext ctx) {
        return valueServices.identifierServices.asIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> valueServices.entityServices.get(entityIdentifier, 0, true, ctx))
                .flatMap(entity -> this.listValues(entity, prefixedPoperty));
    }

    public Mono<TripleModel> listValues(RdfEntity entity, @Nullable String prefixedPoperty) {
        if(Objects.isNull(prefixedPoperty)) {
            return this.listAllValues(entity);
        } else {
            return this.listValuesForProperty(entity, prefixedPoperty);
        }
    }

    Mono<TripleModel> listValuesForProperty(RdfEntity entity, String prefixedValuePredicate) {
        return this.valueServices.schemaServices.resolvePrefixedName(prefixedValuePredicate)
                .flatMap(valuePredicate -> this.listValuesForProperty(entity, valuePredicate));
    }

    Mono<TripleModel> listValuesForProperty(RdfEntity entity, IRI valuePredicate) {
        entity.reduce(st -> st.getPredicate().equals(valuePredicate));

        new HashSet<>(entity.getModel())
                .stream().filter(statement -> statement.getSubject().isIRI()) // we don't generate hashes for RDF* statements
                .forEach(statement -> {
                    Triple triple = Values.triple(statement);
                    String hash = this.generateHashForValue(statement.getObject().stringValue());
                    entity.getModel().add(triple, DC.IDENTIFIER, Values.literal(hash));
                });
        return Mono.just(entity);
    }

    private Mono<TripleModel> listAllValues(RdfEntity entity) {
        entity.reduce(statement -> statement.getObject().isLiteral());

        new HashSet<>(entity.getModel())
                .stream().filter(statement -> statement.getSubject().isIRI())
                .forEach(statement -> {
                    Triple triple = Values.triple(statement);
                    String hash = this.generateHashForValue(statement.getObject().stringValue());
                    entity.getModel().add(triple, DCTERMS.IDENTIFIER, Values.literal(hash));
                });
        return Mono.just(entity);
    }


    Optional<Triple> findValueTripleByLanguageTag(RdfEntity entity, IRI valuePredicate, String languageTag) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .filter(literal -> ((Literal) literal).getLanguage().map(tag -> tag.equalsIgnoreCase(languageTag)).orElseGet(() -> Boolean.FALSE))
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();

    }


    Optional<Triple> findValueTripleByHash(RdfEntity entity, IRI valuePredicate, String hash) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(literal -> {
                    String generatedHash = this.generateHashForValue(literal.stringValue());
                    return hash.equalsIgnoreCase(generatedHash);
                })
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();
    }

    Optional<Triple> findSingleValueTriple(RdfEntity entity, IRI valuePredicate) throws InvalidEntityModelException {
        List<Triple> list = entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .toList();
        if(list.size() > 1) throw new InvalidEntityModelException("Multiple value for entity with key '%s' and value '%s' found.".formatted(entity.getIdentifier(), valuePredicate));
        else if(list.size() == 0) return Optional.empty();
        else return Optional.of(list.get(0));
    }



    String generateHashForValue(String value) {
        return ChecksumGenerator.generateChecksum(value, 8, 'o');
    }
}
