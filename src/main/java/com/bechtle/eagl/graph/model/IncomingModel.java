package com.bechtle.eagl.graph.model;

import com.bechtle.eagl.graph.connector.rdf4j.model.GeneratedIdentifier;
import com.bechtle.eagl.graph.connector.rdf4j.model.vocabulary.Default;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A collection of statements in a request body. Might be named or not, might include one or more entities.
 */
public class IncomingModel implements NamespaceAware {

    private ModelBuilder model;

    public IncomingModel() {
        this.model = new ModelBuilder();
        this.model.setNamespace(Default.NS);
    }


    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public Model getModel() {
        return model.build();
    }

    public Stream<NamespaceAwareStatement> stream() {
        return this.getModel().stream().map(sts -> NamespaceAwareStatement.wrap(sts, getNamespaces()));
    }


    public void generateName(Resource subj) throws IOException {

        IRI identifier = new GeneratedIdentifier(Default.NS);

        ArrayList<Statement> copy = new ArrayList<>(this.getModel());

        this.model = new ModelBuilder();

        for(Statement st : copy) {
            if(st.getSubject().equals(subj)) {
                model.add(identifier, st.getPredicate(), st.getObject());
            } else if(st.getObject().equals(subj)) {
                model.add(st.getSubject(), st.getPredicate(), identifier);
            } else {
                model.add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }
    }

    public ModelBuilder getBuilder() {
        return this.model;
    }
}
