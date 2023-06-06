package org.av360.maverick.graph.feature.applications.store;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.av360.maverick.graph.feature.applications.domain.model.Application.CONFIG_KEYS;
import org.av360.maverick.graph.feature.applications.security.SubscriptionToken;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.RequestDetails;
import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.config.DefaultRepositoryBuilder;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Replaces the @see DefaultRepositoryBuilder
 */
@SuppressWarnings("SuspiciousMethodCalls")
@Component
@Slf4j(topic = "graph.feat.app.repo.builder")
@Primary
public class ApplicationRepositoryBuilder extends DefaultRepositoryBuilder {


    public ApplicationRepositoryBuilder(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }


    /**
     * Initializes the connection to a repository. The connections are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Mono<LabeledRepository> buildRepository(TripleStore store, Authentication authentication) {
            try {
                Assert.notNull(authentication, "Failed to resolve repository due to missing authentication");
                Assert.notNull(authentication.getDetails(), "No details object in authentication of type: %s".formatted(authentication.getClass()));
                Assert.isTrue(authentication.getDetails() instanceof RequestDetails, "Details object in authentication of class %s of wrong type: %s".formatted(authentication.getClass(), authentication.getDetails()));
                if (!authentication.isAuthenticated()) throw new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName());

                Map<String, String> configuration = ((RequestDetails) authentication.getDetails()).getConfiguration();
                if(configuration.isEmpty() || ! configuration.containsKey(CONFIG_KEYS.LABEL.toString())) {
                    return super.buildRepository(store, authentication);
                }
                String appLabel = configuration.get(CONFIG_KEYS.LABEL.toString());
                if (StringUtils.hasLength(appLabel)) {
                    return this.buildApplicationRepository(store, authentication, configuration);
                } else {
                    return super.buildRepository(store, authentication);
                }

            } catch (Exception e) {
                log.error("Failed to build repository of type '{}' due to error: {}", store.getRepositoryType(), e.getMessage());
                return Mono.error(e);
            }
    }

    public Mono<LabeledRepository> buildApplicationRepository(TripleStore store, Authentication authentication, Map<String, String> appConfig) {
        Assert.isTrue(appConfig.containsKey(CONFIG_KEYS.LABEL.toString()), "Missing application configuration: label");
        Assert.isTrue(appConfig.containsKey(CONFIG_KEYS.KEY.toString()), "Missing application configuration: key");
        Assert.isTrue(appConfig.containsKey(CONFIG_KEYS.FLAG_PERSISTENT.toString()), "Missing application configuration: flag persistent ");
        Assert.isTrue(appConfig.containsKey(CONFIG_KEYS.FLAG_PUBLIC.toString()), "Missing application configuration: flag public");

        try {
            LabeledRepository repository = null;
            if (authentication instanceof TestingAuthenticationToken) {
                repository = this.buildApplicationsRepository(store, appConfig, "test");
            } else if (authentication instanceof SubscriptionToken subscriptionToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
                repository = this.resolveRepositoryForApplicationAuthentication(store, appConfig, subscriptionToken);
            } else if (authentication instanceof AdminToken adminToken) {
                repository = this.resolveRepositoryForSystemAuthentication(store, appConfig, adminToken);
            } else if (authentication instanceof AnonymousAuthenticationToken) {
                repository = this.resolveRepositoryForAnonymousAuthentication(store, appConfig);
            } else if (authentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
                repository = this.resolveRepositoryForSystemAuthentication(store, appConfig, authenticationToken);
            }

            return Mono.just(super.validateRepository(repository, store, authentication));
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private LabeledRepository resolveRepositoryForAnonymousAuthentication(TripleStore store, Map<String, String> appConfig) throws IOException {
        if (!Boolean.parseBoolean(appConfig.get(CONFIG_KEYS.FLAG_PUBLIC.toString()))) {
            throw new InsufficientPrivilegeException("Requested application does not exist or is not public.");
        }
        log.trace("Resolving public repository for application '{}' with anonymous authentication.", appConfig.get(CONFIG_KEYS.LABEL.toString()));
        return this.buildApplicationsRepository(store, appConfig);

    }


    private LabeledRepository resolveRepositoryForApplicationAuthentication(TripleStore store, Map<String, String> appConfig, SubscriptionToken authentication) throws IOException {
        Assert.isTrue(Authorities.satisfies(Authorities.READER, authentication.getAuthorities()), "Missing authorization: " + Authorities.READER.getAuthority());
        Assert.isTrue(authentication.getApplication().label().equalsIgnoreCase(appConfig.get(CONFIG_KEYS.LABEL.toString())), "Subscription token is not configured for requested application: %s".formatted(appConfig.get(CONFIG_KEYS.LABEL)));

        log.trace("Resolving repository with application authentication.");
        return this.buildApplicationsRepository(store, appConfig);
    }

    private LabeledRepository resolveRepositoryForSystemAuthentication(TripleStore store, Map<String, String> appConfig, Authentication authentication) throws IOException {
        if (!Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities()))
            throw new InsufficientPrivilegeException("Authorization issue while resolving repository.");

        log.trace("Resolving repository with admin authentication and additional subscription key.");
        return this.buildApplicationsRepository(store, appConfig);
    }


    private LabeledRepository buildApplicationsRepository(TripleStore store, Map<String, String> appConfig, String... details) {
        String label = super.formatRepositoryLabel(store.getRepositoryType(), ArrayUtils.addAll(new String[]{appConfig.get(CONFIG_KEYS.LABEL.toString())}, details));
        meterRegistry.counter("graph.store.repository", "method", "access", "label", label).increment();

        if (!Boolean.parseBoolean(appConfig.get(CONFIG_KEYS.FLAG_PERSISTENT.toString()))) {
            log.debug("Resolving in-memory repository for application with label '{}'", appConfig.get(CONFIG_KEYS.LABEL.toString()));
            return super.getCache().get(label, s -> super.initializeVolatileRepository(label));

        } else {
            Path path = Paths.get(store.getDirectory(), appConfig.get(CONFIG_KEYS.KEY.toString()));
            return super.getCache().get(label, s -> super.initializePersistentRepository(path, label));
        }
    }


}
