package org.av360.maverick.graph.feature.applications.scoped;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.ApplicationsTestsBase;
import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.io.IOException;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
@Slf4j
public class ScheduleScopedJobs extends ApplicationsTestsBase {


    @AfterEach
    public void resetRepository() {
        super.resetRepository("test_app");
    }
    @Test
    public void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() throws InterruptedException, IOException {

        super.printStart("Running a job within an application context");
        applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true)).expectStatus().isCreated();

        super.printStep("Importing file");
        adminTestClient.importTurtleFile(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl"), Map.of("X-Application", "test_app"));

        Thread.sleep(500);

        super.printStep("Importing file");
        adminTestClient.importTurtleFile(new ClassPathResource("requests/create-valid_withEmbedded.ttl"), Map.of("X-Application", "test_app"));

        // Mono<RdfTransaction> tx1 = adminTestClient.importFile(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl"), RDFFormat.TURTLE, TestSecurityConfig.createTestContext().withEnvironment().setScope("test_app") ).doOnSubscribe(sub -> super.printStep("Importing file"));
        // Mono<RdfTransaction> tx2 = entityServices.importFile(new ClassPathResource("requests/create-valid_withEmbedded.ttl"), RDFFormat.TURTLE, TestSecurityConfig.createTestContext().withEnvironment().setScope("test_app")).doOnSubscribe(sub -> super.printStep("Importing another file"));
        // Mono<Void> scheduler = this.scheduledDetectDuplicates.checkForDuplicates(RDFS.LABEL, TestSecurityConfig.createTestContext()).doOnSubscribe(sub -> log.trace("-------- 3"));
        /*Mono<Model> getAll = entityServices.getModel(TestSecurityConfig.createTestContext().withEnvironment().setScope("test-app")).doOnSubscribe(sub -> super.printStep());

        StepVerifier.create(
                        tx1.then(tx2).then(getAll)
                ).assertNext(model -> {
                    super.printModel(model, RDFFormat.TURTLE);

                    Set<Resource> videos = model.filter(null, RDF.TYPE, SDO.VIDEO_OBJECT).subjects();
                    Assertions.assertEquals(3, videos.size());

                    // Set<Resource> terms = model.filter(null, RDF.TYPE, SDO.DEFINED_TERM).subjects();
                    // Assertions.assertEquals(1, terms.size());
                })
                .verifyComplete();*/

        //.flatMap(trx -> entityServicesClient.getModel());


    }
}
