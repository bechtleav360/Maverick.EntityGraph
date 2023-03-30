package org.av360.maverick.graph.feature.jobs.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.transformers.replaceGlobalIdentifiers.ExternalIdentifierTransformer;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
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


@Slf4j(topic = "graph.jobs.identifiers")
@Component
public class ReplaceExternalIdentifiersServiceV2 {

    private final QueryServices queryServices;

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;

    private final IdentifierFactory identifierFactory;

    private final ExternalIdentifierTransformer transformer;


    public ReplaceExternalIdentifiersServiceV2(QueryServices queryServices, EntityStore store, TransactionsStore trxStore, IdentifierFactory identifierFactory, ExternalIdentifierTransformer transformer) {
        this.queryServices = queryServices;
        this.entityStore = store;
        this.trxStore = trxStore;
        this.identifierFactory = identifierFactory;
        this.transformer = transformer;
    }

    private boolean labelCheckRunning = false;

    public boolean isRunning() {
        return labelCheckRunning;
    }


    public Mono<Void> run(Authentication authentication) {
        if (Objects.isNull(this.transformer))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForGlobalIdentifiers(authentication)
                .then();

    }

    private Flux<Value> findCandidates(Authentication authentication) {
        String tpl = """
                SELECT DISTINCT ?a WHERE {
                  ?a a ?c .
                  FILTER NOT EXISTS {
                    FILTER STRSTARTS(str(?a), "%s").
                    }
                  } LIMIT 5000
                """;
        String query = String.format(tpl, Local.Entities.NAMESPACE);
        return queryServices.queryValues(query, authentication)
                .map(bindings -> bindings.getValue("a"));
    }

    public Flux<Transaction> checkForGlobalIdentifiers(Authentication authentication) {


        return findCandidates(authentication)
                .flatMap(value -> this.loadFragments(value, authentication))
                .flatMap(bag -> this.convertAndStoreStatements(bag, authentication))
                .flatMap(this::deleteOldStatements)
                .buffer(50)
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .buffer(50)
                .flatMap(transactions -> this.storeTransactions(transactions, authentication))
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

    private Flux<Transaction> commit(List<Transaction> transactions, Authentication authentication) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(transactions, authentication);
    }

    private Flux<Transaction> storeTransactions(Collection<Transaction> transactions, Authentication authentication) {
        //FIXME: through event
        return this.trxStore.store(transactions, authentication);
    }

    private Mono<Transaction> deleteOldStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.subjectStatements());
        statements.addAll(statementsBag.objectStatements());

        return entityStore.removeStatements(statements, statementsBag.transaction());
    }

    private Mono<StatementsBag> convertAndStoreStatements(StatementsBag bag, Authentication authentication) {
        LinkedHashModel model = new LinkedHashModel();
        model.addAll(bag.objectStatements());
        model.addAll(bag.subjectStatements());

        return this.transformer.handle(new TripleModel(model), Map.of(), authentication)
                .flatMap(updatedModel -> this.entityStore.insert(updatedModel.getModel(), bag.transaction()))
                .map(transaction -> bag);
    }


    /**
     * find all statements where the culprit is either subject or object
     *
     * @param value
     * @return
     */
    private Mono<StatementsBag> loadFragments(Value value, Authentication authentication) {
        if (value.isResource()) {
            return Mono.zip(
                    this.entityStore.listStatements((Resource) value, null, null, authentication),
                    //.map(statements -> statements.stream().filter(statement -> !statement.getPredicate().equals(RDF.TYPE)).toList()),
                    this.entityStore.listStatements(null, null, value, authentication)
            ).map(pair -> new StatementsBag(pair.getT1(), pair.getT2(), (Resource) value, new Transaction()));
        }
        return Mono.empty();
    }


    private record StatementsBag(List<Statement> subjectStatements, List<Statement> objectStatements,
                                 Resource globalIdentifier, Transaction transaction) {
    }
}


