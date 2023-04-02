package org.av360.maverick.graph.model.identifier;


import org.springframework.stereotype.Service;

import java.io.Serializable;
@Service
public class DefaultIdentifierFactory implements IdentifierFactory  {

    private static final DefaultIdentifierFactory sharedInstance = new DefaultIdentifierFactory();
    public static DefaultIdentifierFactory getInstance() {
        return sharedInstance;
    }


    @Override
    public LocalIdentifier createRandomIdentifier(String namespace) {
        return new RandomIdentifier(namespace);
    }

    @Override
    public LocalIdentifier createReproducibleIdentifier(String namespace, Serializable ... parts) {
        return new ChecksumIdentifier(namespace, parts);
    }
}
