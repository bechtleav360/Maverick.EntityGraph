package org.av360.maverick.graph.tests.clients;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.tests.util.RdfConsumer;
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

import java.io.StringWriter;
import java.util.Objects;

public class TestEntitiesClient {

    private final WebTestClient webClient;

    public TestEntitiesClient(WebTestClient webClient) {
        this.webClient = webClient;
    }

    public WebTestClient.ResponseSpec createEntity(Publisher<String> content, RDFFormat format) {
        return this.createEntity(content, "/api/entities", format);
    }

    public WebTestClient.ResponseSpec createEntity(Publisher<String> content, String path, RDFFormat format) {
        return webClient.post()
                .uri(path)
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

        throw new RuntimeException("Invalid file format for file: " + file);
    }

    public RdfConsumer createEntity(Resource file) {
        return this.createEntity(file, "/api/entities");
    }

    public RdfConsumer createEntity(Resource file, String path) {
        RDFFormat format = getFormatFromExt(file);
        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        webClient.post()
                .uri(path)
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

    public WebTestClient.ResponseSpec checkEntity(String sourceIdentifier) {
        return this.checkEntity(sourceIdentifier, null);
    }

    public WebTestClient.ResponseSpec checkEntity(String sourceIdentifier, @Nullable RDFFormat formatParam) {
        RDFFormat format = Objects.isNull(formatParam) ? RDFFormat.TURTLE : formatParam;
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}")
                        .build(sourceIdentifier)
                )
                .accept(RdfUtils.getMediaType(format))
                .header("X-API-KEY", "test")
                .exchange();

    }

    public RdfConsumer listEntities(String path) {
        RDFFormat format = RDFFormat.TURTLE;

        RdfConsumer rdfConsumer = new RdfConsumer(format, false);
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .build()
                )
                .accept(RdfUtils.getMediaType(format))
                .header("X-API-KEY", "test")
                .exchange()
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;

    }

    public RdfConsumer readEntity(String sourceIdentifier) {
        RDFFormat format = RDFFormat.TURTLE;

        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        this.checkEntity(sourceIdentifier, format)
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;
    }

    public WebTestClient.ResponseSpec deleteLink(String sourceId, String prefixedKey, String targetId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/links/{prefixedKey}/{target}")
                        .build(sourceId, prefixedKey, targetId)
                )
                .accept(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()))
                .exchange();
    }

    public WebTestClient.ResponseSpec createLink(String sourceId, String prefixedKey, String targetId) {

        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/links/{prefixedKey}/{target}")
                        .build(sourceId, prefixedKey, targetId)
                )
                .accept(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()))
                .exchange();

    }


    static Mono<String> serialize(Model builder, RDFFormat format) {
        StringWriter sw = new StringWriter();
        Rio.write(builder, sw, format);
        return Mono.just(sw.toString());
    }


    public WebTestClient.ResponseSpec deleteEntity(String entityKey) {

        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}")
                        .build(entityKey)
                )
                .exchange()
                .expectStatus().isOk();
    }



}
