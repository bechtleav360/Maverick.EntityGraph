package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Convenience methods to build a transaction.
 *
 * It is actually a composite of a the transaction model and the actual results.
 *
 */
@Slf4j
public class Transaction extends AbstractModel {
    private final IRI transactionIdentifier;
    private Set<NamespaceAwareStatement> affectedModel;

    public Transaction() {
        super();
        transactionIdentifier = new GeneratedIdentifier(Local.Transactions.NAMESPACE);
        super.getBuilder()
                .setNamespace(PROV.NS)
                .setNamespace(Local.Transactions.NS)
                .subject(transactionIdentifier)
                .add(RDF.TYPE, Transactions.TRANSACTION)
                .add(Transactions.AT, SimpleValueFactory.getInstance().createLiteral(new Date()));
    }


    public Transaction with(AbstractModel model) {
        return this.with(model.streamNamespaceAwareStatements());
    }

    public Transaction with(Model model) {
       return this.with(model.parallelStream().map(st -> NamespaceAwareStatement.wrap(st, model.getNamespaces())));
    }

    public Transaction with(Stream<NamespaceAwareStatement> parallelStream) {
        if(this.affectedModel == null) this.affectedModel = new HashSet<>();
        parallelStream.forEach(this.affectedModel::add);
        return this;
    }

    public Transaction with(NamespaceAwareStatement statement) {
        if(this.affectedModel == null) this.affectedModel = new HashSet<>();
        this.affectedModel.add(statement);
        return this;
    }

    public Transaction with(Iterable<Statement> statements) {
        return this.with(StreamSupport.stream(statements.spliterator(), true).map(st -> NamespaceAwareStatement.wrap(st, Collections.emptySet())));
    }


    public static boolean isTransaction(Model model) {
        return model.contains(null, RDF.TYPE, Transactions.TRANSACTION);
    }


    public void hasCreated(Resource resource) {
        this.getBuilder().add(Transactions.CREATED, resource);
    }

    public void hasUpdated(Resource resource) {
        this.getBuilder().add(Transactions.UPDATED, resource);
    }

    public void hasDeleted(Resource resource) {
        this.getBuilder().add(Transactions.DELETED, resource);
    }

    public List<Value> listModifiedResources() {
        return super.streamStatements(transactionIdentifier, null, null)
                .filter(statement -> statement.getPredicate().equals(Transactions.CREATED) ||
                        statement.getPredicate().equals(Transactions.UPDATED) ||
                        statement.getPredicate().equals(Transactions.DELETED))
                .map(Statement::getObject)
                .collect(Collectors.toList());
    }


    @Override
    public Iterable<? extends NamespaceAwareStatement> asStatements() {
        return this.affectedModel == null ? super.asStatements() : Stream.concat(super.streamNamespaceAwareStatements(), this.affectedModel.stream()).distinct().toList();
    }



}
