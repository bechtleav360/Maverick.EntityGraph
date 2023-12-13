package org.av360.maverick.graph.store.rdf.fragments;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.identifier.DefaultIdentifierFactory;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;

/**
 * The Transaction consists of three models:
 * <p>
 * - the transaction statements (persisted in transactions graph)
 * - the affected model (what is returned to the client, comprises the changeset and its context)
 * - the changeset model (the actual content of the transaction)
 * <p>
 * The are individual named graphs in the model.
 */


@Slf4j
public class RdfTransaction extends TripleModel implements Transaction {
    private final IRI transactionIdentifier;


    public RdfTransaction() {
        super();
        transactionIdentifier = DefaultIdentifierFactory.getInstance().createRandomIdentifier(Local.Transactions.NAME);

        super.getBuilder()
                .namedGraph(Transactions.GRAPH_PROVENANCE)
                .setNamespace(PROV.NS)
                .setNamespace(Local.Transactions.NAMESPACE)
                .subject(transactionIdentifier)
                .add(Transactions.STATUS, Transactions.RUNNING)
                .add(RDF.TYPE, Transactions.TRANSACTION)
                .add(Transactions.AT, SimpleValueFactory.getInstance().createLiteral(new Date()));
    }


    public Transaction removes(Collection<Statement> statements) {
        if (log.isTraceEnabled())
            log.trace("Removal planned for {} statements in transaction '{}'.", statements.size(), this.getIdentifier().getLocalName());

        return this.include(statements, Activity.REMOVED);
    }

    public IRI getIdentifier() {
        return this.transactionIdentifier;
    }

    private RdfTransaction remove(Collection<Statement> statements, Activity activity) {
        super.getBuilder().add(statements, Transactions.GRAPH_DELETED);

        statements.stream().map(Statement::getSubject).distinct().forEach(resource -> {
            super.getModel().add(transactionIdentifier, activity.toIRI(), resource, Transactions.GRAPH_PROVENANCE);
        });
        return this;
    }


    public RdfTransaction include(Collection<Statement> statements, Activity activity) {
        switch (activity) {
            case INSERTED -> super.getBuilder().add(statements, Transactions.GRAPH_CREATED);
            case UPDATED -> super.getBuilder().add(statements, Transactions.GRAPH_UPDATED);
            case REMOVED -> super.getBuilder().add(statements, Transactions.GRAPH_DELETED);
            default -> super.getBuilder().add(statements, Transactions.GRAPH_UPDATED);
        }

        statements.stream().map(Statement::getSubject).distinct().forEach(resource -> {
            super.getModel().add(transactionIdentifier, activity.toIRI(), resource, Transactions.GRAPH_PROVENANCE);
        });

        return this;
    }



    public RdfTransaction affects(TripleModel wrappedModel) {
        return this.affects(wrappedModel.getModel());
    }

    public RdfTransaction affects(Statement statement) {
        return this.affects(List.of(statement));
    }

    /**
     * The affected model is unchanged
     *
     * @param statements
     * @return
     */
    public RdfTransaction affects(Collection<Statement> statements) {
        super.getBuilder().add(statements, Transactions.GRAPH_AFFECTED);
        return this;
    }


    public Transaction inserts(Collection<Statement> statements) {
        return this.include(statements, Activity.INSERTED);
    }

    @Override
    public Transaction updates(Collection<Statement> statements) {
        return this.include(statements, Activity.UPDATED);
    }

    public static boolean isTransaction(Model model) {
        return model.contains(null, RDF.TYPE, Transactions.TRANSACTION);
    }



    @Override
    public Model getModel() {
        return super.getModel();
    }


    public List<Value> affectedSubjects(Activity... activities) {
        List<Value> result = new ArrayList<>();
        Arrays.stream(activities).forEach(activity -> {
            List<Value> values = super.streamStatements(transactionIdentifier, activity.toIRI(), null).map(Statement::getObject).toList();
            result.addAll(values);
        });
        return result;
    }


    /**
     * We merge the named graphs of the transaction and affected model (but not the actual change itself)
     *
     * @return
     */
    @Override
    public Collection<AnnotatedStatement> asStatements() {
        return super.asStatements(Transactions.GRAPH_PROVENANCE, Transactions.GRAPH_AFFECTED);
    }



    public void setCompleted() {
        Model m = super.getBuilder().build();
        m.add(transactionIdentifier, Transactions.STATUS, Transactions.SUCCESS, Transactions.GRAPH_PROVENANCE);
        m.remove(transactionIdentifier, Transactions.STATUS, Transactions.RUNNING, Transactions.GRAPH_PROVENANCE);
    }


    @Override
    public boolean isCompleted() {
        return getModel().contains(transactionIdentifier, Transactions.STATUS, Transactions.SUCCESS, Transactions.GRAPH_PROVENANCE);
    }

    public void setFailed(String message) {
        Model m = super.getBuilder().build();
        m.add(transactionIdentifier, Transactions.STATUS, Transactions.FAILURE, Transactions.GRAPH_PROVENANCE);
        m.remove(transactionIdentifier, Transactions.STATUS, Transactions.RUNNING, Transactions.GRAPH_PROVENANCE);

    }



    private static class StatementComparator implements Comparator<Statement> {
        @Override
        public int compare(Statement o1, Statement o2) {
            return 0;
        }
    }
}
