package org.av360.maverick.graph.model.identifier;

import org.eclipse.rdf4j.model.IRI;

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

}
