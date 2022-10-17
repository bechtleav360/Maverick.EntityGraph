package cougar.graph.store.behaviours;

import cougar.graph.store.RepositoryType;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

public interface Resettable extends RepositoryBehaviour {

    Mono<Void> reset(Authentication authentication, RepositoryType repositoryType, GrantedAuthority requiredAuthority);


    Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Authentication authentication, GrantedAuthority requiredAuthority);
}
