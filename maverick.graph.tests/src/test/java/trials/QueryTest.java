package trials;

import com.apicatalog.jsonld.JsonLdError;
import org.av360.maverick.graph.store.rdf4j.repository.TestRepository;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QueryTest {

    @Test
    public void load() throws IOException, JsonLdError {

        final String queryStr = "SELECT DISTINCT * WHERE { ?s ?p ?o }";

        TestRepository testRepository = new TestRepository();

        Variable s = SparqlBuilder.var("s");
        Variable p = SparqlBuilder.var("p");
        Variable o = SparqlBuilder.var("o");
        SelectQuery q = Queries.SELECT()
                .where(s.has(p, o));

        try (RepositoryConnection conn = testRepository.getConnection()) {
            Resource file = new ClassPathResource("schema/sparql-examples.ttl");
            testRepository.load(file);

            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q.getQueryString());
            try (TupleQueryResult result = query.evaluate()) {
                assertNotNull(result);

                int count = 0;
                while (result.hasNext()) {
                    result.next();
                    count++;
                }
                assertEquals(108, count);
            }
        }
    }


    @Test
    public void loadWithQueryBuilder() throws IOException {

        final String queryStr = "SELECT DISTINCT * WHERE { ?s ?p ?o }";

        TestRepository testRepository = new TestRepository();

        try (RepositoryConnection conn = testRepository.getConnection()) {
            Resource file = new ClassPathResource("schema/sparql-examples.ttl");
            testRepository.load(file);

            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                assertNotNull(result);

                int count = 0;
                while (result.hasNext()) {
                    result.next();
                    count++;
                }
                assertEquals(108, count);
            }
        }
    }

    @Test
    public void test() throws Exception {
        SailRepository sailRepository = new SailRepository(new MemoryStore());

        final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";


        try (SailRepositoryConnection conn = sailRepository.getConnection()) {
            conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                assertNotNull(result);

                int count = 0;
                while (result.hasNext()) {
                    result.next();
                    count++;
                }
                // result should be both a and b.
                assertEquals(2, count);
            }
        }
    }


    @Test
    public void testWithQueryBuilder() throws Exception {
        SailRepository sailRepository = new SailRepository(new MemoryStore());

        final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";


        Variable node = SparqlBuilder.var("node");
        Variable subPersistent = SparqlBuilder.var("f");
        Variable sublabel = SparqlBuilder.var("g");


        try (SailRepositoryConnection conn = sailRepository.getConnection()) {
            conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
            try (TupleQueryResult result = query.evaluate()) {
                assertNotNull(result);

                int count = 0;
                while (result.hasNext()) {
                    result.next();
                    count++;
                }
                // result should be both a and b.
                assertEquals(2, count);
            }
        }
    }
}
