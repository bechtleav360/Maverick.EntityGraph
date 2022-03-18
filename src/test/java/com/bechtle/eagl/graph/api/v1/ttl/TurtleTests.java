package com.bechtle.eagl.graph.api.v1.ttl;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import com.bechtle.eagl.graph.api.v1.EntitiesTest;
import com.bechtle.eagl.graph.domain.model.vocabulary.SDO;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import config.TestConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import utils.CsvConsumer;
import utils.RdfConsumer;

import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class TurtleTests implements EntitiesTest {


    public static ValueFactory vf = SimpleValueFactory.getInstance();

    @Autowired
    private WebTestClient webClient;



    @AfterEach
    public void resetRepository() {
        webClient.get()
                .uri("/api/admin/bulk/reset")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Override
    @Test
    public void createEntity() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.ttl");
        RdfConsumer rdfConsumer = this.create(file);


        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));

        // check if correct application events have been recorded
    }

    @Override
    @Test
    public void createEntityWithMissingType() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid-missingType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithInvalidSyntax() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid-syntax.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()

                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithValidId() {
        Resource file = new ClassPathResource("data/v1/requests/create-validWithId.ttl");
        RdfConsumer rdfConsumer = this.create(file);

        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));

    }

    @Test
    public void createEntityWithValidIdAndBase() {
        Resource file = new ClassPathResource("data/v1/requests/create-validWithIdAndBase.ttl");
        RdfConsumer rdfConsumer = create(file);

        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));

    }



    @Test
    @Override
    /**
     * The parser fails here, bug was reported: https://github.com/eclipse/rdf4j/issues/3658
     */
    public void createEntityWithInvalidId() {
        Resource file = new ClassPathResource("data/v1/requests/create-validWithInvalidId.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createMultipleEntities() {

        Resource file = new ClassPathResource("data/v1/requests/create-valid_multiple.ttl");
        RdfConsumer rdfConsumer = this.create(file);

        Collection<Statement> statements = rdfConsumer.getStatements();

        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));;

        Assertions.assertTrue(rdfConsumer.hasStatement(null, SDO.IDENTIFIER, SimpleValueFactory.getInstance().createLiteral("_a")));
        Assertions.assertTrue(rdfConsumer.hasStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT));
    }


    @Test
    public void createMultipleEntitiesWithNoType() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid_multipleOneNoType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    public void createMultipleEntitiesWithMixedIds() {
        Assertions.assertTrue(true);
    }




    @Override
    @Test
    public void createValue() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.ttl");
        RdfConsumer rdfConsumer = this.create(file);



        Model statements = rdfConsumer.asModel();
        Statement video = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video")).spliterator(), false).findFirst().orElseThrow();
        String description = "This is a description";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/dc.description")
                                .build(
                                        vf.createIRI(video.getSubject().stringValue()).getLocalName()
                                )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(description))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), video.getPredicate(), video.getObject()));
        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), vf.createIRI("http://purl.org/dc/terms/description"), vf.createLiteral(description)));
    }


    @Test
    public void createValueWithUnknownPrefix() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.ttl");
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
        Statement video = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video")).spliterator(), false).findFirst().orElseThrow();
        String description = "This is a description";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/xxx.myPred")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(description))
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Override
    @Test
    public void createEmbeddedEntity() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.ttl");

        RdfConsumer rdfConsumer = this.create(file);

        Model statements = rdfConsumer.asModel();
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));

        Statement video = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video")).spliterator(), false).findFirst().orElseThrow();



        Resource embedded = new ClassPathResource("data/v1/requests/create-valid_embedded.ttl");

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.hasDefinedTerm")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(embedded))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), video.getPredicate(), video.getObject()));
        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), vf.createIRI("http://schema.org/hasDefinedTerm"), null));
    }


    @Test
    public void createEmbeddedEntitiesWithSharedItems() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid_withEmbedded.ttl");
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

        long videos = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, vf.createIRI("http://schema.org/", "VideoObject")).spliterator(), false).count();
        Assertions.assertEquals(2, videos);

        List<Statement> collect = StreamSupport.stream(statements.getStatements(null, RDFS.LABEL, vf.createLiteral("Term 1")).spliterator(), false).toList();
        Assertions.assertEquals(1, collect.size());
    }


    @Test
    public void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() {
        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE);

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(new ClassPathResource("data/v1/requests/create-valid_withEmbedded.ttl")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody();

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(new ClassPathResource("data/v1/requests/create-valid_withEmbedded_second.ttl")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody();


        /**
         * SELECT DISTINCT * WHERE {
         *   ?id a <http://schema.org/VideoObject> ;
         * 		 <http://schema.org/hasDefinedTerm> ?term .
         *   ?term  <http://www.w3.org/2000/01/rdf-schema#label> "Term 1" .
         * }
         * LIMIT 10
         *
         * SELECT *
         * WHERE { ?id a <http://schema.org/VideoObject> ;
         *     <http://schema.org/hasDefinedTerm> ?term .
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

        //FIXME: now check if count distinct of binding "term" == 1
        /*
                .json()
                .


        Model statements = rdfConsumer.asModel();

        statements.forEach(System.out::println);

        long videos = StreamSupport.stream(statements.getStatements(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video")).spliterator(), false).count();
        Assertions.assertEquals(2, videos);

        List<Statement> collect = StreamSupport.stream(statements.getStatements(null, RDFS.LABEL, vf.createLiteral("Term 1")).spliterator(), false).toList();
        Assertions.assertEquals(1, collect.size());*/
    }

    @Override
    public void createEdgeWithIdInPayload() {

    }

    @Override
    public void createEdge() {

    }

    @Override
    public void createEdgeWithInvalidDestinationId() {

    }

    private RdfConsumer create(Resource file) {
        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE, true);

        webClient.post()
                .uri("/api/entities")
                .contentType(RdfUtils.getMediaType(RDFFormat.TURTLE))
                .accept(RdfUtils.getMediaType(RDFFormat.TURTLE))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(rdfConsumer);

        return rdfConsumer;
    }
}
