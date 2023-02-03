package io.av360.maverick.graph.main.schedulers;

import io.av360.maverick.graph.main.config.TestConfigurations;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.services.schedulers.detectDuplicates.ScheduledDetectDuplicates;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.tests.api.v2.MergeDuplicatesScheduler;
import io.av360.maverick.graph.tests.util.CsvConsumer;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.StreamSupport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class MergeDuplicateTests extends TestsBase implements MergeDuplicatesScheduler {


    @Autowired
    private ScheduledDetectDuplicates scheduledDetectDuplicates;

    @AfterEach
    public void resetRepository() {
        super.resetRepository(RepositoryType.ENTITIES.name());
    }


    /**
     * Verify that embedded items in one request are merged
     */
    @Override
    @Test
    public void createEmbeddedEntitiesWithSharedItems() {
        log.info("---------- Running test: Create embedded with share items ---------- ");

        Resource file = new ClassPathResource("requests/create-valid_withEmbedded.ttl");
        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE);

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .accept(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()

                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(rdfConsumer);

        Model statements = rdfConsumer.asModel();

        statements.forEach(System.out::println);

        long videos = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, TestsBase.vf.createIRI("https://schema.org/", "VideoObject")).spliterator(), false).count();
        Assertions.assertEquals(2, videos);

        //long terms = StreamSupport.stream(statements.getStatements(null, RDFS.LABEL, TestsBase.vf.createLiteral("Term 1")).spliterator(), false).count();
        long terms = StreamSupport.stream(statements.getStatements(null, RDFS.LABEL, null).spliterator(), false).count();
        Assertions.assertEquals(1, terms);
    }


    @Override
    @Test
    public void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() throws InterruptedException {
        log.info("---------- Running test: Create embedded with shared items in separate requests ---------- ");

        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE);

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(new ClassPathResource("requests/create-valid_withEmbedded.ttl")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody();

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(new ClassPathResource("requests/create-valid_withEmbedded_second.ttl")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody();


        StepVerifier.create(this.scheduledDetectDuplicates.checkForDuplicates(RDFS.LABEL, new TestingAuthenticationToken("", "", List.of(Authorities.SYSTEM)))).verifyComplete();

        /**
         * SELECT DISTINCT * WHERE {
         *   ?id a <https://schema.org/VideoObject> ;
         * 		 <https://schema.org/hasDefinedTerm> ?term .
         *   ?term  <http://www.w3.org/2000/01/rdf-schema#label> "Term 1" .
         * }
         * LIMIT 10
         *
         * SELECT *
         * WHERE { ?id a <https://schema.org/VideoObject> ;
         *     <https://schema.org/hasDefinedTerm> ?term .
         * ?term <http://www.w3.org/2000/01/rdf-schema#label> "Term 1" . }
         */


        CsvConsumer csvConsumer = new CsvConsumer();
        Variable id = SparqlBuilder.var("id");
        Variable term = SparqlBuilder.var("term");
        // SelectQuery all = Queries.SELECT(id).where(id.isA(SDO.VIDEO_OBJECT).andHas(SDO.HAS_DEFINED_TERM, term)).where(term.has(RDFS.LABEL, "Term 1")).all();
        SelectQuery all = Queries.SELECT(id).where(term.has(RDFS.LABEL, "Term 1")).all();
        webClient.post()
                .uri("/api/query/select")
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .body(BodyInserters.fromValue(all.getQueryString()))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);

        Assertions.assertEquals(1, csvConsumer.getRows().size());

    }


}
