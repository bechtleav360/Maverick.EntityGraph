package org.av360.maverick.graph.feature.jobs.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceAnonymousIdentifiers;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Checks for subjects which don't conform to our internal identifier schema, e.g
 * - _:fefa62849b5844b4a2dff3f2747619632 for anonymous nodes
 * - http://example.com/data/sds for external identifiers
 * <p>
 * This job will replace these identifiers with internal urns both in subjects in objects
 * <p>
 * Example:
 * <p>
 * [] a ns1:LearningResource,
 * ns1:hasCategoryCode [ a ns1:CategoryCode ;
 * ns1:codeValue "27" ]
 * <p>
 * is transformed to
 * <p>
 * urn:pwid:meg:e:2121312 a ns1:LearningResource
 * ns1:hasCategoryCode
 * <p>
 * urn:pwid:meg:e:6423412 a ns1:DefinedTerm
 * ns1:termCode "23"
 */

@Slf4j(topic = "graph.jobs.identifiers")
@Component
@Deprecated
public class ReplaceExternalIdentifiersServiceV2 {

    private final QueryServices queryServices;

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;

    private final IdentifierFactory identifierFactory;

    private final ReplaceExternalIdentifiers replaceExternalIdentifiers;
    private final ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers;


    private record StatementsBag(
            Resource candidateIdentifier,
            Set<Statement> subjectStatements,

            Set<Statement> objectStatements,
            Model convertedStatements,
            Transaction transaction
            ) {
    }

    public ReplaceExternalIdentifiersServiceV2(QueryServices queryServices, EntityStore store, TransactionsStore trxStore, IdentifierFactory identifierFactory, ReplaceExternalIdentifiers transformer, ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers) {
        this.queryServices = queryServices;
        this.entityStore = store;
        this.trxStore = trxStore;
        this.identifierFactory = identifierFactory;
        this.replaceExternalIdentifiers = transformer;
        this.replaceAnonymousIdentifiers = replaceAnonymousIdentifiers;
    }

    private boolean labelCheckRunning = false;

    public boolean isRunning() {
        return labelCheckRunning;
    }


    public Mono<Void> run(Authentication authentication) {
        if (Objects.isNull(this.replaceExternalIdentifiers))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForExternalIdentifiers(authentication)
                .then();

    }


    public Flux<Transaction> checkForExternalIdentifiers(Authentication authentication) {
        return findCandidates(authentication)
                .flatMap(value -> this.loadFragment(value, authentication))
                .flatMap(this::convertStatements)
                .flatMap(this::insertStatements)
                .flatMap(this::deleteStatements)
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .flatMap(transaction -> this.storeTransactions(List.of(transaction), authentication))
                .doOnError(throwable -> {
                    log.error("Exception during check for global identifiers: {}", throwable.getMessage());
                })
                .doOnSubscribe(sub -> {
                    log.trace("Checking for external identifiers.");
                    labelCheckRunning = true;
                })
                .doOnComplete(() -> {
                    labelCheckRunning = false;
                });
    }





    private Flux<Resource> findCandidates(Authentication authentication) {
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


    private Flux<Transaction> commit(Transaction transaction, Authentication authentication) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(List.of(transaction), authentication);
    }

    private Flux<Transaction> storeTransactions(Collection<Transaction> transactions, Authentication authentication) {
        //FIXME: through event
        return this.trxStore.store(transactions, authentication);
    }

