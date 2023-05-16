package org.av360.maverick.graph.feature.applications.store;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.security.SubscriptionToken;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.RequestDetails;
import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.config.DefaultRepositoryBuilder;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
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
import java.util.Objects;

/**
 * Replaces the @see DefaultRepositoryBuilder
 */
@Component
@Slf4j(topic = "graph.feat.app.repo.cfg.builder")
@Primary
public class ApplicationRepositoryBuilder extends DefaultRepositoryBuilder {


    public ApplicationRepositoryBuilder(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }


    /**
     * Initializes the connection to a repository. Txhe connections are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Mono<LabeledRepository> buildRepository(TripleStore store, Authentication authentication) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .flatMap(application -> {
                    try {
                        if(application.label().equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL)) {
                            return super.buildRepository(store, authentication);
                        } else {
                            return Mono.just(this.buildRepository(store, authentication, application));
                        }
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })

                .switchIfEmpty(Mono.defer(() -> {
                    if(authentication.getDetails() != null && authentication.getDetails() instanceof RequestDetails && ((RequestDetails) authentication.getDetails()).headers().containsKey("X-APPLICATION")) {
                        return Mono.error(new IOException("Application header provided, but not in context."));
                    }
                    return super.buildRepository(store, authentication);
                }));
    }

    public LabeledRepository buildRepository(TripleStore store, Authentication authentication, @NotNull Application requestedApplication) throws IOException {
        if (Objects.isNull(authentication))
            throw new IllegalArgumentException("Failed to resolve repository due to missing authentication");
        if (!authentication.isAuthenticated())
            throw new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName());


        LabeledRepository repository = null;
        if (authentication instanceof TestingAuthenticationToken) {
            repository = this.buildApplicationsRepository(store, requestedApplication, "test");
        } else if (authentication instanceof SubscriptionToken subscriptionToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            repository = this.resolveRepositoryForApplicationAuthentication(store, requestedApplication, subscriptionToken);
        } else if (authentication instanceof AdminToken adminToken) {
            repository = this.resolveRepositoryForSystemAuthentication(store, requestedApplication, adminToken);
        } else if (authentication instanceof AnonymousAuthenticationToken) {
            repository = this.resolveRepositoryForAnonymousAuthentication(store, requestedApplication);
        }else if (authentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
            repository = this.resolveRepositoryForSystemAuthentication(store, requestedApplication, authenticationToken);
        }

        return super.validateRepository(repository, store, authentication);
    }

    private LabeledRepository resolveRepositoryForAnonymousAuthentication(TripleStore store, @NotNull Application requestedApplication) throws IOException {
        if (!requestedApplication.flags().isPublic())
            throw new InsufficientAuthenticationException("Requested application does not exist or is not public.");

        log.trace("Resolving public repository for application '{}' with anonymous authentication.", requestedApplication.label());
        return this.buildApplicationsRepository(store, requestedApplication);

    }


    private LabeledRepository resolveRepositoryForApplicationAuthentication(TripleStore store, Application requestedApplication, SubscriptionToken authentication) throws IOException {
        Assert.isTrue(Authorities.satisfies(Authorities.READER, authentication.getAuthorities()), "Missing authorization: " + Authorities.READER.getAuthority());
        Assert.isTrue(authentication.getApplication().label().equalsIgnoreCase(requestedApplication.label()), "Subscription token is not configured for requested application " + requestedApplication);

        log.trace("Resolving repository with application authentication.");
        return this.buildApplicationsRepository(store, requestedApplication);
    }

    private LabeledRepository resolveRepositoryForSystemAuthentication(TripleStore store, @NotNull Application requestedApplication, Authentication authentication) throws IOException {
        if (!Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities()))
            throw new InsufficientAuthenticationException("Authorization issue while resolving repository.");

        log.trace("Resolving repository with admin authentication and additional subscription key.");
        return this.buildApplicationsRepository(store, requestedApplication);
    }


    private LabeledRepository buildApplicationsRepository(TripleStore store, @NotNull Application application, String... details) {
        String label = super.formatRepositoryLabel(store.getRepositoryType(), ArrayUtils.addAll(new String[]{application.label()}, details));
        meterRegistry.counter("graph.store.repository", "method", "access", "label", label).increment();

        if (!application.flags().isPersistent() || !StringUtils.hasLength(store.getDirectory())) {
            log.debug("Initializing volatile {} repository for application '{}' [{}]", label, application.label(), application.key());
            return super.getCache().get(label, s -> super.initializeVolatileRepository(label));

        } else {
            Path path = Paths.get(store.getDirectory(), application.key());
            return super.getCache().get(label, s -> super.initializePersistentRepository(path, label));
        }
    }


}
