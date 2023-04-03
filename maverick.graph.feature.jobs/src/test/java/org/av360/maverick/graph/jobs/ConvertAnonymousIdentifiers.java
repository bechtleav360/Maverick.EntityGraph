package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.ReplaceExternalIdentifiersService;
import org.av360.maverick.graph.model.vocabulary.Local;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;


@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
class ConvertAnonymousIdentifiers extends TestsBase {

    @Autowired
    private ReplaceExternalIdentifiersService scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void checkForGlobalIdentifiers() throws IOException {

        super.printStart("Test: Convert anonymous identifiers (Simple)");

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(3, model.size());
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertEquals(Local.Entities.NAMESPACE+"b2p6u4vx", model.subjects().stream().findFirst().get().stringValue());
                })
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersMultiple() throws IOException {

        super.printStart("Test: Convert anonymous identifiers (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid_multiple-ext.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(8, model.size());
                })
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersObjects() throws IOException {

        super.printStart("Test: Convert global identifiers (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid_withEmbedded-ext.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(7, model.size());
                })
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersMultipleWithSharedObject() throws IOException {

        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(8, model.size());
                })
                .verifyComplete();

    }

}