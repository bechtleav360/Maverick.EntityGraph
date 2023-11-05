package org.av360.maverick.graph.tests.clients;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.tests.util.CsvConsumer;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EntitiesTestClient {

    private final WebTestClient webClient;

    public EntitiesTestClient(WebTestClient webClient) {
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

    public RdfConsumer createValue(String entityKey, String prefixedKey, String value) {
        return this.createValue(entityKey, prefixedKey, value, true);
    }

    public RdfConsumer createValue(String entityKey, String prefixedKey, String value, boolean replace) {

        RdfConsumer consumer = new RdfConsumer(RDFFormat.JSONLD);
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{entityKey}/values/{prefixedKey}").queryParam("replace", replace)
                        .build(entityKey, prefixedKey)

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()))
                .body(BodyInserters.fromValue(value))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(consumer);
        return consumer;
    }

    public RdfConsumer createValue(IRI entityIdentifier, String prefixedKey, String value, boolean replace) {
        return this.createValue(entityIdentifier.getLocalName(), prefixedKey, value, replace);
    }

    public RdfConsumer createValue(IRI entityIdentifier, String prefixedKey, String value) {
        return this.createValue(entityIdentifier.getLocalName(), prefixedKey, value, true);
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
        return this.createEntity(file, "/api/entities", Map.of());
    }

    public RdfConsumer createEntity(Resource file, String path, Map<String, String> headersMap) {
        RDFFormat format = getFormatFromExt(file);
        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        webClient.post()
                .uri(path)
                .contentType(RdfUtils.getMediaType(format))
                .accept(RdfUtils.getMediaType(format))
                .body(BodyInserters.fromResource(file))
                .headers(headers -> headersMap.forEach(headers::add))
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

    public CsvConsumer listAllStatements(Map<String, String> headers) {

        CsvConsumer csvConsumer = new CsvConsumer();
        webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path("/api/query/select")
                        .queryParam("repository", "entities")
                        .build()
                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .header("X-API-KEY", "test")
                .headers(c -> headers.forEach((k,v) -> c.put(k, List.of(v))))
                .body(BodyInserters.fromValue("SELECT DISTINCT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object }"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(csvConsumer);

        return csvConsumer;
    }

    public CsvConsumer listAllStatements() {
        return this.listAllStatements(Map.of());
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

    public RdfConsumer listValues(IRI sourceIdentifier) {
        RDFFormat format = RDFFormat.TURTLESTAR;

        RdfConsumer rdfConsumer = new RdfConsumer(format, false);
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/values")
                        .build(sourceIdentifier.getLocalName())
                )
                .accept(RdfUtils.getMediaType(format))
                .header("X-API-KEY", "test")
                .exchange()
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;
    }


    public RdfConsumer listValues(IRI sourceIdentifier, String property) {
        RDFFormat format = RDFFormat.TURTLESTAR;

        RdfConsumer rdfConsumer = new RdfConsumer(format, false);
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/values")
                        .queryParam("property", property)
                        .build(sourceIdentifier.getLocalName())
                )
                .accept(RdfUtils.getMediaType(format))
                .header("X-API-KEY", "test")
                .exchange()
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;
    }

    public RdfConsumer readEntity(String entityKey) {
        RDFFormat format = RDFFormat.TURTLE;

        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        this.checkEntity(entityKey, format)
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;
    }

    public RdfConsumer readEntity(IRI entityIdentifier) {
        return this.readEntity(entityIdentifier.getLocalName());
    }

    public WebTestClient.ResponseSpec deleteLink(String sourceId, String prefixedKey, String targetId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/links/{prefixedKey}/{target}")
                        .build(sourceId, prefixedKey, targetId)
                )
                .exchange();
    }

    public WebTestClient.ResponseSpec deleteValueDetail(IRI sourceIdentifier, String valueProperty, String detailProperty) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/values/{valueProperty}/details/{detailProperty}")
                        .build(sourceIdentifier, valueProperty, detailProperty)
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


    public WebTestClient.ResponseSpec deleteValue(IRI entityIri, String prefixedProperty) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/{prefixedProperty}")
                        .build(entityIri.getLocalName(), prefixedProperty)

                )
                .exchange();
    }

    public WebTestClient.ResponseSpec deleteValueByHash(IRI entityIri, String prefixedProperty, String hash) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/{prefixedProperty}")
                        .queryParam("hash", hash)
                        .build(entityIri.getLocalName(), prefixedProperty)

                )
                .exchange();
    }

    public WebTestClient.ResponseSpec deleteValueByTag(IRI entityIri, String prefixedProperty, String tag) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/{prefixedProperty}")
                        .queryParam("lang", tag)
                        .build(entityIri.getLocalName(), prefixedProperty)

                )
                .exchange();
    }



    public WebTestClient.ResponseSpec addDetail(IRI entityIdentifier, String valueProperty, String detailProperty, String detailValue, @Nullable RdfConsumer consumer) {
        WebTestClient.ResponseSpec exchange = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/values/{valueProperty}/details/{detailProperty}")
                        .build(entityIdentifier.getLocalName(), valueProperty, detailProperty)

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType(RDFFormat.TURTLE.getDefaultMIMEType()))
                .body(BodyInserters.fromValue(detailValue))
                .exchange();

        if(Objects.nonNull(consumer)) {
            exchange.expectBody().consumeWith(consumer);
        }

        return exchange;
    }


    public WebTestClient.ResponseSpec addDetail(IRI entityIdentifier, String valueProperty, String detailProperty, String detailValue, String hash,  @Nullable RdfConsumer consumer) {
        WebTestClient.ResponseSpec exchange = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}/values/{valueProperty}/details/{detailProperty}")
                        .queryParam("hash", hash)
                        .build(entityIdentifier.getLocalName(), valueProperty, detailProperty)

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType(RDFFormat.TURTLE.getDefaultMIMEType()))
                .body(BodyInserters.fromValue(detailValue))
                .exchange();

        if(Objects.nonNull(consumer)) {
            exchange.expectBody().consumeWith(consumer);
        }

        return exchange;
    }


}
