package com.bechtle.eagl.graph.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObject;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * A collection of statements
 */
public class Triples implements Serializable {

    private final Model model;

    public Triples(Collection<Statement> statements) {
        this.model = new LinkedHashModel();
        this.model.addAll(statements);
        this.model.setNamespace(new SimpleNamespace("local", "http://eagl.av360.io"));
    }
    public Triples() {
        this(Collections.emptyList());
    }


    public static Triples of(ObjectNode object, ObjectMapper mapper) throws IOException {
        RDFParser parser = Rio.createParser(RDFFormat.JSONLD);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(mapper.writeValueAsBytes(object))) {
            ContextStatementCollector collector = new ContextStatementCollector(SimpleValueFactory.getInstance());
            parser.setRDFHandler(collector);
            parser.parse(bais);

            return new Triples(collector.getStatements());
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

    }

    public Collection<Statement> getStatements() {
        return this.model;
    }


    @Override
    public String toString() {
        return getStatements().toString();
    }

    public synchronized Model getModel() {
        return this.model;
    }
}
