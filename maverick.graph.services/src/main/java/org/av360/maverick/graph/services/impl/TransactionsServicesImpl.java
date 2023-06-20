package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Service
@Slf4j(topic = "graph.srvc.trx")
public class TransactionsServicesImpl implements TransactionsService {
    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Flux<RdfTransaction> list(Integer limit, Integer offset, SessionContext authentication) {
        return Flux.error(NotImplementedException::new);
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<RdfTransaction> find(String identifier, SessionContext authentication) {
        return Mono.error(NotImplementedException::new);
    }
}
