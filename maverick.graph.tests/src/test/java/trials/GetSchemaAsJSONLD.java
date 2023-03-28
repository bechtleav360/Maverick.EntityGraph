package trials;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import io.av360.maverick.graph.store.rdf4j.repository.TestRepository;
import jakarta.json.JsonArray;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
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
