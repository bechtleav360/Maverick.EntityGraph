package io.av360.maverick.graph.store.rdf.models;

import io.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.*;

import java.util.Set;

/**
 * A collection of statements in a request body. Might be named or not, might include one or more entities.
 */
public class StatementsBag extends AbstractModel {


    public StatementsBag() {
        super();
        super.getBuilder().setNamespace(Local.Entities.NS);
    }


    public Set<Resource> getSubjects() {
        return this.getModel().subjects();
    }

}
