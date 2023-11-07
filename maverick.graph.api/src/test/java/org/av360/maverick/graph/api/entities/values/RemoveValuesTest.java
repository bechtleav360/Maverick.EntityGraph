package org.av360.maverick.graph.api.entities.values;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.vocabulary.Details;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
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
    public void removeValueDefault() {
        super.printStart("removeTitle");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findFirstStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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
    public void removeAllValues() {

        super.printStart("Remove a value from a list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);

        super.printStep("Remove the value");

        super.getTestClient().deleteValue(sourceIdentifier, "sdo.author").expectStatus().isBadRequest();
    }

    @Test
    public void removeValueIsLink() {
        super.printStart("remove link to resource");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findFirstStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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
        Statement video = rdfConsumer.findFirstStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

        super.printStep();
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/sdo.title")
                        .queryParam("languageTag", "de")
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
    public void removeTitleWithLanguageTagFail() {
        super.printStart("failToRemoveTitleWithLanguage");

        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findFirstStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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



    @Test
    public void removeValueByHash() {

        super.printStart("Remove a value from a list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);

        super.printStep("Remove the value");

        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier, "sdo.author");
        rc2.print(RDFFormat.TURTLESTAR);
        Statement valueStatement = rc2.findFirstStatement(sourceIdentifier, SDO.AUTHOR, null);
        Statement detailStatement = rc2.findFirstStatement(Values.triple(valueStatement), Details.HASH, null);

        Assertions.assertTrue(detailStatement.getSubject().isTriple());

        super.getTestClient().deleteValueByHash(sourceIdentifier, "sdo.author", detailStatement.getObject().stringValue());


        RdfConsumer rc3 = super.getTestClient().listValues(sourceIdentifier, "sdo.author");
        rc3.print(RDFFormat.TURTLESTAR);
        Assertions.assertEquals(1, rc3.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());
    }


    @Test
    public void removeValueWithDuplicateLanguageTag() {

        super.printStart("Remove a value from a list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);

        super.printStep("Remove the value");

        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier, "sdo.author");
        rc2.print(RDFFormat.TURTLESTAR);

        super.getTestClient().deleteValueByTag(sourceIdentifier, "sdo.author", "en").expectStatus().isBadRequest();
    }

}
