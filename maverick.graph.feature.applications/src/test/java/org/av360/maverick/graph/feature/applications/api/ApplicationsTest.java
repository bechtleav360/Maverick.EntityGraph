package org.av360.maverick.graph.feature.applications.api;

import org.av360.maverick.graph.feature.applications.api.dto.Responses;
import org.av360.maverick.graph.feature.applications.config.ApplicationsTestsBase;
import org.av360.maverick.graph.feature.applications.services.model.ApplicationFlags;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
class ApplicationsTest extends ApplicationsTestsBase {


    @Autowired
    private QueryServices queryServices;




    @AfterEach
    public void resetRepository() {
        super.resetRepository("test_app");
    }

    @Test
    public void createPublicApplication() {
        super.printStep();

        super.applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true, false))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.key").isNotEmpty();
    }



    @Test
    public void listApplications() {

        super.printStep();

        super.applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true, false))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        super.applicationsTestClient.createApplication("test_app_1", new ApplicationFlags(true, true, false))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        super.applicationsTestClient.createApplication("test_app_2", new ApplicationFlags(false, false, false))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        String string = super.applicationsTestClient.listApplications()
                .expectStatus().isOk()
                .expectBody()
                .toString();
        System.out.println(string);

        byte[] sr = super.applicationsTestClient.listApplications()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(obj -> {
                    Assertions.assertNotNull(obj);
                    if(obj instanceof JSONArray array) {
                        Assertions.assertEquals(array.length(), 3);
                    }


                    // Assertions.assertEquals(array.size(), 3);

                })
                .jsonPath("$.size()").isEqualTo(3)
                .returnResult().getResponseBody();

        super.printResult("Result", new String(sr));


    }


    @Test
    public void getApplication() {

        super.printStart("Read node");

        Responses.ApplicationResponse app = super.applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true, false))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();



        Variable s = SparqlBuilder.var("s");
        Variable p = SparqlBuilder.var("p");
        Variable o = SparqlBuilder.var("o");
        SelectQuery q = Queries.SELECT(s, p, o).all().where(s.has(p, o));
        List<BindingSet> block = this.queryServices.queryValues(q, RepositoryType.SYSTEM, new SessionContext().setSystemAuthentication()).collectList().block();

        super.printStep();
        super.applicationsTestClient.getApplication(app.key())
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.key").isEqualTo(app.key());

    }

    @Test
    public void createSubscription() {
        super.printStart("Create Subscription");
        Responses.ApplicationResponse re = super.applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, false, false))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(re);
        Assertions.assertNotNull(re.key());

        super.printStep();

        super.applicationsTestClient.createSubscription("test-subscription", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

    }

    @Test
    public void listSubscriptions() {
        Responses.ApplicationResponse re = super.applicationsTestClient.createApplication("test-public", new ApplicationFlags(false, false, false))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        super.applicationsTestClient.createSubscription("test-sub-1", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        super.applicationsTestClient.createSubscription("test-sub-2", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        super.applicationsTestClient.createSubscription("test-sub-3", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        super.applicationsTestClient.listSubscriptions(re.key())
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.size()").isEqualTo(3);


    }


}
