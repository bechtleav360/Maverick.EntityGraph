package com.bechtle.eagl.graph.api;

import com.bechtle.eagl.graph.subscriptions.api.dto.Requests;
import com.bechtle.eagl.graph.subscriptions.api.dto.Responses;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
class SubscriptionsTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void createSubscription() {

        webClient.post()
                .uri("/api/admin/subscriptions")
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody()
                .jsonPath("$.identifier").isNotEmpty();
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
        Responses.Subscription re = webClient.post()
                .uri("/api/admin/subscriptions")
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody(Responses.Subscription.class)
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
                .expectBody(Responses.ApiKey.class);
    }

    @Test
    void revokeToken() {
    }
}