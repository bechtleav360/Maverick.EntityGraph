package org.av360.maverick.graph.feature.applications.store;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class ResolveRepositoriesTest {

    @Autowired
    ApplicationRepositoryBuilder builder;

    @Autowired
    EntityStore entityStore;

    @Test
    public void buildEntityRepoWithTestAuthentication() throws IOException {
        Repository repository = builder.buildRepository(entityStore, TestSecurityConfig.createAuthenticationToken()).block();
        Assertions.assertTrue(repository.isInitialized());
    }

    @Test
    public void buildEntityRepoWithAdminAuthentication() throws IOException {
        Repository repository = builder.buildRepository(entityStore, TestSecurityConfig.createAdminToken()).block();
        Assertions.assertTrue(repository.isInitialized());
    }

    @Test
    public void buildEntityRepoWithAnonAuthentication() throws IOException {
        Repository repository = builder.buildRepository(entityStore, TestSecurityConfig.createAnonymousToken()).block();
        Assertions.assertTrue(repository.isInitialized());

    }

    @Test
    public void buildAppEntityRepoWithTestAuthentication() throws IOException {
        Application application = new Application(SimpleValueFactory.getInstance().createIRI("http://example.org/app"), "app", "123213", new ApplicationFlags(false, false));
        Repository repository = builder.buildRepository(entityStore, TestSecurityConfig.createAuthenticationToken(), application);
        Assertions.assertTrue(repository.isInitialized());
    }

    @Test
    public void buildAppEntityRepoWithTestAuthenticationWithContext() throws IOException {
        Application application = new Application(SimpleValueFactory.getInstance().createIRI("http://example.org/app"), "app", "123213", new ApplicationFlags(false, false));


        Mono<LabeledRepository> mono = builder.buildRepository(entityStore, TestSecurityConfig.createAdminToken())
                .contextWrite(ctx -> ctx.put(ReactiveApplicationContextHolder.CONTEXT_KEY, application));


        StepVerifier.create(mono).assertNext(Repository::isInitialized);
    }


}
