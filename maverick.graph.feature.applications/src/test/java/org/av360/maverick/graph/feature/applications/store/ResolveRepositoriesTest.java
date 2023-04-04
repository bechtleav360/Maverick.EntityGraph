package org.av360.maverick.graph.feature.applications.store;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class ResolveRepositoriesTest {

    @Autowired
    ApplicationRepositoryBuilder builder;


    @Test
    public void buildEntityRepoWithTestAuthentication() {
        Mono<Repository> mono = builder.buildRepository(RepositoryType.ENTITIES, TestSecurityConfig.createAuthenticationToken());

        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }

    @Test
    public void buildEntityRepoWithAdminAuthentication() {
        Mono<Repository> mono = builder.buildRepository(RepositoryType.ENTITIES, TestSecurityConfig.createAdminToken());

        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }

    @Test
    public void buildEntityRepoWithAnonAuthentication() {
        Mono<Repository> mono = Mono.just(TestSecurityConfig.createAnonymousToken())
                .flatMap(token -> builder.buildRepository(RepositoryType.ENTITIES, token));


        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }

    @Test
    public void buildAppEntityRepoWithTestAuthentication() {
        Application application = new Application(SimpleValueFactory.getInstance().createIRI("http://example.org/app"), "app", "123213", new ApplicationFlags(false, false, null, null, null));
        Mono<Repository> mono = builder.buildRepository(RepositoryType.ENTITIES, TestSecurityConfig.createAuthenticationToken(), application);


        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }

    @Test
    public void buildAppEntityRepoWithTestAuthenticatio2n() {
        Application application = new Application(SimpleValueFactory.getInstance().createIRI("http://example.org/app"), "app", "123213", new ApplicationFlags(false, false, null, null, null));


        Mono<Repository> mono = builder.buildRepository(RepositoryType.ENTITIES, TestSecurityConfig.createAdminToken())
                .contextWrite(ctx -> ctx.put(ReactiveApplicationContextHolder.CONTEXT_KEY, application));


        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }


}
