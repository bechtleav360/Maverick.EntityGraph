package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceAnonymousIdentifiers;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.springframework.security.core.Authentication;
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
 * <urn:pwid:meg:e:_ii64uxx> a <htps://schema.org/VideoObject>;
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
public class ReplaceLinkedExternalIdentifiersJob implements Job {

    public static String NAME = "replaceLinkedIdentifiers";

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

    public ReplaceLinkedExternalIdentifiersJob(EntityStore store, TransactionsStore trxStore, ReplaceExternalIdentifiers transformer, ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers) {
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

        return this.checkForLinkedObjectIdentifiers(authentication).then();

    }



    private Flux<RdfTransaction> checkForLinkedObjectIdentifiers(Authentication authentication) {
        return this.loadObjectStatements(authentication)
                .flatMap(this::convertObjectStatements)
                .flatMap(this::insertStatements)
                .flatMap(this::deleteStatements)
                .buffer(10)
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .buffer(10)
                .flatMap(transactions -> this.storeTransactions(transactions, authentication))
                .doOnError(throwable -> {
                    log.error("Exception while relinking objects to new subject identifiers: {}", throwable.getMessage());
                })
                .doOnSubscribe(sub -> {
                    log.trace("Checking for external or anonymous identifiers in objects.");
                })
                .doOnComplete(() -> {
                    log.debug("Completed checking for external or anonymous identifiers in objects.");
                });
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

    private Mono<StatementsBag> convertObjectStatements(StatementsBag bag) {
        Objects.requireNonNull(bag.originalIdentifierStatement());

        ModelBuilder modelBuilder = new ModelBuilder();
        bag.removableStatements().forEach(statement -> {
            modelBuilder.add(statement.getSubject(), statement.getPredicate(), bag.originalIdentifierStatement().getSubject());
        });
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

        return Mono.just(bag).doOnSubscribe(sub -> log.trace("Converting object removableStatements for resource '{}' with {} removableStatements", bag.candidateIdentifier(), bag.removableStatements().size()));
    }


    public Flux<StatementsBag> loadObjectStatements(Authentication authentication) {
        return this.entityStore.listStatements(null, Local.ORIGINAL_IDENTIFIER, null, authentication, Authorities.READER)
                .flatMapMany(Flux::fromIterable)
                .filter(statement -> statement.getObject().isResource())
                .flatMap(st ->
                        this.entityStore.listStatements(null, null, st.getObject(), authentication)
                                .map(statements -> statements.stream()
                                        .filter(s -> ! s.getPredicate().equals(Local.ORIGINAL_IDENTIFIER))
                                        .filter(s -> ! s.getPredicate().equals(OWL.SAMEAS))
                                        .collect(Collectors.toSet()))
                                .map(statements -> new StatementsBag((Resource) st.getObject(), new HashSet<>(statements), new LinkedHashModel(), st, new RdfTransaction()))
                );
    }


}


