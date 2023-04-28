package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceAnonymousIdentifiers;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Checks for subjects which don't conform to our internal identifier schema, e.g
 * - _:fefa62849b5844b4a2dff3f2747619632 for anonymous nodes
 * - "//example.com/data/sds" for external identifiers
 *
 * <p>
 * This job will replace these identifiers with internal urns both in subjects in objects
 * <p>
 * Example:
 * <pre>
 * _:anon1 a ns1:LearningResource,
 *      ns1:hasCategoryCode [ a ns1:CategoryCode ;
 *      ns1:codeValue "27" ]
 * </pre>
 * is transformed to
 * <pre>
 *  urn:pwid:meg:e:2121312 a ns1:LearningResource
 *      urn:int:srcid _:anon1
 *      ns1:hasCategoryCode []
 *
 *  urn:pwid:meg:e:6423412 a ns1:DefinedTerm
 *      urn:int:srcid _:anon2
 *      ns1:codeValue "27"
 * </pre>
 *
 */

@Slf4j(topic = "graph.jobs.identifiers")
@Component
public class ReplaceExternalIdentifiersJob implements Job {

    public static String NAME = "replaceIdentifiers";

    private final QueryServices queryServices;

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;


    private final ReplaceExternalIdentifiers replaceExternalIdentifiers;
    private final ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers;



    private record StatementsBag(
            Resource candidateIdentifier,
            Set<Statement> removableStatements,
            Model convertedStatements,
            @Nullable Statement originalIdentifierStatement,
            RdfTransaction transaction
    ) {
    }

    public ReplaceExternalIdentifiersJob(QueryServices queryServices, EntityStore store, TransactionsStore trxStore, ReplaceExternalIdentifiers transformer, ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers) {
        this.queryServices = queryServices;
        this.entityStore = store;
        this.trxStore = trxStore;
        this.replaceExternalIdentifiers = transformer;
        this.replaceAnonymousIdentifiers = replaceAnonymousIdentifiers;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(Authentication authentication) {
        if (Objects.isNull(this.replaceExternalIdentifiers))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForExternalSubjectIdentifiers(authentication).then();

    }


    /**
     * First run: replace only subject identifiers, move old identifier in temporary property
     */
    public Flux<RdfTransaction> checkForExternalSubjectIdentifiers(Authentication authentication) {
        return findSubjectCandidates(authentication)
                .flatMap(value -> this.loadSubjectStatements(value, authentication))
                .flatMap(this::convertSubjectStatements)
                .flatMap(this::insertStatements)
                .flatMap(this::deleteStatements)
                .buffer(50)
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction))
                .buffer(50)
                .flatMap(transactions -> this.storeTransactions(transactions, authentication))
                .doOnError(throwable -> log.error("Exception while finding and replacing subject identifiers: {}", throwable.getMessage()))
                .doOnSubscribe(sub -> log.trace("Checking for external or anonymous subject identifiers."))
                .doOnComplete(() -> log.debug("Completed checking for external or anonymous identifiers in subjects."));
    }



    private Flux<Resource> findSubjectCandidates(Authentication authentication) {
        String tpl = """
                SELECT DISTINCT ?a WHERE {
                  ?a a ?c .
                  FILTER NOT EXISTS {
                    FILTER STRSTARTS(str(?a), "%s").
                    }
                  }
                """;
        String query = String.format(tpl, Local.Entities.NAMESPACE);
        return queryServices.queryValues(query, authentication)
                .map(bindings -> bindings.getValue("a"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }



    private Flux<RdfTransaction> commit(List<RdfTransaction> transactions, Authentication authentication) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(transactions, authentication);
    }

    private Flux<RdfTransaction> storeTransactions(Collection<RdfTransaction> transactions, Authentication authentication) {
        //FIXME: through event
        return this.trxStore.store(transactions, authentication);
    }

    private Mono<RdfTransaction> deleteStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.removableStatements());
        return entityStore.removeStatements(statements, statementsBag.transaction());
    }

    private Mono<StatementsBag> insertStatements(StatementsBag bag) {
        return this.entityStore.insert(bag.convertedStatements(), bag.transaction()).then(Mono.just(bag));
    }


    private Mono<StatementsBag> convertSubjectStatements(StatementsBag bag) {
        LinkedHashModel model = new LinkedHashModel();
        model.addAll(bag.removableStatements());

        return Mono.zip(
                this.replaceExternalIdentifiers.buildIdentifierMappings(model).collectList(),
                this.replaceAnonymousIdentifiers.buildIdentifierMappings(model).collectList()
        ).map(pair -> {
            Map<Resource, IRI> map = new HashMap<>();
            pair.getT1().forEach(mapping -> map.put(mapping.oldIdentifier(), mapping.newIdentifier()));
            pair.getT2().forEach(mapping -> map.put(mapping.oldIdentifier(), mapping.newIdentifier()));
            return map;
        }).map(identifierMap -> {
            ModelBuilder modelBuilder = new ModelBuilder();
            bag.removableStatements.forEach(statement -> {
                if (identifierMap.containsKey(statement.getSubject())) {
                    modelBuilder.add(identifierMap.get(statement.getSubject()), statement.getPredicate(), statement.getObject());
                    modelBuilder.add(identifierMap.get(statement.getSubject()), Local.ORIGINAL_IDENTIFIER, statement.getSubject());
                } else {
                    modelBuilder.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
                }
            });
            bag.convertedStatements().addAll(modelBuilder.build());
            return bag;
        }).doOnSubscribe(sub -> log.trace("Converting subject removableStatements for resource '{}' with {} removableStatements", bag.candidateIdentifier(), bag.removableStatements().size()));

    }

    public Mono<StatementsBag> loadSubjectStatements(Resource candidate, Authentication authentication) {
        return this.entityStore.listStatements(candidate, null, null, authentication)
                .map(statements -> new StatementsBag(candidate, Collections.synchronizedSet(statements), new LinkedHashModel(), null, new RdfTransaction()));
    }



}


