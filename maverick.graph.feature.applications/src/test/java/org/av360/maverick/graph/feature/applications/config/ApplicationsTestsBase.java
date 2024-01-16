package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.client.ApplicationsTestClient;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

public abstract class ApplicationsTestsBase extends ApiTestsBase  {

    protected ApplicationsTestClient applicationsTestClient;

    private ApplicationsStore applicationsStore;



    @BeforeEach
    public void setup() {
        applicationsTestClient = new ApplicationsTestClient(super.webClient);
    }

    protected void resetRepository(String label) {
        super.printCleanUp();
        adminTestClient.reset(RepositoryType.ENTITIES, Map.of("X-Application", label));
        adminTestClient.reset(RepositoryType.TRANSACTIONS, Map.of("X-Application", label));
        adminTestClient.reset(RepositoryType.APPLICATION, Map.of());
    }

    protected void resetRepository() {
        super.printCleanUp();

        SessionContext ctx = TestSecurityConfig.createTestContext();
        Mono<Void> purge = this.applicationsStore.asMaintainable().purge(ctx.getEnvironment().setRepositoryType(RepositoryType.APPLICATION));
        StepVerifier.create(purge).verifyComplete();

        super.resetRepository();
    }
    @Autowired
    public void setApplicationsStore(ApplicationsStore applicationsStore) {
        this.applicationsStore = applicationsStore;
    }
}
