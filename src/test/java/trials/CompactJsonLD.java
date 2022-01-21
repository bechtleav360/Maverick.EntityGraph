package trials;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import utils.TestRepository;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class CompactJsonLD {

    @Test
    public void load() throws IOException, JsonLdError {

        try (TestRepository testRepository = new TestRepository()) {
            Resource file = new ClassPathResource("schema/sample.ttl");
            Resource contextResource = new ClassPathResource("schema/context.json");

            testRepository.load(file);
            String jsonld = testRepository.dump(RDFFormat.JSONLD);


            JsonDocument of = JsonDocument.of(new StringReader(jsonld));
            JsonDocument context = JsonDocument.of(contextResource.getInputStream());

            JsonObject obj = JsonLd.compact(of, context)
                    .get();

            Map<String,String> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, "");
            Json.createWriterFactory(config).createWriter(System.out).write(obj);
  ;
        }
    }
}
