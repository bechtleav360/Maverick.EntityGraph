package org.av360.maverick.graph.feature.applications.decorators;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/***
 * The thing with the ids: to support navigation by links, we have to encode the scope within the identifier
 * The entity's localname with the id "xewr32s" becomes "example.xewr32s"
 *
 * Computing the Identifier is done by this service. In default mode, it will simply return the identifier.
 * In application mode, it will prefix the current application.
 *
 * Reading always expects an unprefixed identifier (this is an internal thing only), the application label is extracted either
 * from the path (e.g. /api/s/example/xewr32s) or header. Reading calls the validate method here.
 *

 */
@Slf4j(topic = "feat.app.delegates.id")
public class DelegatingIdentifierServices implements IdentifierServices {

    private final IdentifierServices delegate;
    private SimpleValueFactory valueFactory;

    public DelegatingIdentifierServices(IdentifierServices delegate) {
        this.delegate = delegate;
        this.valueFactory = SimpleValueFactory.getInstance();
    }


    @Override
    public Mono<String> validate(String identifier, Environment environment) {
        if(identifier.contains(".")) return Mono.just(identifier);

        if(environment.hasScope()) {
            return Mono.just(String.format("%s.%s", environment.getScope().label(), identifier));
        } else return delegate.validate(identifier, environment);

    }

    @Override
    public Mono<IRI> asIRI(String key, String namespace, Environment environment) {
        return this.validate(key, environment)
                .flatMap(validatedKey -> delegate.asIRI(validatedKey, namespace, environment));
    }


    @Override
    public Mono<IRI> asReproducibleIRI(String namespace, Environment environment, Serializable... parts) {
        LocalIdentifier identifier = IdentifierServices.createReproducibleIdentifier(namespace, parts);
        return this.validate(identifier.getLocalName(), environment)
                .map(validatedKey -> valueFactory.createIRI(namespace, validatedKey));



    }
    @Override
    public Mono<IRI> asRandomIRI(String namespace, Environment environment) {
        LocalIdentifier identifier = IdentifierServices.createRandomIdentifier(namespace);
        return this.validate(identifier.getLocalName(), environment)
                .map(validatedKey -> valueFactory.createIRI(namespace, validatedKey));

    }

}
