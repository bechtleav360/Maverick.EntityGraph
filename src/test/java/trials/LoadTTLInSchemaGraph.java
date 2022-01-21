package trials;

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
import org.xmlunit.builder.Input;
import utils.TestRepository;

import java.io.IOException;
import java.io.InputStream;

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
