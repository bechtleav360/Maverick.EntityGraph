package io.av360.maverick.graph.api.values;

import io.av360.maverick.graph.boot.TestConfigurations;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
/**
 * Tests for endpoint POST /api/rs/{id}/{prefix.key}
 */
public class CreateValuesTest extends TestsBase {

    @Test
    public void setDescription() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

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
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), video.getPredicate(), video.getObject()));
        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), vf.createIRI("http://purl.org/dc/terms/description"), vf.createLiteral(description)));
    }

    @Test
    public void setValueWithUnknownPrefix() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

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

    @Test
    public void replaceTitle() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

        String title = "A new title";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void addTitleWithMissingTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

        String title = "A new title";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void addTitleWithLanguageTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

        String title = "A new title@en";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(2, resultConsumer.countValues(video.getSubject(), vf.createIRI("http://schema.org/", "title")));
    }

    @Test
    public void addTitleWithUnknownLanguageTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));

        String title = "A new title@this-is-.invalid";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isBadRequest();


    }
}
