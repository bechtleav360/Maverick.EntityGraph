package org.av360.maverick.graph.tests.util;

import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.tests.clients.AdminTestClient;
import org.av360.maverick.graph.tests.clients.EntitiesTestClient;
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

    protected AdminTestClient adminTestClient;

    protected EntitiesTestClient entitiesTestClient;


    @Autowired
    public void setWebClient(WebTestClient webClient) {
        this.webClient = webClient;
        this.adminTestClient = new AdminTestClient(webClient);
        this.entitiesTestClient = new EntitiesTestClient(webClient);
    }

    protected void dumpStatementsAsTable(CsvConsumer csvConsumer) {
        this.printSummary("All %s statements in repository".formatted(csvConsumer.rows.size()));
        System.out.println(csvConsumer.getMapAsString());
    }

    protected void dumpStatementsAsTable(Map<String, String> headers) {

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
        this.dumpStatementsAsTable(csvConsumer);
    }



    protected void dumpStatementsAsTable() {
        this.dumpStatementsAsTable(Map.of());
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

    protected EntitiesTestClient getTestClient() {
        return this.entitiesTestClient;
    }


}
