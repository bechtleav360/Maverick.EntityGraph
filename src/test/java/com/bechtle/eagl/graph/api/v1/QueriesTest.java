package com.bechtle.eagl.graph.api.v1;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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