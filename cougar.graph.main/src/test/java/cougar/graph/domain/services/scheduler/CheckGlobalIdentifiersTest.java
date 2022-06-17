package cougar.graph.domain.services.scheduler;

import cougar.graph.TestConfigurations;
import cougar.graph.model.security.Authorities;
import cougar.graph.store.rdf.models.Transaction;
import cougar.graph.services.schedulers.replaceGlobalIdentifiers.ScheduledReplaceGlobalIdentifiers;
import cougar.graph.feature.admin.domain.AdminServices;
import cougar.graph.services.services.QueryServices;
import cougar.graph.store.EntityStore;
import cougar.graph.store.TransactionsStore;
import cougar.graph.tests.util.TestsBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
class CheckGlobalIdentifiersTest extends TestsBase {
    @Autowired
    private WebTestClient webClient;

    private ScheduledReplaceGlobalIdentifiers scheduled;

    @Autowired
    private AdminServices adminServices;

    @Autowired
    EntityStore entityStore;

    @Autowired
    QueryServices queryServices;

    @Autowired
    TransactionsStore transactionsStore;

    @Test
    void checkForGlobalIdentifiers() {

        createEntities();
        scheduled = new ScheduledReplaceGlobalIdentifiers(queryServices, entityStore, transactionsStore);

        Flux<Transaction> action = this.createEntities().thenMany(
                scheduled.checkForGlobalIdentifiers(new TestingAuthenticationToken("", "", List.of(Authorities.ADMIN, Authorities.USER)))
                        .doOnNext(transaction -> log.trace("Completed transaction"))
        );


        Duration duration = StepVerifier.create(action)
                .thenAwait(Duration.of(2, ChronoUnit.SECONDS))
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

    private Mono<Void> createEntities() {
        Resource file = new ClassPathResource("requests/create-esco.ttl");

        Flux<DataBuffer> read = DataBufferUtils.read(file, new DefaultDataBufferFactory(), 128);

        return adminServices.importEntities(read, "text/turtle", new TestingAuthenticationToken("test", "test", List.of(Authorities.ADMIN)));

    }
}