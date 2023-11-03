package org.av360.maverick.graph.services.impl.values;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.identifier.ChecksumGenerator;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

public class ReadValues {


    private final ValueServicesImpl valueServices;


    public ReadValues(ValueServicesImpl valueServices) {

        this.valueServices = valueServices;
    }

    public Mono<TripleModel> listValues(String entityKey, @Nullable String prefixedPoperty, SessionContext ctx) {
        if(Objects.isNull(prefixedPoperty)) {
            return this.listAllValues(entityKey, ctx);
        } else {
            return this.listValuesForProperty(entityKey, prefixedPoperty, ctx);
        }
    }

    private Mono<TripleModel> listValuesForProperty(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                valueServices.identifierServices.asIRI(entityKey, ctx.getEnvironment())
                        .flatMap(entityIdentifier -> valueServices.entityServices.get(entityIdentifier, 0, true, ctx)),
                valueServices.schemaServices.resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfEntity entity = pair.getT1();
            IRI property = pair.getT2();
            entity.reduce(st -> st.getPredicate().equals(property));
            return entity;
        }).map(entity -> {
            new HashSet<>(entity.getModel())
                    .stream().filter(statement -> statement.getSubject().isIRI()) // we don't generate hashes for RDF* statements
                    .forEach(statement -> {
                        Triple triple = Values.triple(statement);
                        String hash = this.generateHashForValue(statement.getObject().stringValue());
                        entity.getModel().add(triple, DC.IDENTIFIER, Values.literal(hash));

                    });
            return entity;
        });
    }

    private Mono<TripleModel> listAllValues(String entityKey, SessionContext ctx) {
        return valueServices.identifierServices.asIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> valueServices.entityServices.get(entityIdentifier, 0, true, ctx))
                .map(entity -> {
                    // remove type relation and links
                    entity.reduce(statement -> statement.getObject().isLiteral());
                    return entity;
                })
                .map(entity -> {
                    new HashSet<>(entity.getModel())
                            .stream().filter(statement -> statement.getSubject().isIRI())
                            .forEach(statement -> {
                                Triple triple = Values.triple(statement);
                                String hash = this.generateHashForValue(statement.getObject().stringValue());
                                entity.getModel().add(triple, DCTERMS.IDENTIFIER, Values.literal(hash));
                            });
                    return entity;
                });
    }

    Optional<Triple> getTripleByHash(RdfEntity entity, IRI valuePredicate, String hash) {

        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(literal -> {
                    String generatedHash = this.generateHashForValue(literal.stringValue());
                    return hash.equalsIgnoreCase(generatedHash);
                })
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();
    }

    Mono<Statement> buildDetailStatementForHashedValue(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, String valueHash) {
        Optional<Triple> requestedTriple = this.getTripleByHash(entity, valuePredicate, valueHash);
        if(requestedTriple.isEmpty()) return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists for predicate <%s> and hash '%s'".formatted(valuePredicate, valueHash)));

        Statement annotationStatement = Statements.statement(requestedTriple.get(), detailPredicate, null, null);
        return Mono.just(annotationStatement);
    }

    String generateHashForValue(String value) {
        return ChecksumGenerator.generateChecksum(value, 8, 'o');
    }
}
