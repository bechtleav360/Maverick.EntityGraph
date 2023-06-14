package org.av360.maverick.graph.feature.applications.store;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.config.DefaultRepositoryBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public Mono<LabeledRepository> buildRepository(TripleStore store, SessionContext context) {
        try {
            Environment environment = context.getEnvironment();

            // TODO: we have to assume that the validation if the current authentication has access to this particular application repository was handled before.

            Validate.isTrue(context.getAuthentication().isPresent());
            Validate.isTrue(context.getAuthentication().get().isAuthenticated());
            Validate.notNull(context.getEnvironment().getRepositoryType());
            Validate.notBlank(context.getEnvironment().getRepositoryType().toString());

            boolean is_default_repository_type = environment.getRepositoryType().equals(RepositoryType.APPLICATION) || context.getEnvironment().getRepositoryType().equals(RepositoryType.SCHEMA);
            boolean has_no_scope_defined = ! environment.hasScope();

            if(is_default_repository_type || has_no_scope_defined) {
                return super.buildRepository(store, context);
            } else {
                return Mono.just(this.buildApplicationRepository(store, context.getEnvironment()));
            }

        } catch (Exception e) {
            return  Mono.error(e);
        }




        /*
            try {
                Authentication authentication = context.getAuthenticationOrThrow();

                Assert.notNull(authentication.getDetails(), "No details object in authentication of type: %s".formatted(authentication.getClass()));
                Assert.isTrue(authentication.getDetails() instanceof RequestDetails, "Details object in authentication of class %s of wrong type: %s".formatted(authentication.getClass(), authentication.getDetails()));
                if (!authentication.isAuthenticated()) throw new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName());

                Map<String, String> configuration = ((RequestDetails) authentication.getDetails()).getConfiguration();
                if(configuration.isEmpty() || ! configuration.containsKey(CONFIG_KEYS.LABEL.toString())) {
                    return super.buildRepository(store, context);
                }
                String appLabel = configuration.get(CONFIG_KEYS.LABEL.toString());
                if (StringUtils.hasLength(appLabel)) {
                    return this.buildApplicationRepository(store, authentication, configuration);
                } else {
                    return super.buildRepository(store, context);
                }

            } catch (Exception e) {
                log.error("Failed to build repository of type '{}' due to error: {}", store.getRepositoryType(), e.getMessage());
                return Mono.error(e);
            }
            */
    }

    private LabeledRepository buildApplicationRepository(TripleStore store, Environment environment) throws IOException {
            Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).isPresent(), "Missing configuration in environment: Persistence flag");
            Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC).isPresent(), "Missing configuration in environment: Public flag");
            Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.KEY).isPresent(), "Missing configuration in environment: Unique key for scope");
            if(Boolean.parseBoolean(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).get())) {
                // TODO: either default path is set, or application has a configuration set for the path. For now we expect the default path
                Validate.notBlank(store.getDirectory(), "No default storage directory defined for persistent application");

            }

            Validate.notBlank(environment.getScope());

            String label = super.formatRepositoryLabel(environment);
            meterRegistry.counter("graph.store.repository", "method", "access", "label", label).increment();
            LabeledRepository labeledRepository = null;
            if(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).map(Boolean::parseBoolean).orElse(false)) {
                Path path = Paths.get(store.getDirectory(), environment.getConfiguration(Environment.RepositoryConfigurationKey.KEY).get());
                labeledRepository = super.getCache().get(label, s -> super.initializePersistentRepository(path, label));

            } else {
                labeledRepository = super.getCache().get(label, s -> super.initializeVolatileRepository(label));
            }

            return super.validateRepository(labeledRepository, store, environment);

    }




    /*
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

            return Mono.just(super.validateRepository(repository, store, null));
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

     */


}
