package io.av360.maverick.graph.services.schedulers.replaceGlobalIdentifiers;

import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.model.vocabulary.Transactions;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.TransactionsStore;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * If we have any global identifiers (externally set) in the repo, we have to replace them with our internal identifiers.
 * Otherwise we cannot address the entities through our API.
 * <p>
 * Periodically runs the following sparql queries, grabs the entity definition for it and regenerates the identifiers
 * <p>
 * SELECT ?a WHERE { ?a a ?c . }
 * FILTER NOT EXISTS {
 * FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
 * }
 * LIMIT 100
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.schedulers.replaceGlobalIdentifiers", havingValue = "true")
public class ScheduledReplaceGlobalIdentifiers {

    // FIXME: should not directly access the services
    private final QueryServices queryServices;

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;


    public ScheduledReplaceGlobalIdentifiers(QueryServices queryServices, EntityStore store, TransactionsStore trxStore) {
        this.queryServices = queryServices;
        this.entityStore = store;
        this.trxStore = trxStore;
    }


    @Scheduled(fixedDelay = 60000)
    public void checkForGlobalIdentifiersScheduled() {
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken();
        authentication.setAuthenticated(true);
        authentication.grantAuthority(Authorities.SYSTEM);

        this.checkForGlobalIdentifiers(authentication)
                .collectList()
                .doOnError(throwable -> log.error("(Scheduled) Checking for invalided identifiers failed. ", throwable))
                .doOnSuccess(list -> {
                    Integer reduce = list.stream()
                            .map(transaction -> transaction.listModifiedResources().size())
                            .reduce(0, Integer::sum);
                    if (reduce > 0) {
                        log.debug("(Scheduled) Checking for invalided identifiers completed, {} resources were updated.", reduce);
                    } else {
                        log.debug("(Scheduled) No invalided identifiers found");
                    }

                }).subscribe();
    }


    public Flux<Transaction> checkForGlobalIdentifiers(Authentication authentication) {
        return findGlobalIdentifiers(authentication)
                .flatMap(value -> this.getOldStatements(value, authentication))
                .flatMap(this::storeNewStatements)
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
                ;
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

        return entityStore.delete(statements, statementsBag.transaction());
    }

    private Mono<StatementsBag> storeNewStatements(StatementsBag statements) {
        ModelBuilder builder = new ModelBuilder();

        GeneratedIdentifier generatedIdentifier = new GeneratedIdentifier(Local.Entities.NAMESPACE, statements.globalIdentifier());

        statements.subjectStatements().forEach(statement -> {
            builder.add(generatedIdentifier, statement.getPredicate(), statement.getObject());
        });

        statements.objectStatements().forEach(statement -> {
            builder.add(statement.getSubject(), statement.getPredicate(), generatedIdentifier);
        });

        builder.add(generatedIdentifier, DC.IDENTIFIER, statements.globalIdentifier());
        return entityStore.insert(builder.build(), statements.transaction())
                .map(transaction -> statements);
    }


    /**
     * find all statements where the culprit is either subject or object
     *
     * @param value
     * @return
     */
    private Mono<StatementsBag> getOldStatements(Value value, Authentication authentication) {

        if (value.isResource()) {
            return Mono.zip(
                    this.entityStore.listStatements((Resource) value, null, null, authentication),
                    //.map(statements -> statements.stream().filter(statement -> !statement.getPredicate().equals(RDF.TYPE)).toList()),
                    this.entityStore.listStatements(null, null, value, authentication)
            ).map(pair -> new StatementsBag(pair.getT1(), pair.getT2(), (Resource) value, new Transaction()));
        }
        return Mono.empty();
    }

    private Flux<Value> findGlobalIdentifiers(Authentication authentication) {
                /*
        Variable id = SparqlBuilder.var("id");
        Variable type = SparqlBuilder.var("type")
        ;
        Having regex = SparqlBuilder.having(Expressions.regex(id, Local.Entities.NAMESPACE, "i")).
        //RdfObject type = Rdf.object(localEntity.type());
        SelectQuery all = Queries.SELECT(id).where(id.isA(type)
                        .filterNotExists(id.))
                .all();
         */


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


    private record StatementsBag(List<Statement> subjectStatements, List<Statement> objectStatements,
                                 Resource globalIdentifier, Transaction transaction) {
    }
}


/*

// Count number of invalid ids in repo
SELECT DISTINCT (count(?a)as ?count) WHERE {
  ?a a ?c .
  FILTER NOT EXISTS {
 FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
}

  }

SELECT DISTINCT (count(?a)as ?count) WHERE {
  ?a a ?c .
  FILTER NOT EXISTS {
 FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
}

  }


// Cound number of transformed ids
PREFIX dc: <http://purl.org/dc/elements/1.1/>

SELECT (count(?a)as ?count) WHERE {
  ?a dc:identifier ?c .

  }



PREFIX skos: <http://www.w3.org/2008/05/skos-xl#>

SELECT ?a  WHERE {
  ?a a skos:Label .
  }
  LIMIT 100







 */