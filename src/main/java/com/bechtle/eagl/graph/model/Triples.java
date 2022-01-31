package com.bechtle.eagl.graph.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObject;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of statements
 */
public class Triples {

    private final Collection<Statement> statements;
    private final Map<String, String> namespaces;

    public Triples(Collection<Statement> statements, Map<String, String> namespaces) {
        this.statements = statements;
        this.namespaces = namespaces;
    }
    public Triples() {
        this.statements = new ArrayList<>();
        this.namespaces = new HashMap<>();
    }


    public static Triples of(ObjectNode object, ObjectMapper mapper) throws IOException {
        RDFParser parser = Rio.createParser(RDFFormat.JSONLD);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(mapper.writeValueAsBytes(object))) {
            ContextStatementCollector collector = new ContextStatementCollector(SimpleValueFactory.getInstance());
            parser.setRDFHandler(collector);
            parser.parse(bais);

            return new Triples(collector.getStatements(), collector.getNamespaces());
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

    }

    public Collection<Statement> getStatements() {
        return statements;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }
}
