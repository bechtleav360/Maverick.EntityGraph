package org.av360.maverick.graph.feature.applications.services.delegates;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    public String validate(String key, Environment environment) {
        if(! StringUtils.hasLength(key)) throw new IllegalArgumentException("Missing identifier for validation.");

        if(environment.hasScope() && key.contains(".")) {
            String[] split = key.split("\\.");
            if(environment.getScope().label().equalsIgnoreCase(split[0])) {
                log.trace("Validating identifier {}: includes correct scope.", key);
                return key;
            } else {
                log.trace("Validating identifier {}: target scope {}, replacing scope {} in key.", key, environment.getScope().label(), split[1]);
                return String.format("%s.%s", environment.getScope().label(), split[1]);
            }
        } else if(! environment.hasScope() && key.contains(".")) {
            String[] split = key.split(" \\.");
            log.trace("Validating identifier {}: target is default , removing scope {}.", key, split[0]);
            return String.format("%s.%s", environment.getScope().label(), split[1]);
        } else if(environment.hasScope() && ! key.contains(".")) {
            log.trace("Validating identifier {}: adding target scope {}.", key, environment.getScope().label());
            return String.format("%s.%s", environment.getScope().label(), key);
        }

        else return delegate.validate(key, environment);

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

    @Override
    public IRI validateIRI(IRI identifier, Environment environment, @Nullable String endpoint, @Nullable Map<String, String> headers) {
        /* key - source_scope - target_scope -> result
         *
         * 1) if target scope matches source scope (and source scope matches X-Application header)
         * urn:pwid:meg:scope_x.123 - scope_x - scope_x -> urn:pwid:meg:scope_x.123
         *
         * 2) if target (env scope) is different from key scope and key scope  matches header scope
         * urn:pwid:meg:scope_y.123 - scope_y - scope_x -> urn:pwid:meg:scope_x.123
         *
         * 3) if env scope is different from key scope and key scope does not match header scope
         * urn:pwid:meg:scope_z.123 - scope_y - scope_x -> http://remote.instance.com/api/entities/s/z/123  -
         *
         * 4) https URLs will be ignored, they have to be handled elsewhere (transformed to local iri with owl:sameas link)
         * http://remote.endpoint.com -> http://remote.endpoint.com -
         *
         * 5) if identifier is unscoped
         * urn:pwid:meg:123 - scope_x - scope_x -> http://remote.instance.com/api/entities/123
         *
         * 6) identifier is known internal type
         */

        try {
            // 4) https URLs will be ignored, they have to be handled elsewhere (transformed to local iri with owl:sameas link)
            if(! identifier.getNamespace().startsWith("urn")) return identifier;
            // 6) identifier is known internal type
            if(Local.Entities.TYPE_INDIVIDUAL.equals(identifier)) { return identifier;}
            if(Local.Entities.TYPE_CLASSIFIER.equals(identifier)) return identifier;
            if(Local.Entities.TYPE_EMBEDDED.equals(identifier)) return identifier;

            Optional<String> header_scope = Objects.isNull(headers) ? Optional.empty() : Optional.of(headers.getOrDefault(Globals.HEADER_APPLICATION_LABEL, null));
            Optional<String> env_scope = environment.hasScope() ? Optional.of(environment.getScope().label()) : Optional.empty();
            Optional<String> key_scope = IdentifierServices.getScopeFromKey(identifier.getLocalName());
            Optional<String> host = Objects.isNull(endpoint) ? Optional.empty() : Optional.of(new URIBuilder(endpoint).removeQuery().setPath("/api/entities/").toString());


            // 1) if (target scope from env matches key scope) and (key scope matches source scope from header): we leave the scope
            if(env_scope.isPresent() && header_scope.isPresent() && key_scope.isPresent() && env_scope.get().equalsIgnoreCase(key_scope.get()) && key_scope.get().equalsIgnoreCase(header_scope.get())) {
                IRI result = Values.iri(environment.getRepositoryType().getIdentifierNamespace(), identifier.getLocalName());
                return result;
            }

            //  2) if target (env scope) is different from key scope and key scope matches header scope): we convert the scope
            if(env_scope.isPresent() && header_scope.isPresent() && key_scope.isPresent() && ! env_scope.get().equalsIgnoreCase(key_scope.get()) && key_scope.get().equalsIgnoreCase(header_scope.get())) {
                IRI result = Values.iri(environment.getRepositoryType().getIdentifierNamespace(), this.validate(identifier.getLocalName(), environment));
                log.trace("Converted local identifier {} with known scope to target scoped identifier {}>", identifier, result);
                return result;
            }

            // 3a) if env scope is different from key scope and key scope does not match header scope (host is present)
            if(env_scope.isPresent() && header_scope.isPresent() && key_scope.isPresent() && host.isPresent() && ! env_scope.get().equalsIgnoreCase(key_scope.get()) && ! key_scope.get().equalsIgnoreCase(header_scope.get())) {

                String newHost = "%s/s/%s".formatted(host.get(), key_scope.get());
                IRI result = Values.iri(newHost, identifier.getLocalName());
                log.trace("Converted local identifier {} with unknown scope into external IRI <{}>", identifier, result);
                return result;
            }

            // 3b) if env scope is different from key scope and key scope does not match header scope (host is not present)
            if(env_scope.isPresent() && header_scope.isPresent() && key_scope.isPresent() && host.isEmpty() && ! env_scope.get().equalsIgnoreCase(key_scope.get()) && ! key_scope.get().equalsIgnoreCase(header_scope.get())) {
                log.error("Cannot convert identifier {}, since target host is missing to resolve the identifier into an URI", identifier);
                return identifier;
            }

            // 5a) if identifier is unscoped but target is scoped
            if(key_scope.isEmpty() && env_scope.isPresent() && host.isPresent()) {
                IRI result = Values.iri(host.get(), identifier.getLocalName());
                log.trace("Converted local identifier {} with no scope into external IRI <{}>, since targed is scoped", identifier, result);
                return result;
            }
            // 5a) if identifier is unscoped but target is scoped
            if(key_scope.isEmpty() && (env_scope.isEmpty() || env_scope.get().equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL))) {
                IRI result = Values.iri(environment.getRepositoryType().getIdentifierNamespace(), identifier.getLocalName());
                return result;
            }


            log.error("Failed to handle identifier: {}", identifier);
            header_scope.ifPresent(str -> log.warn("Source scope in header: {}", str));
            key_scope.ifPresent(str -> log.warn("Source scope in key: {}", str));
            env_scope.ifPresent(str -> log.warn("Target scope in environment: {}", str));
            throw new RuntimeException("Failed to handle identifier: %s".formatted(identifier));

        } catch (URISyntaxException e) {
            log.warn("Failed to parse endpoint: {}", endpoint);
            throw new RuntimeException(e);
        }


    }

}
