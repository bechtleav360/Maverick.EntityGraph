package org.av360.maverick.graph.feature.applications.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.store.InvalidStoreConfiguration;
import org.av360.maverick.graph.store.FragmentsStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.config.DefaultRdfRepositoryBuilder;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRdfRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Replaces the @see DefaultRepositoryBuilder
 */
@Component
@Slf4j(topic = "graph.feat.app.repo.builder")
@Primary
public class ApplicationRdfRepositoryBuilder extends DefaultRdfRepositoryBuilder {


    /**
     * Initializes the connection to a repository. The connections are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Mono<LabeledRepository> buildRepository(FragmentsStore store, Environment env) {
        if (store instanceof AbstractRdfRepository repository) {
            Validate.isTrue(env.isAuthorized(), "Unauthorized operation in environment %s".formatted(env));
            Validate.notNull(env.getRepositoryType(), "Repository type is not set in environment %s".formatted(env));
            Validate.notBlank(env.getRepositoryType().toString(), "Repository type is empty string in environment %s".formatted(env));

            boolean is_default_repository_type = env.getRepositoryType().equals(RepositoryType.APPLICATION) || env.getRepositoryType().equals(RepositoryType.SCHEMA);
            boolean has_no_scope_defined = !env.hasScope();

            try {
                LabeledRepository labeledRepository = null;
                if (is_default_repository_type || has_no_scope_defined) {
                    labeledRepository = super.buildDefaultRepository(repository, env);
                } else {
                    labeledRepository = this.buildApplicationRepository(repository, env);

                }

                return super.validateRepository(labeledRepository, store, env);
            } catch (IOException e) {
                return Mono.error(e);
            }
        } else
            return Mono.error(new InvalidStoreConfiguration("Store of type %s not supported by for building a RDF repository.".formatted(store.getClass().getSimpleName())));








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

    private LabeledRepository buildApplicationRepository(AbstractRdfRepository store, Environment environment) throws IOException {
        Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).isPresent(), "Missing configuration in environment: Persistence flag");
        Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC).isPresent(), "Missing configuration in environment: Public flag");
        Validate.isTrue(environment.getConfiguration(Environment.RepositoryConfigurationKey.KEY).isPresent(), "Missing configuration in environment: Unique key for scope");
        if (Boolean.parseBoolean(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).get())) {
            // TODO: either default path is set, or application has a configuration set for the path. For now we expect the default path
            Validate.notBlank(store.getDirectory(), "No default storage directory defined for persistent application");
        }

        Validate.notNull(environment.getScope());

        String label = super.formatRepositoryLabel(environment);
        if (Objects.nonNull(this.meterRegistry)) {
            meterRegistry.counter("graph.store.repository", "method", "access", "label", label).increment();
        }


        if (environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).map(Boolean::parseBoolean).orElse(false)) {
            Path path = Paths.get(store.getDirectory(), environment.getConfiguration(Environment.RepositoryConfigurationKey.KEY).get());
            return super.getCache().get(label, s -> super.initializePersistentRepository(path, label));

        } else {
            return super.getCache().get(label, s -> super.initializeVolatileRepository(label));
        }


    }

}
