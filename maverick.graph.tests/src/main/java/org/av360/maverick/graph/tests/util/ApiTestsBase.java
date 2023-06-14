package org.av360.maverick.graph.tests.util;

import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;
import java.util.Map;
@AutoConfigureWebTestClient(timeout = "360000")
public abstract class ApiTestsBase extends TestsBase {


    protected WebTestClient webClient;

    @Autowired
    public void setWebClient(WebTestClient webClient) {
        this.webClient = webClient;
    }


    protected void dump(Map<String, String> headers) {

        CsvConsumer csvConsumer = new CsvConsumer();
        webClient
                .post()
                .uri("/api/query/select")
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .headers(c -> headers.forEach((k,v) -> c.put(k, List.of(v))))
                .body(BodyInserters.fromValue("SELECT DISTINCT * WHERE { ?s ?p ?o }"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);

        System.out.println(csvConsumer.getAsString());

    }

    protected void dump() {
        this.dump(Map.of());
    }





    protected RdfConsumer upload(String path) {
        Resource file = new ClassPathResource(path);
        return this.upload(file);

    }

    protected RdfConsumer upload(Resource file) {

        RDFFormat format = null;
        if (file.getFilename().endsWith("ttl")) format = RDFFormat.TURTLE;
        if (file.getFilename().endsWith("jsonld")) format = RDFFormat.JSONLD;
        if (file.getFilename().endsWith("n3")) format = RDFFormat.N3;
        if (file.getFilename().endsWith("xml")) format = RDFFormat.RDFXML;

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

    protected RdfConsumer loadEntity(IRI subject) {

        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE, true);
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}")
                        .build(
                                subject.getLocalName()
                        )

                )
                .accept(RdfUtils.getMediaType(RDFFormat.TURTLE))
                .header("X-API-KEY", "test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;

    }
}
