package com.bechtle.eagl.graph.connector.rdf4j.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

import java.security.SecureRandom;
import java.util.Base64;

public class GeneratedIdentifier extends EntityIdentifier {
    private static final SecureRandom secureRandom;

    static {
        secureRandom = new SecureRandom();
    }

    public GeneratedIdentifier(String namespace) {
        super.setNamespace(namespace);
        super.setLocalName(this.generate());
    }

    public GeneratedIdentifier(Namespace defaultNamespace) {
        this(defaultNamespace.getName());
    }


    private String generate() {
        byte[] token = new byte[16];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }


    public static IRI get(String namespace) {
        return new GeneratedIdentifier(namespace);

    }
}
