package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.preprocessors.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Result of the check for external subject identifiers
 * <pre>
 * <urn:epwid:meg:e:_ii64uxx> a <htps://schema.org/VideoObject>;
 *   <htps://schema.org/identifier> "c";
 *   <urn:int:srcid> _:2c7bc378441944efadb5210464450a1d2;
 *   <htps://schema.org/hasDefinedTerm> _:2c7bc378441944efadb5210464450a1d3 .
 *
 * <urn:pwid:meg:e:b9wnybnx> a <htps://schema.org/DefinedTerm>;
 *   <htp://www.w3.org/2000/01/rdf-schema#label> "Term 3";
 *   <urn:int:srcid> _:2c7bc378441944efadb5210464450a1d3 .
 * </pre>
 *
 *
 * In the next phase, we have to
 * a) find object candidates (all removableStatements with bnode or external iri identifier)
 * b) check if we have a statement ?s <urn:int:srcid> candidate, if yes, get its local identifier
 * c) replace all removableStatements ?s ?p candidate with `?s ?p localIdentifier
 * d) remove all removableStatements ?s <urn:int:srcid> candidate
 */

@Slf4j(topic = "graph.jobs.identifiers")
@Component
public class ReplaceLinkedIdentifiersJob implements ScheduledJob {

    public static String NAME = "replaceLinkedIdentifiers";

    private final EntityServices entityServices;
    private final TransactionsService transactionsService;
    private final ReplaceExternalIdentifiers replaceExternalIdentifiers;
    private final QueryServices queryServices;


    private record StatementsBag(
            Resource candidateIdentifier,
            Set<Statement> removableStatements,
            Model convertedStatements,
            @Nullable Statement originalIdentifierStatement,
            Transaction transaction
    ) {
    }

    public ReplaceLinkedIdentifiersJob(IndividualsStore store, TransactionsStore trxStore, EntityServices entityServices, TransactionsService transactionsService, ReplaceExternalIdentifiers transformer, QueryServices queryServices) {
        this.entityServices = entityServices;
        this.transactionsService = transactionsService;
        this.replaceExternalIdentifiers = transformer;
        this.queryServices = queryServices;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(SessionContext ctx) {
        if (Objects.isNull(this.replaceExternalIdentifiers))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForLinkedObjectIdentifiers(ctx)
                .doOnError(throwable -> log.error("Exception while relinking objects to new subject identifiers in environment {}: {}", ctx.getEnvironment(), throwable.getMessage()))
                .doOnSubscribe(sub -> {
                    ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.ENTITIES));
                    log.trace("Checking for external or anonymous identifiers in objects in environment: {}.", ctx.getEnvironment());
                })
                .doOnComplete(() -> log.info("Completed checking for external or anonymous identifiers in objects in environment: {}.", ctx.getEnvironment()))
                .then();

    }



    private Flux<Transaction> checkForLinkedObjectIdentifiers(SessionContext ctx) {
        return this.loadObjectStatements(ctx)
                .flatMap(this::convertObjectStatements)
                .flatMap(bag -> this.insertStatements(bag, ctx))
                .flatMap(bag -> this.deleteStatements(bag, ctx))
                .buffer(100)
                .flatMap(transactions -> this.commit(transactions, ctx))
                .doOnNext(transaction -> Assert.isTrue(transaction.getModel(Transactions.GRAPH_PROVENANCE).contains(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction))
                .buffer(5)
                .flatMap(transactions -> this.storeTransactions(transactions, ctx));
    }


    private Flux<Transaction> commit(List<Transaction> transactions, SessionContext ctx) {
        // FIXME: assert system authentication
        return this.entityServices.getStore(ctx).asCommitable().commit(transactions, ctx.getEnvironment());
    }

    private Flux<Transaction> storeTransactions(Collection<Transaction> transactions, SessionContext ctx) {
        //FIXME: through event
        return this.transactionsService.save(transactions, ctx);
    }

    private Mono<Transaction> deleteStatements(StatementsBag statementsBag, SessionContext ctx) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.removableStatements());
        return Mono.just(statementsBag.transaction().removes(statements));
    }

    private Mono<StatementsBag> insertStatements(StatementsBag bag, SessionContext ctx) {
        bag.transaction().forInsert(bag.convertedStatements());
        return Mono.just(bag);
    }

    private Mono<StatementsBag> convertObjectStatements(StatementsBag bag) {
        Objects.requireNonNull(bag.originalIdentifierStatement());

        ModelBuilder modelBuilder = new ModelBuilder();
        bag.removableStatements().forEach(statement -> modelBuilder.add(statement.getSubject(), statement.getPredicate(), bag.originalIdentifierStatement().getSubject()));
        bag.convertedStatements().addAll(modelBuilder.build());

        if(bag.originalIdentifierStatement().getObject().isBNode()) {
            // we remove old identifiers if they were anonymous
            bag.removableStatements().add(bag.originalIdentifierStatement());
        }

        if(bag.originalIdentifierStatement().getObject().isIRI()) {
            // in other cases we replace it with owl:sameAs
            bag.removableStatements().add(bag.originalIdentifierStatement());
            bag.convertedStatements().add(bag.originalIdentifierStatement().getSubject(), OWL.SAMEAS, bag.originalIdentifierStatement().getObject());
        }

        return Mono.just(bag).doOnSubscribe(sub -> log.trace("Converting object removableStatements for resource '{}' with {} affected Statements", bag.candidateIdentifier(), bag.removableStatements().size()));
    }


    public Flux<StatementsBag> loadObjectStatements(SessionContext ctx) {
        return this.entityServices.getStore(ctx).asStatementsAware().listStatements(null, Local.ORIGINAL_IDENTIFIER, null, ctx.getEnvironment())
                .flatMapMany(Flux::fromIterable)
                .filter(statement -> statement.getObject().isResource())
                .flatMap(st ->
                        this.entityServices.getStore(ctx).asStatementsAware().listStatements(null, null, st.getObject(), ctx.getEnvironment())
                                .map(statements -> statements.stream()
                                        .filter(s -> ! s.getPredicate().equals(Local.ORIGINAL_IDENTIFIER))
                                        .filter(s -> ! s.getPredicate().equals(OWL.SAMEAS))
                                        .collect(Collectors.toSet()))
                                .map(statements -> new StatementsBag((Resource) st.getObject(), new HashSet<>(statements), new LinkedHashModel(), st, new RdfTransaction()))
                );
    }

    public Flux<StatementsBag> loadObjectStatementsWithQuery(SessionContext ctx) {
        String query = """
                SELECT DISTINCT ?subject ?object 
                WHERE {
                  ?subject <%s> ?object .
                }
                LIMIT 1000
                """.formatted(Local.ORIGINAL_IDENTIFIER);


        return  this.queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(bindings -> SimpleValueFactory.getInstance().createStatement(
                        (Resource) bindings.getValue("subject"),
                        Local.ORIGINAL_IDENTIFIER,
                        bindings.getValue("object")
                ))
                .filter(statement -> statement.getObject().isResource())
                .flatMap(st ->
                        this.entityServices.getStore(ctx).asStatementsAware().listStatements(null, null, st.getObject(), ctx.getEnvironment())
                                .map(statements -> statements.stream()
                                        .filter(s -> ! s.getPredicate().equals(Local.ORIGINAL_IDENTIFIER))
                                        .filter(s -> ! s.getPredicate().equals(OWL.SAMEAS))
                                        .collect(Collectors.toSet()))
                                .map(statements -> new StatementsBag((Resource) st.getObject(), new HashSet<>(statements), new LinkedHashModel(), st, new RdfTransaction()))
                );
    }


    private Flux<Resource> findSubjectCandidates(SessionContext ctx) {
        String tpl = """
                SELECT DISTINCT ?a WHERE {
                  ?a a ?c .
                  FILTER NOT EXISTS {
                    FILTER STRSTARTS(str(?a), "%s").
                    }
                  }
                  LIMIT 1000
                """;
        String query = String.format(tpl, Local.Entities.NAME);
        return this.queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(bindings -> bindings.getValue("a"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }


}


