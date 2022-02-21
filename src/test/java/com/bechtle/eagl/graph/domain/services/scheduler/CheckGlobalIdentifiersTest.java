package com.bechtle.eagl.graph.domain.services.scheduler;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.domain.services.AdminServices;
import config.TestConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import utils.RdfConsumer;

import java.time.Duration;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
class CheckGlobalIdentifiersTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private CheckGlobalIdentifiers scheduled;

    @Autowired
    private AdminServices adminServices;


    @Test
    void checkForGlobalIdentifiers() {
        createEntities();

        Flux<Transaction> action = scheduled.checkForGlobalIdentifiers().doOnNext(transaction -> {
            log.trace("Completed transaction");
        });

        Duration duration = StepVerifier.create(action)
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .verifyComplete();

        Assert.isTrue(duration.getNano() > 0, "no transaction time");


    }

    private void createEntities() {
        Resource file = new ClassPathResource("data/v1/requests/create-esco.ttl");

        Flux<DataBuffer> read = DataBufferUtils.read(file, new DefaultDataBufferFactory(), 128);

        Mono<Void> voidMono = adminServices.importEntities(read, "text/turtle");

        StepVerifier.create(voidMono).verifyComplete();
    }
}