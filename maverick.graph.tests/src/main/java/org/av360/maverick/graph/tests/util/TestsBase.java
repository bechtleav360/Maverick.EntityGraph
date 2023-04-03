package org.av360.maverick.graph.tests.util;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.EntityStore;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.StringWriter;

@Slf4j
public abstract class TestsBase {

    protected static ValueFactory vf = SimpleValueFactory.getInstance();


    private EntityStore entityStore;

    private TransactionsStore transactionsStore;

    static {
        Hooks.onOperatorDebug();
    }




    protected IRI createIRIFrom(String url) {
        return vf.createIRI(url);
    }

    protected Literal createLiteralFrom(String value) {
        return vf.createLiteral(value);
    }

    @Autowired
    public void setStores(EntityStore entityStore, TransactionsStore transactionsStore) {
        this.entityStore = entityStore;
        this.transactionsStore = transactionsStore;
    }

    int steps = 0;
    protected void printStart(String label) {
        System.out.println("\n----------- Starting: "+label+" --------------------------------------------------------------------------------------\n");
        steps = 0;
    }

    public void printStep() {
        System.out.println("\n----------- Step: "+ ++steps +" --------------------------------------------------------------------------------------");
    }

    public void printModel(Model md, RDFFormat rdfFormat) {
       String m = this.dumpModel(md, rdfFormat);
       log.trace("Current model: \n {}", m);
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
        TestingAuthenticationToken token = TestSecurityConfig.createAuthenticationToken();
        Mono<Void> r1 = this.entityStore.reset(token, Authorities.SYSTEM)
                .then(this.transactionsStore.reset(token, Authorities.SYSTEM));

        StepVerifier.create(r1).verifyComplete();
    }



}