    private Mono<Transaction> deleteStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.subjectStatements());
        statements.addAll(statementsBag.objectStatements());

        return entityStore.removeStatements(statements, statementsBag.transaction());
    }

    private Mono<StatementsBag> insertStatements(StatementsBag bag) {
        return this.entityStore.insert(bag.convertedStatements(), bag.transaction()).then(Mono.just(bag));
    }

    private Mono<StatementsBag> convertStatements(StatementsBag bag) {
        LinkedHashModel model = new LinkedHashModel();
        model.addAll(bag.objectStatements());
        model.addAll(bag.subjectStatements());

        return this.replaceExternalIdentifiers.handle(new TripleModel(model), Map.of())
                .flatMap(tripleModel -> this.replaceAnonymousIdentifiers.handle(tripleModel, Map.of()))
                .map(tripleModel -> bag.convertedStatements().addAll(tripleModel.getModel()))
                .then(Mono.just(bag))
                .doOnSubscribe(sub -> log.trace("Converting statements for resource '{}' with {} statements as subject and {} statements as objects", bag.candidateIdentifier(), bag.subjectStatements().size(), bag.objectStatements().size()));
    }

    public Mono<StatementsBag> loadFragment(Resource candidate, Authentication authentication) {
        StatementsBag statementsBag = new StatementsBag(candidate, Collections.synchronizedSet(new HashSet<>()), Collections.synchronizedSet(new HashSet<>()), new LinkedHashModel(), new Transaction());

        return this.loadFragment(candidate, statementsBag, authentication)
                .doOnSuccess(su -> {
                    log.debug("Finding {} subject and {} object statements for candidate identifier '{}'", statementsBag.subjectStatements().size(), statementsBag.objectStatements().size(), statementsBag.candidateIdentifier());
                });


    }

    public Mono<StatementsBag> loadFragment(Resource candidate, StatementsBag bag, Authentication authentication) {
        return Mono.zip(
                this.entityStore.listStatements(candidate, null, null, authentication),
                this.entityStore.listStatements(null, null, candidate, authentication)
        ).flatMap(pair -> {
            bag.subjectStatements().addAll(pair.getT1());
            bag.objectStatements().addAll(pair.getT2());
            return Mono.just(bag);
            /*
            Set<Statement> statements = bag.subjectStatements().stream().filter(statement -> statement.getObject().isBNode()).collect(Collectors.toUnmodifiableSet());
            // !!! recursive call, be careful
            return Flux.fromIterable(statements)
                    .flatMap(statement -> {
                        if(bag.subjectStatements().stream().filter(s -> s.getSubject().equals(statement.getObject())).findAny().isEmpty()) {
                            return loadFragment((BNode) statement.getObject(), bag, authentication);
                        } else {
                            return Mono.just(statement);
                        }

                    })
                    .then(Mono.just(bag));

             */

        });
    }


    /**
     * find all statements where the culprit is either subject or object
     *
     * @param value
     * @return
     */
    private Mono<StatementsBag> loadSubjectFragments(Value value, Authentication authentication) {

        /**
         * Problem
         *  in first run, anonymous node in object is mapped to fbklgj5f
         *  in second run, anonymous node in same object is mapped to bzpgjhdx
         *
         *  Result:
         * _:6469bdd431834fddb990e1cafe26847f2 <https://schema.org/hasDefinedTerm> <urn:pwid:meg:e:fbklgj5f> .
         *
         * <urn:pwid:meg:e:z6_jewxx> a <https://schema.org/VideoObject>;
         *   <https://schema.org/hasDefinedTerm> <urn:pwid:meg:e:bzpgjhdx> .
         *
         * <urn:pwid:meg:e:bzpgjhdx> a <https://schema.org/DefinedTerm>;
         *
         * Best Approach:
         *  2 runs
         *      First: replace only subjects (since here we properties to make it reproducible)
         *          Store mappings
         *      Second:
         *          For all existing mappings replace also the objects
         *
         */
        if (value.isResource()) {

            this.entityStore.listStatements((Resource) value, null, null, authentication)
                    .map(pair -> new StatementsBag((Resource) value, new HashSet<>(pair), Set.of(), new LinkedHashModel() , new Transaction()))
                    .doOnSubscribe(sub -> {
                        log.debug("Finding subject candidates for resource '{}'", value.stringValue());
                    });
        }
        return Mono.empty();
    }

    private Mono<StatementsBag> loadObjectFragments(Value value, Authentication authentication) {
        if (!value.isResource()) return Mono.empty();

        return this.entityStore.listStatements(null, null, value, authentication)
                .map(pair -> new StatementsBag((Resource) value, Set.of(), new HashSet<>(pair), new LinkedHashModel() , new Transaction()))

                .doOnSubscribe(sub -> {
                    log.debug("Finding object candidates for resource '{}'", value.stringValue());
                });

    }

}


