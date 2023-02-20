package io.av360.maverick.graph.tests.util;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

@Slf4j
public class RdfConsumer implements Consumer<EntityExchangeResult<byte[]>> {

    private final RDFParser parser;
    private boolean printStatements = false;
    private ContextStatementCollector collector;

    public RdfConsumer(RDFFormat format) {
        parser = Rio.createParser(format);
    }

    public RdfConsumer(RDFFormat format, boolean printStatements) {
        this(format);
        this.printStatements = printStatements;
    }


    public String dump(RDFFormat rdfFormat) {
        StringWriter sw = new StringWriter();
        RDFWriter writer = Rio.createWriter(rdfFormat, sw);
        writer.startRDF();
        this.asModel().forEach(writer::handleStatement);
        writer.endRDF();
        return sw.toString();
    }

    @Override
    public void accept(EntityExchangeResult<byte[]> entityExchangeResult) {

        collector = new ContextStatementCollector(SimpleValueFactory.getInstance());
        parser.setRDFHandler(collector);

        Assert.notNull(entityExchangeResult, "Null result");
        Assert.notNull(entityExchangeResult.getResponseBody(), "Null response body");


        try (ByteArrayInputStream bais = new ByteArrayInputStream(entityExchangeResult.getResponseBody())) {
            parser.parse(bais);
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (printStatements) {
            StringBuilder sb = new StringBuilder();
            collector.getStatements().forEach(statement -> sb.append(statement).append('\n'));
            log.trace("Statements in model: \n {}", sb);
        }
    }

    public void assertLength(int expected) {

    }


    public Collection<Statement> getStatements() {
        return this.collector.getStatements();
    }

    public Map<String, String> getNamespaces() {
        return this.collector.getNamespaces();
    }

    public Model asModel() {
        LinkedHashModel statements = new LinkedHashModel();
        statements.addAll(this.getStatements());
        return statements;
    }

    public boolean hasStatement(Resource subject, IRI predicate, Value object) {
        return this.asModel().getStatements(subject, predicate, object).iterator().hasNext();
    }

    public Statement findStatement(Resource s, IRI p, IRI o) {
        return StreamSupport.stream(this.asModel().getStatements(s, p, o).spliterator(), false).findFirst().orElseThrow();
    }

    public int countValues(Resource s, IRI p) {
        return Long.valueOf(StreamSupport.stream(this.asModel().getStatements(s, p, null).spliterator(), false).count()).intValue();
    }

    public void print() {
        System.out.println(this.dump(RDFFormat.TURTLE));
    }
}
