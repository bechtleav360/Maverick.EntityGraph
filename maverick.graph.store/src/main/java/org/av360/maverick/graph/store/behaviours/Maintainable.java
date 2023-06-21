package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

public interface Maintainable extends TripleStore {

    Mono<Void> reset(Environment environment);


    Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Environment environment);
}
