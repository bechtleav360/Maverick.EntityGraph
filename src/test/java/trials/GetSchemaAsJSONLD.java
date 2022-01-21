package trials;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.api.ExpansionApi;
import com.apicatalog.jsonld.api.FromRdfApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.DocumentParser;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.rdf.RdfDataset;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.shaded.com.google.common.net.MediaType;
import jakarta.json.JsonArray;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import utils.TestRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@SpringBootTest
public class GetSchemaAsJSONLD {

    @Test
    public void load() throws IOException, JsonLdError {

        try (TestRepository testRepository = new TestRepository()) {
            Resource file = new ClassPathResource("schema/sample.ttl");
            testRepository.load(file);
            String jsonld = testRepository.dump(RDFFormat.JSONLD);
            System.out.println(jsonld);

            JsonDocument of = JsonDocument.of(new StringReader(jsonld));

            JsonArray jsonArray = JsonLd.expand(of)
                    .get();
            jsonArray.stream().forEach(jsonValue -> {
                System.out.println(jsonValue.toString());
            });
        }
    }
}
