package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.Default;
import org.eclipse.rdf4j.model.*;

import java.util.ArrayList;
import java.util.Set;

/**
 * A collection of statements in a request body. Might be named or not, might include one or more entities.
 */
public class IncomingStatements extends AbstractModelWrapper<IncomingStatements> {


    public IncomingStatements() {
        super();
        super.getBuilder().setNamespace(Default.NS);
    }


    public void replaceAnonymousIdentifier(Resource subj) {

        IRI identifier = new GeneratedIdentifier(Default.NS);

        ArrayList<Statement> copy = new ArrayList<>(this.getModel());

        super.reset();

        for(Statement st : copy) {
            if(st.getSubject().equals(subj)) {
                super.getBuilder().add(identifier, st.getPredicate(), st.getObject());
            } else if(st.getObject().equals(subj)) {
                super.getBuilder().add(st.getSubject(), st.getPredicate(), identifier);
            } else {
                super.getBuilder().add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }
    }

    public Set<Resource> getSubjects() {
        return this.getModel().subjects();
    }
}
