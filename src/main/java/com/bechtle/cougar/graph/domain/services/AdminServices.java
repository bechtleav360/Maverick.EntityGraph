package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j(topic = "cougar.graph.service.admin")
public class AdminServices {

    private final EntityStore graph;

    public AdminServices(EntityStore graph) {
        this.graph = graph;
    }


    public Mono<Void> reset(Authentication authentication, RepositoryConfiguration.RepositoryType repositoryType) {
        return this.graph.reset(authentication, repositoryType).then();
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, Authentication authentication) {
        log.trace("Importing statements of type '{}' through admin services", mimetype);
        return this.graph.importStatements(bytes, mimetype, authentication).then();
    }
}
