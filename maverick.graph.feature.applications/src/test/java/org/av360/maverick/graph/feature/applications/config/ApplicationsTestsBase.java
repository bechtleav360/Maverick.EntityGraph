package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.client.ApplicationsTestClient;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class ApplicationsTestsBase extends ApiTestsBase  {

    protected ApplicationsTestClient applicationsTestClient;

    @BeforeEach
    public void setup() {
        applicationsTestClient = new ApplicationsTestClient(super.webClient);
    }

    protected void resetRepository(String label) {
        super.printCleanUp();
        adminTestClient.reset(RepositoryType.ENTITIES, Map.of("X-Application", label));
        adminTestClient.reset(RepositoryType.TRANSACTIONS, Map.of("X-Application", label));
        adminTestClient.reset(RepositoryType.SYSTEM, Map.of());
    }

    protected void resetRepository() {

        super.printCleanUp();
        adminTestClient.reset(RepositoryType.SYSTEM, Map.of());
        super.resetRepository();
    }
}
