package io.av360.maverick.graph.main.api.entities.values;

import io.av360.maverick.graph.main.config.TestConfigurations;
import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
/**
 * Tests for endpoint POST /api/rs/{id}/{prefix.key}
 */
public class RemoveValuesTest extends TestsBase {

    @Test
    public void removeTitle() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(0, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    public void removeTitleWithLanguage() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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

        RdfConsumer resultConsumer = super.loadEntity((IRI) video.getSubject());
        Assert.equals(1, resultConsumer.countValues(video.getSubject(), SDO.TITLE));
    }

    @Test
    public void failToRemoveTitleWithLanguage() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_with_tags.ttl");
        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT);

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
    public void failToRemoveLink() {
        RdfConsumer rdfConsumer = super.upload("requests/create-esco.ttl");
        Statement entity = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://data.europa.eu/esco/model#", "Skill"));

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/skos.broader")
                        .build(
                                vf.createIRI(entity.getSubject().stringValue()).getLocalName()
                        )

                )
                .exchange()
                .expectStatus().isBadRequest();
    }

}
