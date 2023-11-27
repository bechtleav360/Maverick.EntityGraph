package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.av360.maverick.graph.feature.jobs.ReplaceSubjectIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;


@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
@SuppressWarnings("all")
class TestReplaceSubjectIdentifiers extends TestsBase {

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
    void testReplaceAnonymousIdentifiers() throws IOException {

        super.printStart("testReplaceAnonymousIdentifiers");
        SessionContext ctx = TestSecurityConfig.createTestContext();

        Mono<Transaction> importMono = entityServices.importFile(new ClassPathResource("requests/create-valid.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> readModelMono = entityServices.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Void> actionMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(readModelMono))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(actionMono)
                .thenAwait(Duration.of(1, ChronoUnit.SECONDS))
                .verifyComplete();

        StepVerifier.create(readModelMono)
                .consumeNextWith(model -> {
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertTrue(model.subjects().iterator().next().isIRI());

                    super.printModel(model, RDFFormat.TURTLE);
                }).verifyComplete();

    }

    @Test
    void testReplaceAnonymousIdentifiersWithShared() throws IOException {

        super.printStart("testReplaceAnonymousIdentifiersInComplex");
        SessionContext ctx = TestSecurityConfig.createTestContext();

        Mono<Transaction> importMono = entityServices.importFile(new ClassPathResource("requests/create-valid-complex.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> readModelMono = entityServices.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Void> actionMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(readModelMono))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(actionMono)
                .thenAwait(Duration.of(1, ChronoUnit.SECONDS))
                .verifyComplete();

        StepVerifier.create(readModelMono)
                .consumeNextWith(model -> {
                    super.printModel(model, RDFFormat.TURTLE);
                    Assertions.assertTrue(IterableUtils.size(model.getStatements(null, RDF.TYPE, Local.Entities.TYPE_EMBEDDED)) == 1);
                    Assertions.assertTrue(IterableUtils.size(model.getStatements(null, RDF.TYPE, Local.Entities.TYPE_INDIVIDUAL)) == 1);
                    Assertions.assertTrue(IterableUtils.size(model.getStatements(null, RDF.TYPE, Local.Entities.TYPE_CLASSIFIER)) == 1);

                    Assertions.assertEquals(3, model.subjects().size());
                    Assertions.assertTrue(model.subjects().iterator().next().isIRI());


                }).verifyComplete();

    }


    @Test
    void testReplaceExternalIdentifiers() throws IOException {

        super.printStart("testReplaceExternalIdentifiers");
        SessionContext ctx = TestSecurityConfig.createTestContext();

        Mono<Transaction> s1 = entityServices.importFile(new ClassPathResource("requests/create-valid-ext.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> s3 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Void> s2 = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(s1.then(s3))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(s2)
                .thenAwait(Duration.of(1, ChronoUnit.SECONDS))
                .verifyComplete();

        StepVerifier.create(s3)
                .consumeNextWith(model -> {
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertTrue(model.subjects().iterator().next().isIRI());
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));

                    super.printModel(model, RDFFormat.TURTLE);
                }).verifyComplete();

    }

    @Test
    void testReplaceSubjectIdentifiers() throws IOException {

        super.printStart("testReplaceSubjectIdentifiersEsco");

        SessionContext ctx = TestSecurityConfig.createTestContext();

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Transaction> s1 = entityServices.importFile(new ClassPathResource("requests/create-esco.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Void> s2 = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> s3 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(s1.then(s3))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(s2)
                .thenAwait(Duration.of(1, ChronoUnit.SECONDS))
                .verifyComplete();

        StepVerifier.create(s3)
                .consumeNextWith(model -> {
                    Assertions.assertEquals(4, model.subjects().size());
                    Assertions.assertTrue(model.subjects().stream().allMatch(sub -> sub.isIRI()));

                    super.printModel(model, RDFFormat.TURTLE);
                }).verifyComplete();

    }

}