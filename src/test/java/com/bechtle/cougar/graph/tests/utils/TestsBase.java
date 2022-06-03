package com.bechtle.cougar.graph.tests.utils;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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


    protected void dump()  {

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
}
