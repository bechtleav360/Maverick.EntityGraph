package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InvalidConfiguration;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceAnonymousIdentifiers;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
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
public class ReplaceSubjectIdentifiersJob implements Job {

    public static String NAME = "replaceSubjectIdentifiers";


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

    public ReplaceSubjectIdentifiersJob(EntityStore store, TransactionsStore trxStore, ReplaceExternalIdentifiers transformer, ReplaceAnonymousIdentifiers replaceAnonymousIdentifiers) {
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
    public Mono<Void> run(SessionContext ctx) {
        if (Objects.isNull(this.replaceExternalIdentifiers))
            return Mono.error(new InvalidConfiguration("External identity transformer is disabled"));

        return this.checkForExternalSubjectIdentifiers(ctx)
                .doOnError(throwable -> log.error("Exception while finding and replacing subject identifiers in environment {}: {}", ctx.getEnvironment(), throwable.getMessage()))
                .doOnSubscribe(sub -> {
                    ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.ENTITIES));
                    log.trace("Checking for external or anonymous subject identifiers in environment: {}.", ctx.getEnvironment());
                })
                .doOnComplete(() -> log.debug("Completed checking for external or anonymous identifiers in subjects in environment: {}.", ctx.getEnvironment()))
                .then();

    }


    /**
     * First run: replace only subject identifiers, move old identifier in temporary property
     */
    private Flux<RdfTransaction> checkForExternalSubjectIdentifiers(SessionContext ctx) {
        return findSubjectCandidates(ctx)
                .flatMap(value -> this.loadSubjectStatements(value, ctx))
                .flatMap(this::convertSubjectStatements)
                .flatMap(this::insertStatements)
                .flatMap(this::deleteStatements)
                .buffer(50)
                .flatMap(transactions -> this.commit(transactions, ctx))
                .doOnNext(transaction -> Assert.isTrue(transaction.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS), "Failed transaction: \n" + transaction))
                .buffer(5)
                .flatMap(transactions -> this.storeTransactions(transactions, ctx));
    }



    private Flux<Resource> findSubjectCandidates(SessionContext ctx) {
        String tpl = """
                SELECT DISTINCT ?a WHERE {
                  ?a a ?c .
                  FILTER NOT EXISTS {
                    FILTER STRSTARTS(str(?a), "%s").
                    }
                  }
                  LIMIT 5000
                """;
        String query = String.format(tpl, Local.Entities.NAMESPACE);

        return entityStore.query(query, ctx.getEnvironment())
                .map(bindings -> bindings.getValue("a"))
                .filter(Value::isResource)
                .map(value -> (Resource) value);
    }



    private Flux<RdfTransaction> commit(List<RdfTransaction> transactions, SessionContext ctx) {
        // log.trace("Committing {} transactions", transactions.size());
        return this.entityStore.commit(transactions, ctx.getEnvironment(), true)
                .doOnComplete(() -> log.trace("Committed {} transactions in job {}", transactions.size(), this.getName()));
    }

    private Flux<RdfTransaction> storeTransactions(Collection<RdfTransaction> transactions, SessionContext ctx) {
        //FIXME: through event
        return this.trxStore.store(transactions, ctx.getEnvironment());
    }

    private Mono<RdfTransaction> deleteStatements(StatementsBag statementsBag) {
        ArrayList<Statement> statements = new ArrayList<>(statementsBag.removableStatements());

        return entityStore.removeStatements(statements, statementsBag.transaction());
    }

    private Mono<StatementsBag> insertStatements(StatementsBag bag) {
        return this.entityStore.insertModel(bag.convertedStatements(), bag.transaction()).then(Mono.just(bag));
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
        }).doOnSubscribe(sub -> log.trace("Converting subjects for resource '{}' with {} removableStatements", bag.candidateIdentifier(), bag.removableStatements().size()));

    }

    public Mono<StatementsBag> loadSubjectStatements(Resource candidate, SessionContext ctx) {
        return this.entityStore.getFragment(candidate, ctx.getEnvironment())
                .map(fragment -> new StatementsBag(candidate, Collections.synchronizedSet(fragment.getModel()), new LinkedHashModel(), null, new RdfTransaction()));

    }



}


