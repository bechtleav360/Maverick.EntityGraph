package cougar.graph.store.rdf.models;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Represents a named graph of one particular entity.
 *
 * Stores all items for one particular entity.
 *
 *
 *
 */
public class Entity extends AbstractModel {


    public Entity(Model model) {
        super(model);
    }

    public Entity() {
        super();
    }

    public Entity withResult(RepositoryResult<Statement> statements) {
        statements.stream().parallel().forEach(statement -> this.getBuilder().add(statement.getSubject(), statement.getPredicate(), statement.getObject()));
        return this;
    }
}
