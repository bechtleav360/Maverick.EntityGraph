package org.av360.maverick.graph.services.schedulers;

import org.av360.maverick.graph.services.clients.EntityServicesClient;
import org.av360.maverick.graph.services.schedulers.replaceGlobalIdentifiers.ScheduledReplaceGlobalIdentifiers;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import lombok.extern.slf4j.Slf4j;
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
class CheckGlobalIdentifiersTest extends TestsBase {

    @Autowired
    private ScheduledReplaceGlobalIdentifiers scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void checkForGlobalIdentifiers() throws IOException {

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-esco.ttl"));
        Mono<Model> readModelMono = entityServicesClient.getModel();



        Flux<Transaction> actionMono = scheduled.checkForGlobalIdentifiers(TestSecurityConfig.createAuthenticationToken());

        StepVerifier.create(importMono.then(readModelMono))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(actionMono)
                .thenAwait(Duration.of(2, ChronoUnit.SECONDS))
                .assertNext(Assertions::assertNotNull)
                .assertNext(Assertions::assertNotNull)
                .assertNext(Assertions::assertNotNull)
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();

        StepVerifier.create(readModelMono)
                .consumeNextWith(model -> {
                    super.printModel(model, RDFFormat.JSONLD);
                }).verifyComplete();

    }

}