package io.av360.maverick.graph.tests.util;

import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

public abstract class TestsBase {

    protected static ValueFactory vf = SimpleValueFactory.getInstance();


    protected WebTestClient webClient;

    @Autowired
    public void setWebClient(WebTestClient webClient) {
        this.webClient = webClient;
    }


    protected void dump() {

        CsvConsumer csvConsumer = new CsvConsumer();
        webClient
                .post()
                .uri("/api/query/select")
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .body(BodyInserters.fromValue("SELECT DISTINCT * WHERE { ?s ?p ?o }"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);

        System.out.println(csvConsumer.getAsString());

    }


    protected void resetRepository(String repositoryType) {
        webClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/admin/bulk/reset")
                                .queryParam("name", repositoryType)
                                .build()

                )
                .exchange()
                .expectStatus().isAccepted();
    }

    protected RdfConsumer upload(String path) {
        Resource file = new ClassPathResource(path);
        return this.upload(file);

    }

    protected RdfConsumer upload(Resource file) {
        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE, true);

        webClient.post()
                .uri("/api/entities")
                .contentType(RdfUtils.getMediaType(RDFFormat.TURTLE))
                .accept(RdfUtils.getMediaType(RDFFormat.TURTLE))
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
