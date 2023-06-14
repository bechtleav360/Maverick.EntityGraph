package org.av360.maverick.graph.feature.applications.scoped;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class ScheduleScopedJobs extends TestsBase {


    @Autowired
    private EntityServices entityServices;

    @Test
    public void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() throws InterruptedException, IOException {

        log.info("---------- Running test: Create scoped embedded with shared items in separate requests ---------- ");
        Mono<RdfTransaction> tx1 = entityServices.importFile(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl"), RDFFormat.TURTLE, TestSecurityConfig.createTestContext().withEnvironment().setScope("test-app") ).doOnSubscribe(sub -> log.trace("-------- 1"));
        Mono<RdfTransaction> tx2 = entityServices.importFile(new ClassPathResource("requests/create-valid_withEmbedded.ttl"), RDFFormat.TURTLE, TestSecurityConfig.createTestContext().withEnvironment().setScope("test-app")).doOnSubscribe(sub -> log.trace("-------- 2"));
        // Mono<Void> scheduler = this.scheduledDetectDuplicates.checkForDuplicates(RDFS.LABEL, TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> log.trace("-------- 3"));
        Mono<Model> getAll = entityServices.getModel(TestSecurityConfig.createTestContext().withEnvironment().setScope("test-app"));

        StepVerifier.create(
                        tx1.then(tx2).then(getAll)
                ).assertNext(model -> {
                    super.printModel(model, RDFFormat.TURTLE);

                    Set<Resource> videos = model.filter(null, RDF.TYPE, SDO.VIDEO_OBJECT).subjects();
                    Assertions.assertEquals(3, videos.size());

                    // Set<Resource> terms = model.filter(null, RDF.TYPE, SDO.DEFINED_TERM).subjects();
                    // Assertions.assertEquals(1, terms.size());
                })
                .verifyComplete();

        //.flatMap(trx -> entityServicesClient.getModel());


    }
}
