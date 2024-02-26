package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.jobs.ReplaceLinkedIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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


@SpringBootTest
@ContextConfiguration(classes = TestRepositoryConfig.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
@SuppressWarnings("all")
class TestReplaceObjectIdentifiers extends TestsBase {

    @Autowired
    private ReplaceLinkedIdentifiersJob scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;

    @Autowired
    EntityServices entityServices;

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void externalSimple() throws IOException {

        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert original identifier to owl:sameAs relation (Single)");

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServices.importFile(new ClassPathResource("intermediate/externalSimple.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServices.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();


        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(6, model.size());
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));
                    Assertions.assertFalse(model.contains(null, Local.ORIGINAL_IDENTIFIER, null));
                })
                .verifyComplete();

    }


    @Test
    void externalMultiple() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert original identifier to owl:sameAs relation (Multiple)");


        Mono<Void> importMono = entityServices.importFile(new ClassPathResource("intermediate/externalMultiple.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServices.asModel(ctx).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(12, model.size());
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#y")));
                    Assertions.assertFalse(model.contains(null, Local.ORIGINAL_IDENTIFIER, null));
                })
                .verifyComplete();

    }


    @Test
    void externalWithClassified() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert mixed identifiers (Multiple)");


        Mono<Void> importMono = entityServices.importFile(new ClassPathResource("intermediate/externalWithEmbedded.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(11, model.size());
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.acme.org/vocab#a")));
                    Assertions.assertFalse(model.contains(null, Local.ORIGINAL_IDENTIFIER, null));

                    model.getStatements(null, SDO.HAS_DEFINED_TERM, null).forEach(statement -> {
                        Assertions.assertTrue(model.contains(statement.getSubject(), RDF.TYPE, SDO.VIDEO_OBJECT));
                        Assertions.assertTrue(statement.getObject().isIRI());
                        Assertions.assertTrue(model.contains((IRI) statement.getObject(), RDF.TYPE, SDO.DEFINED_TERM));
                    });
                })
                .verifyComplete();

    }

    @Test
    void externalMultipleWithEmbedded() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");


        Mono<Void> importMono = entityServices.importFile(new ClassPathResource("intermediate/externalMultipleWithEmbedded.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(14, model.size());
                    Assertions.assertEquals(3, model.subjects().size());
                })
                .verifyComplete();

    }


    @Test
    void externalWithShared() throws IOException {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");

        Mono<Void> importMono = entityServices.importFile(new ClassPathResource("intermediate/externalWithShared.ttl"), RDFFormat.TURTLE, ctx).doOnSubscribe(sub -> super.printStep()).then();
        Mono<Model> read1 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServices.asModel(ctx).doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(ctx).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();

        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(26, model.size());
                    Assertions.assertEquals(5, model.subjects().size());
                })
                .verifyComplete();

    }

}