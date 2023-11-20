package org.av360.maverick.graph.tests.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.StringWriter;

@Slf4j
public abstract class TestsBase {

    protected static ValueFactory vf = SimpleValueFactory.getInstance();


    private IndividualsStore entityStore;

    private TransactionsStore transactionsStore;

    static {
        Hooks.onOperatorDebug();
    }


    private static String ECHO_LINE_PREFIX = "-----------";



    protected IRI createIRIFrom(String url) {
        return vf.createIRI(url);
    }

    protected Literal createLiteralFrom(String value) {
        return vf.createLiteral(value);
    }

    @Autowired
    public void setStores(IndividualsStore entityStore, TransactionsStore transactionsStore) {
        this.entityStore = entityStore;
        this.transactionsStore = transactionsStore;
    }

    int steps = 0;
    protected void printStart(String label) {
        echo("Starting: " + label);
        steps = 0;
    }

    private void echo(String message) {
        String formatted = "\n%s %s\n".formatted(StringUtils.leftPad("|", 10, "-"), StringUtils.rightPad(message+" |", 100, "-"));
        System.out.println(formatted);
    }
    public void printStep() {
        echo("Step: "+ ++steps);
    }

    public void printStep(String detail) {
        echo("Step: "+ ++steps+" ("+detail+")");
    }

    public void printSummary(String detail) {
        echo(detail);
    }

    public void printCleanUp() {
        echo("Cleaning up");
    }

    public void printResult(String message, String dump) {
        this.echo(message);
        System.out.println(dump);
    }

    public void printModel(Model md, RDFFormat rdfFormat) {
       String m = this.dumpModel(md, rdfFormat);
       this.printSummary("Content of current model");
       System.out.println(m);
    }

    public String dumpModel(Model md, RDFFormat rdfFormat) {
        StringWriter sw = new StringWriter();
        RDFWriter writer = Rio.createWriter(rdfFormat, sw);
        writer.startRDF();
        md.forEach(writer::handleStatement);
        writer.endRDF();
        return sw.toString();
    }

    protected void resetRepository() {
        SessionContext ctx = TestSecurityConfig.createTestContext();
        this.resetRepository(ctx);
    }


    protected void resetRepository(SessionContext ctx) {
        Mono<Void> r1 = this.entityStore.asMaintainable().purge(ctx.getEnvironment())
                .then(this.transactionsStore.purge(ctx.getEnvironment()));

        StepVerifier.create(r1).verifyComplete();
    }



}
