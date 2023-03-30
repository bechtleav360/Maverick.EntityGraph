package io.av360.maverick.graph.feature.applications.config;

import io.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.tests.config.TestSecurityConfig;
import io.av360.maverick.graph.tests.util.ApiTestsBase;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

public class ApplicationsTestsBase extends ApiTestsBase  {


    private ApplicationsStore applicationsStore;

    @Autowired
    public void setStores(ApplicationsStore applicationsStore) {
        this.applicationsStore = applicationsStore;
    }

    @Override
    protected void resetRepository() {
        StepVerifier.create(this.applicationsStore.reset(TestSecurityConfig.createAuthenticationToken(), Authorities.SYSTEM)).verifyComplete();

        super.resetRepository();
    }
}
