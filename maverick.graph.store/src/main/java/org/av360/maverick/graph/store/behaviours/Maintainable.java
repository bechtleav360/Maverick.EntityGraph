package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Statement;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface Maintainable extends Commitable {

    Mono<Void> purge(Environment environment);

    Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Environment environment);

    default Mono<Void> importStatements(Collection<Statement> statements, Environment environment) {
        Transaction trx = new RdfTransaction().inserts(statements);
        return this.commit(trx, environment).then();
    }

}
