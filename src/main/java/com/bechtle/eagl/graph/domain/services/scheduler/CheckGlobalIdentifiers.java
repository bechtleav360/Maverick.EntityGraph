package com.bechtle.eagl.graph.domain.services.scheduler;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
public class CheckGlobalIdentifiers {

    // FIXME: should not directly access the services
    private final QueryServices queryServices;


    private final EntityStore store;
    private final TransactionsStore trxStore;


    public CheckGlobalIdentifiers( QueryServices queryServices, EntityStore store, TransactionsStore trxStore) {
        this.queryServices = queryServices;
        this.store = store;
        this.trxStore = trxStore;
    }


    @Scheduled(fixedDelay = 5000)
    public void checkForGlobalIdentifiers() {
        findGlobalIdentifiers()
                .flatMap(this::getOldStatements)
                .flatMap(this::storeNewStatements)
                .flatMap(this::deleteOldStatements)
                .flatMap(this::storeTransaction)
                .collectList()
                .doOnSubscribe(s -> log.trace("Starting"))
                .doOnSuccess(list -> {
                    Integer reduce = list.stream()
                            .map(transaction -> transaction.listModifiedResources().size())
                            .reduce(0, Integer::sum);
                    log.debug("(Scheduled) Checking for invalided identifiers completed, {} resources were updated.", reduce);
                })
                .subscribe();


    }

    private Mono<Transaction> storeTransaction(Transaction transaction) {
        //FIXME: through event
        return this.trxStore.store(transaction);
    }

    private Mono<Transaction> deleteOldStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.subjectStatements());
        statements.addAll(statementsBag.objectStatements());

        return store.deleteModel(statements, statementsBag.transaction());
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
        return store.insertModel(builder.build(), statements.transaction())
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
                    this.store.listStatements((Resource) value, null, null),
                            //.map(statements -> statements.stream().filter(statement -> !statement.getPredicate().equals(RDF.TYPE)).toList()),
                    this.store.listStatements(null, null, value)
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
                            //log.trace("Found global identifier '{}', transform to local identifier", bindings.getValue("a"));
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