package com.bechtle.eagl.graph.domain.model.extensions;

import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * The generated entity identifier needs to resolve, it should be in the form
 *
 * http://example.org/api/entities/{id}
 * http://example.org/api/transactions/{id}
 *
 * FIXME: should require the current id (either bnode or externally set) to create reproducible ids
 * FIXME: should also keep track of the original id (we should store this in the provenance)
 */
public class GeneratedIdentifier extends EntityIRI {
    private static final SecureRandom secureRandom;
    private static final RandomStringGenerator randomStringGenerator;
    private static final char[][] range = { {'a', 'z'}, {'0', '9'} };
    public static int LENGTH = 16;

    static {
        secureRandom = new SecureRandom();
        randomStringGenerator = new RandomStringGenerator.Builder()
                .withinRange(range)
                .build();
    }

    public GeneratedIdentifier(String namespace) {
        super(namespace);
        super.setLocalName(this.generateRandomString());
    }

    public GeneratedIdentifier(Namespace defaultNamespace) {
        this(defaultNamespace.getName());
    }

    /**
     *
     * @param obj, the IRI to check
     * @return true, if the given resource conforms to a local identifier
     */
    public static boolean is(IRI obj, String ns) {
        return (obj instanceof GeneratedIdentifier)
                ||
               (obj.getNamespace().equalsIgnoreCase(ns))  && (obj.getLocalName().length() == LENGTH);
    }


    private String generateSecureRandom() {
        byte[] token = new byte[LENGTH];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }

    private String generateRandomString() {
        return randomStringGenerator.generate(LENGTH);
    }


    public static IRI get(String namespace) {
        return new GeneratedIdentifier(namespace);

    }
}
