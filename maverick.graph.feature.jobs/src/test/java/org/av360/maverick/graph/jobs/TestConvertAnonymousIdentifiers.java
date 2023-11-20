package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ReplaceSubjectIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
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
@SuppressWarnings("all")
class TestConvertAnonymousIdentifiers extends TestsBase {

    @Autowired
    private ReplaceSubjectIdentifiersJob scheduled;


    @Autowired
    EntityServices entityServicesClient;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void checkForGlobalIdentifiers() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert anonymous identifiers (Simple)");

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServicesClient.importFile(new ClassPathResource("requests/create-valid.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServicesClient.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> Assertions.assertTrue(md.subjects().size() > 0)).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(4, model.size());
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertEquals(Local.Entities.NAMESPACE + "b2p6u4vx", model.subjects().stream().findFirst().get().stringValue());
                })
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersMultiple() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert anonymous identifiers (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFile(new ClassPathResource("requests/create-valid_multiple-ext.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServicesClient.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> Assertions.assertTrue(md.subjects().size() > 0)).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> Assertions.assertEquals(10, model.size()))
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersObjects() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert global identifiers (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFile(new ClassPathResource("requests/create-valid_withEmbedded-ext.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> Assertions.assertTrue(md.subjects().size() > 0)).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> Assertions.assertEquals(9, model.size()))
                .verifyComplete();

    }


    @Test
    void checkForGlobalIdentifiersMultipleWithSharedObject() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");


        Mono<Void> importMono = entityServicesClient.importFile(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(12, model.size());
                })
                .verifyComplete();

    }

}

/*

<urn:pwid:meg:e:bzpgjhdx> a <https://schema.org/DefinedTerm>, <urn:pwid:meg:e:Classifier>;
  <http://www.w3.org/2000/01/rdf-schema#label> "Term 1" .

<urn:pwid:meg:e:b56adz7x> a <urn:pwid:meg:e:Individual>, <https://schema.org/VideoObject>;
  <https://schema.org/hasDefinedTerm> <urn:pwid:meg:e:bzpgjhdx>;
  <https://schema.org/identifier> "video_a" .

<urn:pwid:meg:e:bspau98x> a <https://schema.org/VideoObject>, <urn:pwid:meg:e:Individual>;
  <https://schema.org/hasDefinedTerm> <urn:pwid:meg:e:bzpgjhdx>;
  <https://schema.org/identifier> "video_b" .

 */