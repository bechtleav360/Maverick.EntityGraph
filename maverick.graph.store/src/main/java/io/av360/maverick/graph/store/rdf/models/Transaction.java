package io.av360.maverick.graph.store.rdf.models;

import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.model.vocabulary.Transactions;
import io.av360.maverick.graph.model.enums.Activity;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

/**
 * The Transaction consists of three models:
 *
 * - the transaction statements (persisted in transactions graph)
 * - the affected model (what is returned to the client, comprises the changeset and its context)
 * - the changeset model (the actual content of the transaction)
 *
 * The are individual named graphs in the model.
 */


@Slf4j
public class Transaction extends AbstractModel {
    private final IRI transactionIdentifier;


    public Transaction() {
        super();
        transactionIdentifier = new GeneratedIdentifier(Local.Transactions.NAMESPACE);

        super.getBuilder()
                .namedGraph(Transactions.GRAPH_PROVENANCE)
                .setNamespace(PROV.NS)
                .setNamespace(Local.Transactions.NS)
                .subject(transactionIdentifier)
                .add(Transactions.STATUS, Transactions.RUNNING)
                .add(RDF.TYPE, Transactions.TRANSACTION)
                .add(Transactions.AT, SimpleValueFactory.getInstance().createLiteral(new Date()));
    }




    public Transaction remove(Collection<Statement> statements, Activity activity) {
        super.getBuilder().add(statements, Transactions.GRAPH_DELETED);

        statements.stream().map(Statement::getSubject).distinct().forEach(resource -> {
            super.getModel().add(transactionIdentifier, activity.toIRI(), resource, Transactions.GRAPH_PROVENANCE);
        });
        return this;
    }

    public Transaction remove(Statement sts, Activity activity) {
        return this.remove(List.of(sts), activity);
    }

    public Transaction remove(Resource subject, IRI predicate, Value value, Activity activity) {
        return this.remove(SimpleValueFactory.getInstance().createStatement(subject, predicate, value), activity);
    }

    public Transaction insert(Collection<Statement> statements, Activity activity) {
        super.getBuilder().add(statements, Transactions.GRAPH_CREATED, Transactions.GRAPH_AFFECTED);

        statements.stream().map(Statement::getSubject).distinct().forEach(resource -> {
            super.getModel().add(transactionIdentifier, activity.toIRI(), resource, Transactions.GRAPH_PROVENANCE);
        });

        return this;
    }

    public Transaction insert(Statement sts, Activity activity) {
        return this.insert(List.of(sts), activity);
    }

    public Transaction insert(Resource subject, IRI predicate, Value value, Activity activity) {
        return this.insert(SimpleValueFactory.getInstance().createStatement(subject, predicate, value), activity);
    }




    public Transaction affected(AbstractModel wrappedModel) {
        return this.affected(wrappedModel.getModel());
    }

    public Transaction affected(Statement statement) {
        return this.affected(List.of(statement));
    }

    /**
     * The affected model is unchanged
     * @param statements
     * @return
     */
    public Transaction affected(Collection<Statement> statements) {
        super.getBuilder().add(statements, Transactions.GRAPH_AFFECTED);
        return this;
    }

    public Transaction affected(Stream<Statement> statements) {
        return this.affected(statements.toList());
    }

    public Transaction affected(Resource subject, IRI predicate, Value value) {
        return this.affected(SimpleValueFactory.getInstance().createStatement(subject, predicate, value));
    }





    public static boolean isTransaction(Model model) {
        return model.contains(null, RDF.TYPE, Transactions.TRANSACTION);
    }

    public List<Value> listModifiedResources(Activity ... activities) {
        List<Value> result = new ArrayList<>();
        Arrays.stream(activities).forEach(activity ->  {
            List<Value> values = super.streamStatements(transactionIdentifier, activity.toIRI(), null).map(Statement::getObject).toList();
            result.addAll(values);
        });
        return  result;
    }

    /**
     * We merge the named graphs of the transaction and affected model (but not the actual change itself)
     * @return
     */
    @Override
    public Iterable<NamespaceAwareStatement> asStatements() {
        return super.asStatements(Transactions.GRAPH_PROVENANCE, Transactions.GRAPH_AFFECTED);
    }

    public void setCompleted() {
        Model m = super.getBuilder().build();
        m.add(transactionIdentifier, Transactions.STATUS, Transactions.SUCCESS, Transactions.GRAPH_PROVENANCE);
        m.remove(transactionIdentifier, Transactions.STATUS, Transactions.RUNNING, Transactions.GRAPH_PROVENANCE);
    }

    public void setFailed(String message) {
        Model m = super.getBuilder().build();
        m.add(transactionIdentifier, Transactions.STATUS, Transactions.FAILURE, Transactions.GRAPH_PROVENANCE);
        m.remove(transactionIdentifier, Transactions.STATUS, Transactions.RUNNING, Transactions.GRAPH_PROVENANCE);

    }

    public Mono<Transaction> asMono() {
        return Mono.just(this);
    }

    public IRI getIdentifier() {
        return this.transactionIdentifier;
    }


    private static class StatementComparator implements Comparator<Statement> {
        @Override
        public int compare(Statement o1, Statement o2) {
            return 0;
        }
    }
}
