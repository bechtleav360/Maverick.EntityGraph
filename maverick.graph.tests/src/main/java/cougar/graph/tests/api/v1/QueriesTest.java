package cougar.graph.tests.api.v1;


import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public
interface QueriesTest {

    /**
     * <pre>POST /api/query/native</pre>
     */
    void runSparqlQuery();

    /**
     * <pre>POST /api/query/native</pre>
     */
    void runInvalidSparqlQuery();

}