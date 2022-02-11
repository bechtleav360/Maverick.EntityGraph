package com.bechtle.eagl.graph.domain.model.extensions;

import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.ICAL;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Namespaces;

import java.util.*;

/**
 *
 */
public class NamespacedModelBuilder extends ModelBuilder {

    private final static Map<String, Namespace> namespaceMap;

    static {
        namespaceMap = new HashMap<>(Namespaces.DEFAULT_RDF4J.size()+10);
        Namespaces.DEFAULT_RDF4J.forEach(namespace -> namespaceMap.put(namespace.getName(), namespace));
        namespaceMap.put(Local.Entities.NAMESPACE, Local.Entities.NS);
        namespaceMap.put(Local.Transactions.NAMESPACE, Local.Transactions.NS);
        namespaceMap.put(Local.Versions.NAMESPACE, Local.Versions.NS);

        namespaceMap.put(ICAL.NAMESPACE, ICAL.NS);
    }

    public NamespacedModelBuilder() {
        super();
    }

    public NamespacedModelBuilder(Model model, Set<Namespace> customNamespaces) {
        super(model);
        customNamespaces.forEach(namespace -> namespaceMap.put(namespace.getName(), namespace));
    }


    public NamespacedModelBuilder with(Model model) {
        model.getNamespaces().forEach(super::setNamespace);
        model.forEach(st -> super.build().add(st));
        return this;
    }

    public NamespacedModelBuilder add(Statement st) {
        super.build().add(st.getSubject(), st.getPredicate(), st.getObject());
        return this;
    }


    public ModelBuilder add(Resource subject, IRI predicate, Object object) {
        if(subject.isIRI()) {
            this.registerNamespace(((IRI) subject).getNamespace());
        }

        this.registerNamespace(predicate.getNamespace());

        if(object instanceof IRI) {
            this.registerNamespace(((IRI) object).getNamespace());
        }

        return super.add(subject, predicate, object);
    }

    private void registerNamespace(String namespace) {
        if(namespaceMap.containsKey(namespace)) super.setNamespace(namespaceMap.get(namespace));
    }


}
