package cougar.graph.main.trials;

import cougar.graph.tests.util.TestRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@SpringBootTest
@SpringBootConfiguration
public class LoadTTLInSchemaGraph {

    @Test
    public void createRepo() throws IOException {
        try (TestRepository testRepository = new TestRepository()) {
            Resource file = new ClassPathResource("schema/sample.ttl");
            testRepository.load(file);
            System.out.println(testRepository.dump(RDFFormat.TURTLESTAR));
        }
    }
}
