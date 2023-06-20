package org.av360.maverick.graph.feature.admin.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
@Slf4j(topic = "graph.feat.admin.svc")
public class AdminServices {


    private final Map<RepositoryType, Maintainable> stores;

    public AdminServices(Set<Maintainable> maintainables) {
        this.stores = new HashMap<>();
        maintainables.forEach(store -> stores.put(store.getRepositoryType(), store));
    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> reset(SessionContext ctx) {
        return this.stores.get(ctx.getEnvironment().getRepositoryType())
                .reset(ctx.getEnvironment())
                .doOnSubscribe(sub -> log.debug("Purging repository through admin services"));
    }
    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, SessionContext ctx) {
        return this.stores.get(ctx.getEnvironment().getRepositoryType())
                .importStatements(bytes, mimetype, ctx.getEnvironment())
                .doOnSubscribe(sub -> log.debug("Importing statements of type '{}' through admin services", mimetype));



    }

}
