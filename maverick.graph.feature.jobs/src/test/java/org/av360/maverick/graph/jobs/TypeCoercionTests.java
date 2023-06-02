package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.AssignInternalTypesJob;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.tests.config.TestRepositoryConfig;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
class TypeCoercionTests extends TestsBase {

    @Autowired
    private AssignInternalTypesJob scheduled;


    @Autowired
    EntityServicesClient entityServicesClient;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    void typeCoercionSimple() throws IOException {

        super.printStart("Test: Type Coercion (Simple)");

        // Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-esco.ttl"));

        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid-ext.ttl")).doOnSubscribe(sub -> super.printStep());
        Mono<Model> read1 = entityServicesClient.getModel().doOnSubscribe(sub -> super.printStep());
        Mono<Model> read2 = entityServicesClient.getModel().doOnNext(model -> super.printModel(model, RDFFormat.TURTLE)).doOnSubscribe(sub -> super.printStep());


        Mono<Void> jobMono = scheduled.run(TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(importMono.then(read1))
                .assertNext(md -> {
                    Assertions.assertTrue(md.subjects().size() > 0);
                }).verifyComplete();

        StepVerifier.create(jobMono).verifyComplete();


        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        IRI iri = vf.createIRI("http://www.example.org/vocab#x");
        StepVerifier.create(read2)
                .assertNext(model -> {
                    Assertions.assertEquals(4, model.size());
                    Assertions.assertEquals(1, model.subjects().size());
                    Assertions.assertEquals(iri, model.subjects().stream().findFirst().get());
                    Assertions.assertTrue(model.contains(iri, RDF.TYPE, Local.Entities.TYPE_INDIVIDUAL));
                })
                .verifyComplete();

    }


    @Test
    void typeCoercionMixed() throws IOException {

        super.printStart("Test: Type Coercion (Mixed)");


        Mono<Void> importMono = entityServicesClient.importFileToStore(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl")).doOnSubscribe(sub -> super.printStep());
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
                    Assertions.assertEquals(14, model.size());
                    Assertions.assertEquals(4, model.subjects().size());
                    Assertions.assertTrue(model.contains(null, RDF.TYPE, Local.Entities.TYPE_INDIVIDUAL), "No individual");
                    Assertions.assertTrue(model.contains(null, RDF.TYPE, Local.Entities.TYPE_CLASSIFIER), "No classifier");
                })
                .verifyComplete();

    }

}