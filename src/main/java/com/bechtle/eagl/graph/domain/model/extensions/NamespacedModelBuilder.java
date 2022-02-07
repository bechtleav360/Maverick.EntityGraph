package com.bechtle.eagl.graph.domain.model.extensions;

import com.bechtle.eagl.graph.domain.model.vocabulary.Default;
import com.bechtle.eagl.graph.domain.model.vocabulary.ICAL;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Namespaces;

import java.util.*;

/**
 *
 */
public class NamespacedModelBuilder extends ModelBuilder {

    private static Map<String, Namespace> namespaceMap;

    static {
        namespaceMap = new HashMap<>(Namespaces.DEFAULT_RDF4J.size()+10);
        Namespaces.DEFAULT_RDF4J.forEach(namespace -> namespaceMap.put(namespace.getName(), namespace));
        namespaceMap.put(Default.NAMESPACE, Default.NS);
        namespaceMap.put(ICAL.NAMESPACE, ICAL.NS);
    }

    public NamespacedModelBuilder() {
        super();
    }

    public NamespacedModelBuilder(Model model, Set<Namespace> customNamespaces) {
        super(model);
        customNamespaces.forEach(namespace -> namespaceMap.put(namespace.getName(), namespace));
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
