package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ReplaceObjectIdentifiersJob;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
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
class ConvertExternalLinked extends TestsBase {

    @Autowired
    private ReplaceObjectIdentifiersJob scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void externalSimple() throws IOException {

        super.printStart("Test: Convert original identifier to owl:sameAs relation (Single)");

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("intermediate/externalSimple.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();


        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(4, model.size());
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));
                    Assertions.assertFalse(model.contains(null, Local.ORIGINAL_IDENTIFIER, null));
                })
                .verifyComplete();

    }


    @Test
    void externalMultiple() throws IOException {

        super.printStart("Test: Convert original identifier to owl:sameAs relation (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("intermediate/externalMultiple.ttl")).doOnSubscribe(sub -> super.printStep());
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
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#x")));
                    Assertions.assertTrue(model.contains(null, OWL.SAMEAS, vf.createIRI("http://www.example.org/vocab#y")));
                    Assertions.assertFalse(model.contains(null, Local.ORIGINAL_IDENTIFIER, null));
                })
                .verifyComplete();

    }


    @Test
    void externalWithEmbedded() throws IOException {

        super.printStart("Test: Convert mixed identifiers (Multiple)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("intermediate/externalWithEmbedded.ttl")).doOnSubscribe(sub -> super.printStep());
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

        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("intermediate/externalMultipleWithEmbedded.ttl")).doOnSubscribe(sub -> super.printStep());
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
                    Assertions.assertEquals(3, model.subjects().size());
                })
                .verifyComplete();

    }


    @Test
    void externalWithShared() throws IOException {

        super.printStart("Test: Convert anonymous identifiers (Multiple with shared object)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("intermediate/externalWithShared.ttl")).doOnSubscribe(sub -> super.printStep());
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
                    Assertions.assertEquals(16, model.size());
                    Assertions.assertEquals(5, model.subjects().size());
                })
                .verifyComplete();

    }

}