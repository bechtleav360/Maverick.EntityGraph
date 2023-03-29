package io.av360.maverick.graph.model.shared;

import com.google.common.hash.Hashing;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The generated entity identifier needs to resolve, it should be in the form
 * <p>
 * http://example.org/api/entities/{id}
 * http://example.org/api/transactions/{id}
 * <p>
 * FIXME: should require the current id (either bnode or externally set) to create reproducible ids
 * FIXME: should also keep track of the original id (we should store this in the provenance)
 */
public class RandomIdentifier extends LocalIRI implements LocalIdentifier {
    private static final SecureRandom secureRandom;
    private static final RandomStringGenerator randomStringGenerator;
    private static final char[][] range = {{'a', 'z'}, {'0', '9'}};


    static {
        secureRandom = new SecureRandom();
        randomStringGenerator = new RandomStringGenerator.Builder()
                .withinRange(range)
                .build();
    }

    public RandomIdentifier(String namespace) {
        super(namespace);
        super.setLocalName(generateRandomKey());
    }


    public RandomIdentifier(String namespace, Resource oldIdentifier) {
        super(namespace);

        if (oldIdentifier.isIRI()) {
            super.setLocalName(generateDerivedIdentifier(((IRI) oldIdentifier).getLocalName()));
        } else {
            super.setLocalName(generateRandomKey());
        }

    }


    public RandomIdentifier(Namespace defaultNamespace) {
        this(defaultNamespace.getName());
    }

    /**
     * @param obj, the IRI to check
     * @return true, if the given resource conforms to a local identifier
     */
    public static boolean is(IRI obj, String ns) {
        return (obj instanceof RandomIdentifier)
                ||
                (obj.getNamespace().equalsIgnoreCase(ns)) && (obj.getLocalName().length() == LENGTH);
    }


    private String generateSecureRandom() {
        byte[] token = new byte[LENGTH];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }

    public synchronized static String generateRandomKey() {
        return randomStringGenerator.generate(LENGTH);
    }

    public synchronized static String generateRandomKey(int length) {
        return randomStringGenerator.generate(length);
    }

    public synchronized static String generateDerivedIdentifier(String localName) {
        String s = Hashing.fingerprint2011().hashString(localName, StandardCharsets.UTF_8).toString();

        if (s.length() < LENGTH) s = s.concat(s);
        return s.substring(0, LENGTH);
    }

    public static IRI get(String namespace) {
        return new RandomIdentifier(namespace);

    }


}
