package org.av360.maverick.graph.feature.applications.client;

import org.av360.maverick.graph.feature.applications.controller.dto.Requests;
import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;
import java.util.Set;

public class ApplicationsTestClient {

    private final WebTestClient webClient;

    public ApplicationsTestClient(WebTestClient webClient) {
        this.webClient = webClient;
    }


    public WebTestClient.ResponseSpec createApplication(String label, ApplicationFlags flags) {
        return this.createApplication(label, flags, Set.of());
    }

    public WebTestClient.ResponseSpec createApplication(String label, ApplicationFlags flags, Set<String> tags) {

        Requests.RegisterApplicationRequest req = new Requests.RegisterApplicationRequest(label, flags, tags, Map.of());

        return webClient.post()
                .uri("/api/applications")
                .body(BodyInserters.fromValue(req))
                .exchange();
    }

    public WebTestClient.ResponseSpec getApplication(String applicationKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/applications/{id}")
                        .build(applicationKey)
                )
                .exchange();
    }


    public WebTestClient.ResponseSpec listApplications() {

        return webClient.get()
                .uri("/api/applications")
                .exchange();
    }

    public WebTestClient.ResponseSpec listSubscriptions(String applicationKey) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/applications/{id}/subscriptions")
                        .build(applicationKey)
                )
                .exchange();
    }


    public WebTestClient.ResponseSpec createSubscription(String subscriptionLabel, String applicationKey) {
        Requests.CreateApiKeyRequest request = new Requests.CreateApiKeyRequest(subscriptionLabel);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/applications/{id}/subscriptions")
                        .build(applicationKey)
                )
                .bodyValue(request)
                .exchange();


        //        .expectStatus().isCreated()
        //        .expectBody(Responses.ApiKeyResponse.class);
    }



}
