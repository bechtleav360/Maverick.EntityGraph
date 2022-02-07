package com.bechtle.eagl.graph.model;

import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class GeneratedIdentifier extends SimpleIRI {
    private static final SecureRandom secureRandom;
    private static final RandomStringGenerator randomStringGenerator;
    private static final char[][] range = { {'a', 'z'}, {'0', '9'} };

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


    private String generateSecureRandom() {
        byte[] token = new byte[16];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }

    private String generateRandomString() {
        return randomStringGenerator.generate(16);
    }


    public static IRI get(String namespace) {
        return new GeneratedIdentifier(namespace);

    }
}
