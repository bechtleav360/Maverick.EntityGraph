package org.av360.maverick.graph.api.entities.values;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.vocabulary.DCTERMS;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
/**
 * Tests for endpoint POST /api/rs/{id}/{prefix.key}
 */
public class CreateValuesTest extends ApiTestsBase {

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void setDescription() {
        super.printStart("setDescription");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String description = "This is a description";

        super.printStep();
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

        super.printResult("Final model", rdfConsumer.dump(RDFFormat.TURTLE));
        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), DCTERMS.DESCRIPTION, vf.createLiteral(description, "en")));
    }

    @Test
    public void setValueWithUnknownPrefix() {
        super.printStart("setValueWithUnknownPrefix");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "VideoObject"));

        String description = "This is a description";

        super.printStep();
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
        super.printStart("replaceTitle");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "VideoObject"));

        String title = "A new title";

        super.printStep();
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
        super.printStart("addTitleWithMissingTag");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title";

        super.printStep();
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
    public void addTitleWithExplicitLanguageTag() {
        super.printStart("addTitleWithExplicitLanguageTag");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("https://schema.org/", "VideoObject"));

        String title = "A new title@en";

        super.printStep();
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .queryParam("lang", "en")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue(title))
                .exchange()
                .expectStatus().isOk();

        super.printStep();

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        super.printModel(resultConsumer.asModel(), RDFFormat.TURTLE);
        Assert.equals(3, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    @Disabled
    public void addTitleWithLanguageTag() {
        super.printStart("addTitleWithLanguageTag");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title@en";


        super.printStep();
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

        super.printStep();
        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        resultConsumer.print();
        Assert.equals(3, resultConsumer.countValues(video.getSubject(), vf.createIRI("https://schema.org/", "title")));
    }

    @Test
    public void addTitleWithUnknownLanguageTag() {
        super.printStart("addTitleWithUnknownLanguageTag");


        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        rdfConsumer.dump(RDFFormat.TURTLE);

        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        String title = "A new title@this-is-.invalid";

        super.printStep();
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

        System.out.println(rdfConsumer.dump(RDFFormat.TURTLE));

        super.printStep();
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


    @Test
    public void testSetResourceLinkValue() {
        super.printStart("set link to resource");

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
    }
}
