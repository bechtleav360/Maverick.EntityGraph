package org.av360.maverick.graph.api.controller;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.SessionContextBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public class AbstractController {



    Set<SessionContextBuilderService> builders;


    protected Mono<SessionContext>

     acquireContext() {
        return Flux.fromIterable(this.builders)
                // see example here: https://stackoverflow.com/questions/73141978/how-to-asynchronosuly-reduce-a-flux-to-mono-with-reactor
                .reduceWith(() -> Mono.just(new SessionContext()), (update, builderService) -> update.flatMap(builderService::build))
                .flatMap(sessionContextMono -> sessionContextMono);
    }


    @Autowired
    public void setBuilders(Set<SessionContextBuilderService> builders) {
        this.builders = builders;
    }

}
