package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ReplaceSubjectIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
@SuppressWarnings("all")
class CheckGlobalIdentifiersTest extends TestsBase {

    @Autowired
    private ReplaceSubjectIdentifiersJob scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;

    @Autowired
    EntityServices entityServices;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void checkForGlobalIdentifiers() throws IOException {

        super.printStart("checkForGlobalIdentifiers");

        SessionContext ctx = TestSecurityConfig.createTestContext();

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<RdfTransaction> importMono = entityServices.importFile(new ClassPathResource("requests/create-esco.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> readModelMono = entityServices.getModel(ctx).doOnSubscribe(sub -> super.printStep());


        Flux<RdfTransaction> actionMono = scheduled.checkForExternalSubjectIdentifiers(TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(readModelMono))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(actionMono)
                .thenAwait(Duration.of(2, ChronoUnit.SECONDS))
                .assertNext(Assertions::assertNotNull)

                .verifyComplete();

        StepVerifier.create(readModelMono)
                .consumeNextWith(model -> {
                    super.printModel(model, RDFFormat.TURTLE);
                }).verifyComplete();

    }

}