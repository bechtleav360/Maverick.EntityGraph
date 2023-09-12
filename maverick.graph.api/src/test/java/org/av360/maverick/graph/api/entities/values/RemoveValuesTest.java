package org.av360.maverick.graph.api.entities.values;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
/**
 * Tests for endpoint POST /api/rs/{id}/{prefix.key}
 */
public class RemoveValuesTest extends ApiTestsBase {

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void removeTitle() {
        super.printStart("removeTitle");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        super.printStep();
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        super.printStep();
        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(0, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    public void testRemoveResourceLinkValue() {
        super.printStart("remove link to resource");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String link = "<http://example.com#concept>";

        super.printStep();
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.hasDefinedTerm")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(link))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), video.getPredicate(), video.getObject()));

        super.printStep();
        rdfConsumer = new RdfConsumer(RDFFormat.TURTLE);
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .accept(RdfMimeTypes.TURTLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), SDO.HAS_DEFINED_TERM, vf.createIRI("http://example.com#concept")));


        super.printStep();

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.hasDefinedTerm")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .exchange()
                .expectStatus().isOk();

        super.printStep();
        rdfConsumer = new RdfConsumer(RDFFormat.TURTLE);
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .accept(RdfMimeTypes.TURTLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertFalse(rdfConsumer.hasStatement(video.getSubject(), SDO.HAS_DEFINED_TERM, vf.createIRI("http://example.com#concept")));
    }

    @Test
    public void removeTitleWithLanguage() {
        super.printStart("removeTitleWithLanguage");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        super.printStep();
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .queryParam("lang", "de")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        super.printStep();
        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(1, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    public void failToRemoveTitleWithLanguage() {
        super.printStart("failToRemoveTitleWithLanguage");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        super.printStep();
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .exchange()
                .expectStatus().isBadRequest();
    }




}
