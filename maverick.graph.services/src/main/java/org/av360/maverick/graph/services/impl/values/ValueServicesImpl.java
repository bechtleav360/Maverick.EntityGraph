package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.av360.maverick.graph.model.aspects.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.av360.maverick.graph.store.SchemaStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Set;

@Slf4j(topic = "graph.svc.values")
@Service
public class ValueServicesImpl implements ValueServices {


    final ApplicationEventPublisher eventPublisher;

    final SchemaServices schemaServices;

    final EntityServices entityServices;

    final IdentifierServices identifierServices;
    final InsertValues insertValues;
    final ReadValues readValues;
    final ReadLinks readLinks;
    final InsertLinks insertLinks;
    final DeleteValue deleteValue;
    final InsertDetails insertDetails;
    final DeleteLinks deleteLinks;
    public DeleteDetails deleteDetails;


    public ValueServicesImpl(SchemaStore schemaStore,
                             ApplicationEventPublisher eventPublisher,
                             SchemaServices schemaServices,
                             EntityServices entityServices,
                             IdentifierServices identifierServices) {
        this.eventPublisher = eventPublisher;
        this.schemaServices = schemaServices;
        this.entityServices = entityServices;
        this.identifierServices = identifierServices;

        this.insertValues = new InsertValues(this);
        this.readValues = new ReadValues(this);
        this.deleteValue = new DeleteValue(this);
        this.readLinks = new ReadLinks(this);
        this.insertLinks = new InsertLinks(this);
        this.insertDetails = new InsertDetails(this);
        this.deleteLinks = new DeleteLinks(this);
        this.deleteDetails = new DeleteDetails(this);


    }




    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertValue(String entityKey, String prefixedPoperty, String value, String languageTag, @Nullable Boolean replace, SessionContext ctx) {
        return this.insertValues.insert(entityKey, prefixedPoperty, value, languageTag, replace, ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx) {
        return this.insertValues.insert(entityIdentifier, predicate, value, replace, ctx);
    }

    /**
     * Deletes a value with a new transaction.  Fails if no entity exists with the given subject
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeValue(String entityKey, String predicate, @Nullable String languageTag, @Nullable String valueIdentifier, SessionContext ctx) {
        return this.deleteValue.remove(entityKey, predicate, languageTag, valueIdentifier, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertLink(String entityKey, String prefixedKey, String targetKey, @Nullable Boolean replace, SessionContext ctx) {
        return this.insertLinks.insert(entityKey, prefixedKey, targetKey, replace, ctx);

    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertDetail(String entityKey, String prefixedValueKey, String prefixedDetailKey, String value, @Nullable String hash, SessionContext ctx) {
        return this.insertDetails.insert(entityKey, prefixedValueKey, prefixedDetailKey, value, hash, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertEmbedded(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, SessionContext ctx) {
        return this.insertValues.insert(entityIdentifier, predicate, embeddedNode, embedded, ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeValue(IRI entityIdentifier, IRI predicate, @Nullable String languageTag, @Nullable String valueIdentifier, SessionContext ctx) {
        return this.deleteValue.remove(entityIdentifier, predicate, languageTag, valueIdentifier, ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeLink(String entityKey, String prefixedProperty, String targetKey, SessionContext ctx) {
        return this.deleteLinks.remove(entityKey, prefixedProperty, targetKey, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeDetail(String entityKey, String prefixedValuePredicate, String prefixedDetailPredicate, String valueHash, SessionContext ctx) {
        return this.deleteDetails.remove(entityKey, prefixedValuePredicate, prefixedDetailPredicate, valueHash, ctx);
    }


    /**
     * Reroutes a statement of an entity, e.g. <entity> <hasProperty> <falseEntity> to <entity> <hasProperty> <rightEntity>.
     * <p>
     * Has to be part of one transaction (one commit call)
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx) {
        return this.insertValues.replace(entityIdentifier, predicate, oldValue, newValue, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> listLinks(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return this.readLinks.list(entityKey, prefixedPoperty, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<TripleModel> listValues(String entityKey, @Nullable String prefixedPoperty, SessionContext ctx) {
        return this.readValues.listValues(entityKey, prefixedPoperty, ctx);
    }


    Mono<Transaction> insertStatements(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, Transaction transaction, SessionContext ctx) {
        return this.entityServices.get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .map(entity -> Pair.of(entity, transaction.affects(entity.getModel())))
                .filter(pair -> !embeddedNode.isBNode())
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to shared node as anonymous node.")))
                .doOnNext(pair -> {
                    Statement statement = SimpleValueFactory.getInstance().createStatement(entityIdentifier, predicate, embeddedNode);
                    embedded.add(statement);
                })
                .flatMap(pair -> this.entityServices.getStore(ctx).insertModel(new LinkedHashModel(embedded), pair.getRight()))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));

    }

    Mono<Transaction> insertStatement(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction, boolean replace, SessionContext ctx) {

        Triple triple = SimpleValueFactory.getInstance().createTriple(entityIdentifier, predicate, value);

        return this.entityServices.get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .doOnNext(entity -> transaction.affects(entity.getModel()))
                .flatMap(entity -> {

                    // linking to bnodes is forbidden
                    if (triple.getObject().isBNode()) {
                        log.trace("Insert link for {} to anonymous node is forbidden.", entityIdentifier);
                        return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to anonymous node."));
                    } else if (triple.getObject().isIRI()) {
                        return this.buildTransactionForIRIStatement(triple, entity, transaction, replace, ctx);
                    } else if (triple.getObject().isLiteral()) {
                        return this.buildTransactionForLiteralStatement(triple, entity, transaction, replace, ctx);
                    } else return Mono.empty();

                })

                .flatMap(pair -> this.entityServices.getStore(ctx).addStatement(entityIdentifier, predicate, value, transaction))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));

    }

    private Mono<Transaction> buildTransactionForIRIStatement(Triple statement, RdfEntity entity, Transaction transaction, boolean replace, SessionContext ctx) {
        // check if entity already has this statement. If yes, we do nothing
        if (statement.getObject().isIRI() && entity.hasStatement(statement) && !replace) {
            log.trace("Entity {} already has a link '{}' for predicate '{}', ignoring update.", entity.getIdentifier(), statement.getObject(), statement.getPredicate());
            return Mono.empty();
        } else {
            return this.entityServices.getStore(ctx).addStatement(statement, transaction);
        }
    }


    private Mono<Transaction> buildTransactionForLiteralStatement(Triple triple, RdfEntity entity, Transaction transaction, boolean replace, SessionContext ctx) {
        if (triple.getObject().isLiteral() && entity.hasStatement(triple.getSubject(), triple.getPredicate(), null) && replace) {
            log.trace("Entity {} already has a value for predicate '{}'.", entity.getIdentifier(), triple.getPredicate());
            Literal updateValue = (Literal) triple.getObject();

            try {
                for (Statement statement : entity.listStatements(entity.getIdentifier(), triple.getPredicate(), null)) {
                    if (!statement.getObject().isLiteral())
                        throw new InvalidEntityUpdate(entity.getIdentifier(), "Replacing an existing link to another entity with a value is not allowed. ");

                    Literal currentValue = (Literal) statement.getObject();
                    if (updateValue.getLanguage().isPresent() && currentValue.getLanguage().isPresent()) {
                        // entity already has a value for this predicate. It has a language tag. If another value with the same language tag exists, we remove it.
                        if (StringUtils.equals(currentValue.getLanguage().get(), updateValue.getLanguage().get())) {
                            this.entityServices.getStore(ctx).removeStatement(statement, transaction);
                        }
                    } else {
                        // entity already has a value for this predicate. It has no language tag. If an existing value has a language tag, we throw an error. If not, we remove it.
                        if (currentValue.getLanguage().isPresent())
                            throw new InvalidEntityUpdate(entity.getIdentifier(), "This value already exists with a language tag within this entity. Please add the tag.");

                        this.entityServices.getStore(ctx).removeStatement(statement, transaction);
                    }

                }
                return Mono.just(transaction);

            } catch (InvalidEntityUpdate e) {
                return Mono.error(e);
            }

        } else return Mono.just(transaction);

    }

}
