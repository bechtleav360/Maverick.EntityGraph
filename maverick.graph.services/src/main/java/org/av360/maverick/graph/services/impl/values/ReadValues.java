package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.vocabulary.Details;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public Mono<Triples> listValues(String entityKey, @Nullable String prefixedValuePredicate, SessionContext ctx) {
        return valueServices.identifierServices.asLocalIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> valueServices.entityServices.get(entityIdentifier, true, 0, ctx))
                .flatMap(entity -> {
                    if(Objects.isNull(prefixedValuePredicate)) {
                        entity.reduce(statement -> statement.getObject().isLiteral());
                        return this.insertValueIdentifiers(entity);
                    } else {
                        return this.valueServices.schemaServices.resolvePrefixedName(prefixedValuePredicate)
                                .map(valuePredicate -> entity.filter(st -> st.getPredicate().equals(valuePredicate)))
                                .flatMap(triples -> this.insertValueIdentifiers(triples));
                    }
                });
    }






    private Mono<Triples> insertValueIdentifiers(Triples entity) {
        new HashSet<>(entity.getModel())
                .stream().filter(statement -> statement.getSubject().isIRI())
                .forEach(statement -> {
                    Triple triple = Values.triple(statement);
                    String hash = this.generateHashForValue(statement.getPredicate().stringValue(), statement.getObject().stringValue());
                    entity.getModel().add(triple, Details.HASH, Values.literal(hash));
                });
        return Mono.just(entity);
    }




    Optional<Triple> findValueTripleByLanguageTag(RdfFragment entity, IRI valuePredicate, String languageTag) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .filter(literal -> ((Literal) literal).getLanguage().map(tag -> tag.equalsIgnoreCase(languageTag)).orElseGet(() -> Boolean.FALSE))
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();

    }


    Optional<Triple> findValueTripleByHash(RdfFragment entity, IRI valuePredicate, String hash) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(literal -> {
                    String generatedHash = this.generateHashForValue(valuePredicate.stringValue(), literal.stringValue());
                    return hash.equalsIgnoreCase(generatedHash);
                })
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();
    }

    Optional<Triple> findSingleValueTriple(RdfFragment entity, IRI valuePredicate) throws InvalidEntityModelException {
        List<Triple> list = entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .toList();
        if(list.size() > 1) throw new InvalidEntityModelException("Multiple value for entity with key '%s' and value '%s' found.".formatted(entity.getIdentifier(), valuePredicate));
        else if(list.size() == 0) return Optional.empty();
        else return Optional.of(list.get(0));
    }



    String generateHashForValue(String predicate, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((predicate+value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }



            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


}
