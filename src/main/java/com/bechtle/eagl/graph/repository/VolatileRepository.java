package com.bechtle.eagl.graph.repository;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Single use in-memory repository. Use it only to load and dump, e.g. in a different format
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class VolatileRepository implements AutoCloseable {

    private final Repository repository;
    private final ObjectMapper objectMapper;

    public VolatileRepository(@Autowired ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        repository = new SailRepository(new MemoryStore());
    }

    public VolatileRepository load(Resource resource) throws IOException {
        try (RepositoryConnection conn = this.repository.getConnection()) {
            try (InputStream input = resource.getInputStream()) {
                conn.add(input, RDFFormat.TURTLE);
            } catch (IOException e) {
                throw e;
            }
        }
        return this;
    }

    public VolatileRepository load(String resource, RDFFormat format) throws IOException {
        try (RepositoryConnection conn = this.repository.getConnection()) {
            RDFParser parser = Rio.createParser(format);
            try (Reader input = new StringReader(resource)) {
                conn.add(input, format);
            } catch (IOException e) {
                throw e;
            }
        }
        return this;
    }


    public VolatileRepository load(ObjectNode json) throws IOException {
        try (RepositoryConnection conn = this.repository.getConnection()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(this.objectMapper.writeValueAsBytes(json))) {
                conn.add(bais, RDFFormat.JSONLD);
            }

        } catch (IOException e) {
            throw e;
        }
        return this;
    }

    public String serialize(RDFFormat format) {
        try (RepositoryConnection conn = this.repository.getConnection()) {
            StringWriter sw = new StringWriter();

            RDFWriter writer = Rio.createWriter(format, sw);
            conn.export(writer);
            return sw.toString();
        }
    }

    public JsonDocument export() throws IOException, JsonLdError {
        try (RepositoryConnection conn = this.repository.getConnection()) {
            try (StringWriter sw = new StringWriter()) {
                RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, sw);
                conn.export(writer);

                try (StringReader sr = new StringReader(sw.toString())) {
                    return JsonDocument.of(sr);
                }

            } catch (IOException | JsonLdError e) {
                throw e;
            }
        }
    }


    @Override
    public void close() {
        this.repository.shutDown();
    }


}
