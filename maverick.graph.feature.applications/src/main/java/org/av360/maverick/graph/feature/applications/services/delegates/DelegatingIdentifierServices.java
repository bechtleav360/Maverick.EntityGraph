package org.av360.maverick.graph.feature.applications.services.delegates;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Collection;

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
    public String validate(String identifier, Environment environment) {
        if(identifier.contains(".")) return identifier;

        if(environment.hasScope()) {
            return String.format("%s.%s", environment.getScope().label(), identifier);
        } else return delegate.validate(identifier, environment);

    }


    @Override
    public Mono<IRI> asReproducibleLocalIRI(String namespace, Environment environment, Collection<Serializable> parts) {
        IRI identifier = IdentifierServices.buildReproducibleIRI(namespace, parts);
        String scopedKey =  this.validate(identifier.getLocalName(), environment);
        return Mono.just(Values.iri(identifier.getNamespace(), scopedKey));
    }



    @Override
    public Mono<IRI> asRandomLocalIRI(String namespace, Environment environment) {
        IRI identifier = IdentifierServices.buildRandomIRI(namespace);
        String scopedKey =  this.validate(identifier.getLocalName(), environment);
        return Mono.just(Values.iri(identifier.getNamespace(), scopedKey));
    }

}
