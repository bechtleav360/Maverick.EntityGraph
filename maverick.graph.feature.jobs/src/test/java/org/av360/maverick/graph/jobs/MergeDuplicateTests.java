package org.av360.maverick.graph.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.DetectDuplicatesService;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
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
import java.util.TreeSet;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class MergeDuplicateTests extends TestsBase  {


    @Autowired
    private DetectDuplicatesService scheduledDetectDuplicates;

    @Autowired
    EntityServicesClient entityServicesClient;

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }


    private String[] asSortedArray(Set<Resource> resources) {
        return resources.stream()
                .map(Value::stringValue)
                .distinct()
                .collect(Collectors.toCollection(TreeSet::new))
                .toArray(new String[resources.size()]);
    }

    @Test
    public void createEmbeddedWithSharedItemsDirect() throws IOException {

        ClassPathResource file = new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl");

        Mono<Model> m = entityServicesClient.importFileMono(file)
                .flatMap(trx -> entityServicesClient.getModel());

        StepVerifier.create(m)
                .assertNext(model -> {
                    super.printModel(model, RDFFormat.TURTLE);

                    Set<Resource> videos = model.filter(null, RDF.TYPE, SDO.VIDEO_OBJECT).subjects();
                    Assertions.assertEquals(2, videos.size());

                    Set<Resource> terms = model.filter(null, RDF.TYPE, SDO.DEFINED_TERM).subjects();
                    Assertions.assertEquals(1, terms.size());
                    Resource first = terms.stream().findFirst().orElseThrow();

                    Set<Resource> linkedVideos = model.filter(null, SDO.HAS_DEFINED_TERM, first).subjects();
                    Assertions.assertEquals(2, linkedVideos.size());


                    // Assertions.assertArrayEquals(asSortedArray(videos), asSortedArray(linkedVideos));


                }).verifyComplete();

    }

    @Test
    public void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() throws InterruptedException, IOException {
        log.info("---------- Running test: Create embedded with shared items in separate requests ---------- ");
        Mono<Transaction> tx1 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-valid_multipleWithEmbedded.ttl")).doOnSubscribe(sub -> log.trace("-------- 1"));
        Mono<Transaction> tx2 = entityServicesClient.importFileMono(new ClassPathResource("requests/create-valid_withEmbedded.ttl")).doOnSubscribe(sub -> log.trace("-------- 2"));
        Mono<Void> scheduler = this.scheduledDetectDuplicates.checkForDuplicates(RDFS.LABEL, TestSecurityConfig.createAuthenticationToken()).doOnSubscribe(sub -> log.trace("-------- 3"));
        Mono<Model> getAll = entityServicesClient.getModel();

        StepVerifier.create(
                tx1.then(tx2).then(scheduler).then(getAll)
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
