package io.av360.maverick.graph.main.api.entities.values;

import io.av360.maverick.graph.main.config.TestConfigurations;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.concurrent.atomic.AtomicBoolean;

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
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String description = "This is a description";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/dc.description")
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

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), DCTERMS.DESCRIPTION, vf.createLiteral(description, "en")));
    }

    @Test
    public void setValueWithUnknownPrefix() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "VideoObject"));

        String description = "This is a description";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/xxx.myPred")
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
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "VideoObject"));

        String title = "A new title";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
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
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();
    }


    public void addTitleWithExplicitLanguageTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "video"));

        String title = "A new title@en";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title/de")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(3, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    public void addTitleWithLanguageTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title@en";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(3, resultConsumer.countValues(video.getSubject(), vf.createIRI("https://schema.org/", "title")));
    }

    @Test
    public void addTitleWithUnknownLanguageTag() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title@this-is-.invalid";

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();

        // value must include fallback as language tag
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

        System.out.println(rdfConsumer.dump(RDFFormat.TURTLE));

        AtomicBoolean found = new AtomicBoolean(false);
        rdfConsumer.getStatements().forEach(statement -> {
            if(statement.getPredicate().equals(SDO.TITLE)) {
                if(statement.getObject().isLiteral()) {
                    Literal val = (Literal) statement.getObject();
                    if(val.getLanguage().isPresent()) {
                        if(val.getLanguage().get().equals("en")) {
                            found.set(true);
                        }
                    }

                }
            }
        });
        Assertions.assertTrue(found.get());


    }
}
