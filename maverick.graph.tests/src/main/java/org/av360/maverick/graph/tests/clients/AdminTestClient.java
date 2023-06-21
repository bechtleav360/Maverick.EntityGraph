package org.av360.maverick.graph.tests.clients;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.core.io.Resource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

public class AdminTestClient {

    private final WebTestClient webClient;

    public AdminTestClient(WebTestClient webClient) {
        this.webClient = webClient;
    }


    public WebTestClient.ResponseSpec reset(RepositoryType repositoryType, Map<String, String> headersMap) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("repository", repositoryType.toString().toLowerCase())
                        .path("/api/admin/reset")
                        .build()
                )
                .header("X-API-KEY", "test")
                .headers(headers -> headersMap.forEach(headers::add))
                .exchange()
                .expectStatus().isAccepted();
    }

    public WebTestClient.ResponseSpec importTurtleFile(Resource file, Map<String, String> headersMap) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("repository", RepositoryType.ENTITIES.toString().toLowerCase())
                        .path("/api/admin/import/content")
                        .build()
                )
                .accept(RdfMimeTypes.TURTLE)
                .body(BodyInserters.fromResource(file))
                .header("X-API-KEY", "test")
                .headers(headers -> headersMap.forEach(headers::add))
                .exchange()
                .expectStatus().isAccepted();
    }
}
