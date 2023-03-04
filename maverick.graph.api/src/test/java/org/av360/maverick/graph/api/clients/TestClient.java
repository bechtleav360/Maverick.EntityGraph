package org.av360.maverick.graph.api.clients;

import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.StringWriter;

public class TestClient {

    private final WebTestClient webClient;

    public TestClient(WebTestClient webClient) {
        this.webClient = webClient;
    }

    public WebTestClient.ResponseSpec createEntity(Publisher<String> content, RDFFormat format) {
        return webClient.post()
                .uri("/api/entities")
                .accept(MediaType.parseMediaType(format.getDefaultMIMEType()))
                .contentType(MediaType.parseMediaType(format.getDefaultMIMEType()))
                .body(BodyInserters.fromPublisher(content, String.class))
                .exchange();
    }

    public RdfConsumer createEntity(Model content) {
        RdfConsumer consumer = new RdfConsumer(RDFFormat.JSONLD);
        Mono<String> serializedMono = serialize(content, RDFFormat.JSONLD);

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()))
                .body(BodyInserters.fromPublisher(serializedMono, String.class))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(consumer);
        return consumer;
    }



    private RDFFormat getFormatFromExt(@Nonnull Resource file) {
        if (StringUtils.hasLength(file.getFilename())) {
            if (file.getFilename().endsWith(RDFFormat.TURTLE.getDefaultFileExtension())) return RDFFormat.TURTLE;
            if (file.getFilename().endsWith(RDFFormat.JSONLD.getDefaultFileExtension())) return RDFFormat.JSONLD;
            if (file.getFilename().endsWith(RDFFormat.N3.getDefaultFileExtension())) return RDFFormat.N3;
            if (file.getFilename().endsWith(RDFFormat.RDFXML.getDefaultFileExtension())) return RDFFormat.RDFXML;
        }

        throw new RuntimeException("Invalid file format for file: "+ file);
    }

    public RdfConsumer createEntity(Resource file) {
        RDFFormat format = getFormatFromExt(file);
        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        webClient.post()
                .uri("/api/entities")
                .contentType(RdfUtils.getMediaType(format))
                .accept(RdfUtils.getMediaType(format))
                .body(BodyInserters.fromResource(file))
                .header("X-API-KEY", "test")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(rdfConsumer);

        return rdfConsumer;
    }



    public WebTestClient.ResponseSpec createLink(String sourceId, String prefixedKey, String targetId) {

        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/links/{prefixedKey}/{target}")
                        .build(sourceId, prefixedKey, targetId)
                )
                .accept(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()))
                .exchange()
                .expectStatus().isCreated();

    }


    static Mono<String> serialize(Model builder, RDFFormat format ) {
        StringWriter sw = new StringWriter();
        Rio.write(builder, sw, format);
        return Mono.just(sw.toString());
    }
}
