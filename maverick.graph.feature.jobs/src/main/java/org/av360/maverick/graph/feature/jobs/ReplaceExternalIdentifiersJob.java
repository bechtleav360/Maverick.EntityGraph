package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.errors.requests.InvalidConfiguration;
import org.av360.maverick.graph.model.security.Authorities;
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
public class ReplaceExternalIdentifiersJob implements Job {

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
        return "replaceIdentifiers";
    }

    @Override
    public Mono<Void> run(Authentication authentication) {
        if (Objects.isNull(this.replaceExternalIdentifiers))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForExternalSubjectIdentifiers(authentication)
                .thenMany(this.checkForLinkedObjectIdentifiers(authentication))
                .then();

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
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .flatMap(transaction -> this.storeTransactions(List.of(transaction), authentication))
                .doOnError(throwable -> {
                    log.error("Exception while finding and replacing subject identifiers: {}", throwable.getMessage());
                })
                .doOnSubscribe(sub -> {
                    log.trace("Checking for external or anonymous subject identifiers.");
                })
                .doOnComplete(() -> {
                });
    }


    /**
     * Result of the first run
     *
     * <pre>
     * <urn:pwid:meg:e:_ii64uxx> a <https://schema.org/VideoObject>;
     *   <https://schema.org/identifier> "c";
     *   <urn:int:srcid> _:2c7bc378441944efadb5210464450a1d2;
     *   <https://schema.org/hasDefinedTerm> _:2c7bc378441944efadb5210464450a1d3 .
     *
     * <urn:pwid:meg:e:b9wnybnx> a <https://schema.org/DefinedTerm>;
     *   <http://www.w3.org/2000/01/rdf-schema#label> "Term 3";
     *   <urn:int:srcid> _:2c7bc378441944efadb5210464450a1d3 .
     * </pre>
     * <p>
     * In the next phase, we have to
     * a) find object candidates (all removableStatements with bnode or external iri identifier)
     * b) check if we have a statement ?s <urn:int:srcid> candidate, if yes, get its local identifier
     * c) replace all removableStatements ?s ?p candidate with `?s ?p localIdentifier
     * d) remove all removableStatements ?s <urn:int:srcid> candidate
     */
    private Flux<RdfTransaction> checkForLinkedObjectIdentifiers(Authentication authentication) {
        return this.loadObjectStatements(authentication)
                .flatMap(this::convertObjectStatements)
                .flatMap(this::insertStatements)
                .flatMap(this::deleteStatements)
                .flatMap(transactions -> this.commit(transactions, authentication))
                .doOnNext(transaction -> {
                    Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction);
                })
                .flatMap(transaction -> this.storeTransactions(List.of(transaction), authentication))
                .doOnError(throwable -> {
                    log.error("Exception while relinking objects to new subject identifiers: {}", throwable.getMessage());
                })
                .doOnSubscribe(sub -> {
                    log.trace("Checking for external or anonymous identifiers in objects.");
                })
                .doOnComplete(() -> {
                });
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

    private Flux<Resource> findObjectCandidates(Authentication authentication) {
        String tpl = """
                SELECT DISTINCT ?c WHERE {
                  ?a a ?c .
                  FILTER NOT EXISTS {
                    FILTER STRSTARTS(str(?c), "%s").
                    }
                  }
                """;
        String query = String.format(tpl, Local.Entities.NAMESPACE);
        return queryServices.queryValues(query, authentication)
                .map(bindings -> bindings.getValue("c"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }


    private Flux<RdfTransaction> commit(RdfTransaction transaction, Authentication authentication) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(List.of(transaction), authentication);
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

    public Flux<StatementsBag> loadObjectStatements(Authentication authentication) {
        return this.entityStore.listStatements(null, Local.ORIGINAL_IDENTIFIER, null, authentication, Authorities.READER)
                .flatMapMany(Flux::fromIterable)
                .filter(statement -> statement.getObject().isResource())

                .flatMap(st ->
                        this.entityStore.listStatements(null, null, st.getObject(), authentication)
                                .map(statements -> statements.stream().filter(s -> ! s.getPredicate().equals(Local.ORIGINAL_IDENTIFIER)).collect(Collectors.toSet()))
                                .map(statements -> new StatementsBag((Resource) st.getObject(), new HashSet<>(statements), new LinkedHashModel(), st, new RdfTransaction()))
                );
    }


}


