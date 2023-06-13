package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.SessionContext;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

public interface Maintainable extends TripleStore {

    Mono<Void> reset(SessionContext context, GrantedAuthority requiredAuthority);


    Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, SessionContext context, GrantedAuthority requiredAuthority);
}
