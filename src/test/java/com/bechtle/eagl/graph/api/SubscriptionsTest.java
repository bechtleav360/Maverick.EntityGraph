package com.bechtle.eagl.graph.api;

import com.bechtle.eagl.graph.features.multitenancy.api.dto.Requests;
import com.bechtle.eagl.graph.features.multitenancy.api.dto.Responses;
import config.TestConfigurations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
class SubscriptionsTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void createSubscription() {

        Requests.RegisterApplicationRequest req = new Requests.RegisterApplicationRequest("test", false);

        webClient.post()
                .uri("/api/admin/subscriptions")
                .body(BodyInserters.fromValue(req))
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody()
                .jsonPath("$.key").isNotEmpty();
                /*.consumeWith(res -> {
                    String str = new String(res.getResponseBody());
                    Assertions.assertTrue(StringUtils.hasLength(str));
                });
               /* .consumeWith(res -> {
                    Assertions.assertTrue(StringUtils.hasLength(res.getResponseBody().identifier()));
                });*/
        //.jsonPath("$.identifier").isNotEmpty();

    }

    @Test
    void listSubscription() {


        webClient.get()
                .uri("/api/admin/subscriptions")
                .exchange()
                .expectStatus().isOk()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    String s = new String(entityExchangeResult.getResponseBody());
                    Assertions.assertTrue(StringUtils.hasLength(s));
                })
                //.jsonPath("$.identifier").isNotEmpty()

                .jsonPath("$").isArray()
                .jsonPath("$.size()").isEqualTo(1);
    }

    @Test
    void generateKey() {

        Requests.RegisterApplicationRequest req = new Requests.RegisterApplicationRequest("test", false);

        Responses.ApplicationResponse re = webClient.post()
                .uri("/api/admin/subscriptions")
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(re.key());

        Requests.CreateApiKeyRequest request = new Requests.CreateApiKeyRequest("test");

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/subscriptions/{id}/keys")
                        .build(re.key())
                )
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Responses.ApiKeyResponse.class);
    }

    @Test
    void revokeToken() {
    }
}