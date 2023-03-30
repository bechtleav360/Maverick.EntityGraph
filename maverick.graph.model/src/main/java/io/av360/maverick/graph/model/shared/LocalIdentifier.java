package io.av360.maverick.graph.model.shared;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

public interface LocalIdentifier extends IRI {
    public static int LENGTH = 8;

    public static char PADDING_CHAR = 'x';

    /**
     * @param obj, the IRI to check
     * @return true, if the given resource conforms to a local identifier
     */
    public static boolean is(IRI obj, String ns) {
        return (obj instanceof LocalIdentifier)
                ||
                (obj.getNamespace().equalsIgnoreCase(ns)) && (obj.getLocalName().length() == LENGTH);
    }

    static void build(String namespace, Resource globalIdentifier) {

    }
}
