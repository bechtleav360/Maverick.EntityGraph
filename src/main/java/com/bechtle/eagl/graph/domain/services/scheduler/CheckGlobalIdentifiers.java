package com.bechtle.eagl.graph.domain.services.scheduler;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.domain.services.QueryServices;
import com.bechtle.eagl.graph.repository.EntityStore;
import com.bechtle.eagl.graph.repository.TransactionsStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Periodically runs the following sparql queries, grabs the entity definition for it and regenerates the identifiers
 * <p>
 * <p>
 SELECT ?a WHERE {
 ?a a ?c .
 FILTER NOT EXISTS {
 FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
 }

 } LIMIT 100
 */
@Slf4j
@Component
@Profile({"check-global-identifiers"})
public class CheckGlobalIdentifiers {

    // FIXME: should not directly access the services
    private final QueryServices queryServices;


    private final EntityStore entityStore;
    private final TransactionsStore trxStore;


    public CheckGlobalIdentifiers( QueryServices queryServices, EntityStore store, TransactionsStore trxStore) {
        this.queryServices = queryServices;
        this.entityStore = store;
        this.trxStore = trxStore;
    }


    @Scheduled(fixedDelay = 60000)
    public void checkForGlobalIdentifiersScheduled() {
        this.checkForGlobalIdentifiers()
                .collectList()
                .doOnError(throwable -> log.error("(Scheduled) Checking for invalided identifiers failed. ", throwable))
                .doOnSuccess(list -> {
                    Integer reduce = list.stream()
                            .map(transaction -> transaction.listModifiedResources().size())
                            .reduce(0, Integer::sum);
                    if(reduce > 0) {
                        log.debug("(Scheduled) Checking for invalided identifiers completed, {} resources were updated.", reduce);
                    } else {
                        log.debug("(Scheduled) No invalided identifiers found");
                    }

                }).subscribe();
    }

    public Flux<Transaction> checkForGlobalIdentifiers() {
        return findGlobalIdentifiers()
                .flatMap(this::getOldStatements)
                .flatMap(this::storeNewStatements)
                .flatMap(this::deleteOldStatements)
                .buffer(50)
                .flatMap(this::commit)
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n"+transaction);
                })
                .buffer(50)
                .flatMap(this::storeTransactions)
                .doOnError(throwable -> {
                    log.error("Exception during check: {}",throwable.getMessage());
                })
                ;
    }

    private Flux<Transaction> commit(List<Transaction> transactions) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(transactions);
    }

    private Flux<Transaction> storeTransactions(Collection<Transaction> transactions) {
        //FIXME: through event
        return this.trxStore.store(transactions);
    }

    private Mono<Transaction> deleteOldStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.subjectStatements());
        statements.addAll(statementsBag.objectStatements());

        return entityStore.deleteModel(statements, statementsBag.transaction());
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
        return entityStore.insertModel(builder.build(), statements.transaction())
                .map(transaction -> statements);
    }


    /**
     * find all statements where the culprit is either subject or object
     * @param value
     * @return
     */
    private Mono<StatementsBag> getOldStatements(Value value) {

        if (value.isResource()) {
            return Mono.zip(
                    this.entityStore.listStatements((Resource) value, null, null),
                            //.map(statements -> statements.stream().filter(statement -> !statement.getPredicate().equals(RDF.TYPE)).toList()),
                    this.entityStore.listStatements(null, null, value)
            ).map(pair -> new StatementsBag(pair.getT1(), pair.getT2(), (Resource) value, new Transaction()));
        }
        return Mono.empty();
    }

    private Flux<Value> findGlobalIdentifiers() {
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
        return queryServices.queryValues(query)
                .flatMapMany(queryResult -> {
                    return Flux.create(c -> {
                        queryResult.forEach(bindings -> {
                            // log.trace("Found global identifier '{}', transform to local identifier", bindings.getValue("a"));
                            c.next(bindings.getValue("a"));
                        });
                        c.complete();
                    });

                });
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