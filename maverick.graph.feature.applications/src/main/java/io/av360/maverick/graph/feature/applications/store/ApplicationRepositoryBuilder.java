package io.av360.maverick.graph.feature.applications.store;

import io.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.feature.applications.security.SubscriptionToken;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.model.security.RequestDetails;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf.LabeledRepository;
import io.av360.maverick.graph.store.rdf4j.config.DefaultRepositoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
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
    @Value("${application.storage.default.path: #{null}}")
    private String applicationsPath;



    public ApplicationRepositoryBuilder() {
        super();
    }


    /**
     * Initializes the connection to a repository. The connections are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @param repositoryType the type, e.g. for schema or entities
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Mono<Repository> buildRepository(RepositoryType repositoryType, Authentication authentication) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .flatMap(application -> buildRepository(repositoryType, authentication, application))
                .switchIfEmpty(super.buildRepository(repositoryType, authentication));
    }

    public Mono<Repository> buildRepository(RepositoryType repositoryType, Authentication authentication, Application requestedApplication) {
        if(Objects.isNull(authentication)) return Mono.error(new IllegalArgumentException("Failed to resolve repository due to missing authentication"));
        if(! authentication.isAuthenticated()) return Mono.error(new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName()));

        if (authentication instanceof TestingAuthenticationToken) {
            String key = super.buildRepositoryLabel(repositoryType, requestedApplication.label(), "test");
            log.trace("Requested repository of type {} and label {}", repositoryType.toString(), key);
            return Mono.just(super.getCache().get(key, s -> new LabeledRepository(key, new SailRepository(new MemoryStore()))));
        } else
        if (authentication instanceof SubscriptionToken subscriptionToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            return this.resolveRepositoryForApplicationAuthentication(repositoryType, requestedApplication.label(), subscriptionToken);
        }




        else return Mono.empty();
    }



    private Mono<Repository> resolveRepositoryForApplicationAuthentication(RepositoryType repositoryType, String requestedApplication, SubscriptionToken authentication)  {
        Assert.isTrue(Authorities.satisfies(Authorities.READER, authentication.getAuthorities()), "Missing authorization: " + Authorities.READER.getAuthority());
        Assert.isTrue(authentication.getApplication().label().equalsIgnoreCase(requestedApplication), "Subscription token is not configured for requested application "+requestedApplication);

        log.trace("Resolving repository with application authentication.");
        return switch (repositoryType) {
            case ENTITIES -> this.getRepository(RepositoryType.ENTITIES, authentication.getApplication());
            case TRANSACTIONS -> this.getRepository(RepositoryType.TRANSACTIONS, authentication.getApplication());
            case SCHEMA -> this.getDefaultRepository(RepositoryType.SCHEMA);
            // application is left out.. we cannot access the applications details with a user authorization only
            default -> Mono.error(new IOException(String.format("Invalid Repository Type '%s' for application context", repositoryType)));
        };
    }

    private Mono<Repository> resolveRepositoryForSystemAuthentication(RepositoryType repositoryType, Authentication authentication) {
        if (Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities()) && authentication instanceof SubscriptionToken) {
                log.trace("Resolving repository with admin authentication and additional subscription key.");
                Application application = ((SubscriptionToken) authentication).getApplication();
                return switch (repositoryType) {
                    case ENTITIES -> this.getRepository(RepositoryType.ENTITIES, application);
                    case TRANSACTIONS -> this.getRepository(RepositoryType.TRANSACTIONS, application);
                    case APPLICATION -> this.getDefaultRepository(RepositoryType.APPLICATION);
                    case SCHEMA -> this.getDefaultRepository(RepositoryType.SCHEMA);
                };
        } else {
            if(authentication instanceof AnonymousAuthenticationToken) {
                log.trace("Resolving repository with anonymous authentication with read access. Entities and transactions are default (probably in-memory) .");
            } else {
                log.trace("Resolving repository with admin token without additional subscription key. Entities and transactions are default (probably in-memory) .");
            }

            return this.getDefaultRepository(repositoryType);
        }
    }


    private Mono<Repository> getDefaultRepository(RepositoryType repositoryType) {
       return this.getRepository(repositoryType, "default");
    }

    private Mono<Repository> getRepository(RepositoryType repositoryType, @Nullable Application application) {
        if(Objects.isNull(application)) return getRepository(repositoryType, "default");
        else {
            String key = buildRepositoryLabel(repositoryType, application.key());
            return Mono.just(super.getCache().get(key, s -> new LabeledRepository(key, this.buildApplicationsRepository(this.applicationsPath, application, repositoryType))));
        }

    }








    private Repository buildApplicationsRepository(String basePath,Application application, RepositoryType repositoryType) {
        if (!application.flags().isPersistent() || !StringUtils.hasLength(basePath)) {
            log.debug("Initializing volatile {} repository for application '{}' [{}]", repositoryType.toString(), application.label(), application.key());
            return super.initializeVolatileRepository();
        } else {
            Path path = Paths.get(basePath, application.key(), repositoryType.toString(), "lmdb");
            return this.initializePersistentRepository(path);
        }
    }



    private String resolveRequestedApplication(Authentication authentication) {
        if(authentication.getDetails() instanceof RequestDetails requestDetails) {
            Objects.requireNonNull(requestDetails.path());

            int modifierIndex = requestDetails.path().indexOf("/app/");
            if(modifierIndex > 0) {
                String substring = requestDetails.path().substring(modifierIndex+5);
                return substring.substring(0, substring.indexOf("/"));
            } else return "default";
        } else {
            return "default";
        }
    }

}
