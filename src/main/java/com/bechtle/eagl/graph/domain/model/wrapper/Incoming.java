package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import org.eclipse.rdf4j.model.*;

import java.util.Set;

/**
 * A collection of statements in a request body. Might be named or not, might include one or more entities.
 */
public class Incoming extends AbstractModel {


    public Incoming() {
        super();
        super.getBuilder().setNamespace(Local.Entities.NS);
    }




    public Set<Resource> getSubjects() {
        return this.getModel().subjects();
    }

}
