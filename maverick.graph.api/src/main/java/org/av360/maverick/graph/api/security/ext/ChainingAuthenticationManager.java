package org.av360.maverick.graph.api.security.ext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "graph.ctrl.cfg.sec.mgr.chain")
public class ChainingAuthenticationManager implements ReactiveAuthenticationManager {

    private final List<ReactiveAuthenticationManager> delegates;

    public ChainingAuthenticationManager(ReactiveAuthenticationManager... entryPoints) {
        this(Arrays.asList(entryPoints));
    }

    public ChainingAuthenticationManager(List<ReactiveAuthenticationManager> entryPoints) {
        Assert.notEmpty(entryPoints, "entryPoints cannot be null");
        this.delegates = entryPoints;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        // @formatter:off
        return Flux.fromIterable(this.delegates)
                // see example here: https://stackoverflow.com/questions/73141978/how-to-asynchronosuly-reduce-a-flux-to-mono-with-reactor
                .reduceWith(() -> Mono.just(authentication), (update, delegate) -> update.flatMap(delegate::authenticate))
                .flatMap(authenticationMono -> authenticationMono);


    }
}
