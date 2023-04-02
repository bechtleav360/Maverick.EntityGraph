package org.av360.maverick.graph.store.rdf.models;

import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.Resource;

import java.util.Set;

/**
 * A collection of statements in a request body. Might be named or not, might include one or more entities.
 */
public class TripleBag extends TripleModel {


    public TripleBag() {
        super();
        super.getBuilder().setNamespace(Local.Entities.NS);
    }


    public Set<Resource> getSubjects() {
        return this.getModel().subjects();
    }

}
