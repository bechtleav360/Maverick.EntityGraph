package org.av360.maverick.graph.feature.applications.store;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.model.domain.Application;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.config.DefaultRdfRepositoryBuilder;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.eclipse.rdf4j.repository.Repository;
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
    DefaultRdfRepositoryBuilder builder;

    @Autowired
    IndividualsStore entityStore;

    @Test
    public void buildEntityRepoWithTestAuthentication() throws IOException {

        Mono<LabeledRepository> mono = builder.getRepository(entityStore,  TestSecurityConfig.createTestContext().getEnvironment());
        StepVerifier.create(mono).assertNext(repo -> repo.isInitialized()).verifyComplete();
    }

    @Test
    public void buildEntityRepoWithAdminAuthentication() throws IOException {
        Mono<LabeledRepository> mono = builder.getRepository(entityStore, TestSecurityConfig.createAdminContext().getEnvironment());
        StepVerifier.create(mono).assertNext(Repository::isInitialized).verifyComplete();
    }

    @Test
    public void buildEntityRepoWithAnonAuthentication() throws IOException {
        Mono<LabeledRepository> mono = builder.getRepository(entityStore, TestSecurityConfig.createAnonymousContext().getEnvironment());
        StepVerifier.create(mono).assertNext(Repository::isInitialized).verifyComplete();

    }

    @Test
    public void buildAppEntityRepoWithTestAuthentication() throws IOException {
        // TODO: replace with s3 params

        SessionContext ctx = TestSecurityConfig.createTestContext();
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.LABEL, "app");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.KEY, "123213");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.FLAG_PUBLIC, "false");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.FLAG_PERSISTENT, "false");


        Mono<LabeledRepository> mono = builder.getRepository(entityStore, ctx.getEnvironment());
        StepVerifier.create(mono).assertNext(Repository::isInitialized).verifyComplete();

    }

    @Test
    public void buildAppEntityRepoWithTestAuthenticationWithContext() throws IOException {
        // TODO: replace with s3 params

        SessionContext ctx = TestSecurityConfig.createAdminContext();
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.LABEL, "app");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.KEY, "123213");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.FLAG_PUBLIC, "false");
        TestSecurityConfig.addConfigurationDetail(ctx, Application.CONFIG_KEYS.FLAG_PERSISTENT, "false");

        Mono<LabeledRepository> mono = builder.getRepository(entityStore, ctx.getEnvironment());


        StepVerifier.create(mono).assertNext(Repository::isInitialized).verifyComplete();
    }


}
