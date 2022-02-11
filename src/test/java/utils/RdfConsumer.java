package utils;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class RdfConsumer implements Consumer<EntityExchangeResult<byte[]>> {

    private final RDFParser parser;
    private  ContextStatementCollector collector;

    public RdfConsumer(RDFFormat format) {
        parser = Rio.createParser(format);

    }

    @Override
    public void accept(EntityExchangeResult<byte[]> entityExchangeResult) {

        collector = new ContextStatementCollector(SimpleValueFactory.getInstance());
        parser.setRDFHandler(collector);

        Assert.notNull(entityExchangeResult, "Null result");
        Assert.notNull(entityExchangeResult.getResponseBody(), "Null response body");


        try(ByteArrayInputStream bais = new ByteArrayInputStream(entityExchangeResult.getResponseBody())) {
            parser.parse(bais);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
}
