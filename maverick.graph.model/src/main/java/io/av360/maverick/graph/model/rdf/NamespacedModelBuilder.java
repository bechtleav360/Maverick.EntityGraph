package io.av360.maverick.graph.model.rdf;

import io.av360.maverick.graph.model.vocabulary.ICAL;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.model.vocabulary.SDO;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Namespaces;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NamespacedModelBuilder extends ModelBuilder {

    private final static Map<String, Namespace> namespaceMap;

    static {
        namespaceMap = new HashMap<>(Namespaces.DEFAULT_RDF4J.size() + 10);
        Namespaces.DEFAULT_RDF4J.forEach(namespace -> namespaceMap.put(namespace.getName(), namespace));
        namespaceMap.put(Local.Entities.NAMESPACE, Local.Entities.NS);
        namespaceMap.put(Local.Transactions.NAMESPACE, Local.Transactions.NS);
        namespaceMap.put(Local.Versions.NAMESPACE, Local.Versions.NS);
        namespaceMap.put(SDO.NAMESPACE, SDO.NS);
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
        super.build().addAll(model);
        return this;
    }

    public NamespacedModelBuilder add(Collection<Statement> statements) {
        super.build().addAll(statements);
        return this;
    }

    public NamespacedModelBuilder add(Collection<Statement> statements, Resource... contexts) {
        statements.forEach(sts -> {
            super.build().add(sts.getSubject(), sts.getPredicate(), sts.getObject(), contexts);
        });
        return this;
    }

    public NamespacedModelBuilder add(Statement st) {
        super.build().add(st);
        return this;
    }


    public ModelBuilder add(Resource subject, IRI predicate, Object object) {
        if (subject.isIRI()) {
            this.registerNamespace(((IRI) subject).getNamespace());
        }

        this.registerNamespace(predicate.getNamespace());

        if (object instanceof IRI) {
            this.registerNamespace(((IRI) object).getNamespace());
        }

        return super.add(subject, predicate, object);
    }

    private void registerNamespace(String namespace) {
        if (namespaceMap.containsKey(namespace)) super.setNamespace(namespaceMap.get(namespace));
    }


}
