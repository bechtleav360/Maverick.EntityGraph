package cougar.graph.services.services;

import cougar.graph.model.security.Authorities;
import cougar.graph.services.services.handler.DelegatingTransformer;
import cougar.graph.model.rdf.NamespaceAwareStatement;
import cougar.graph.store.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@Slf4j(topic = "graph.service.query")
public class QueryServices {

    private final EntityStore entityStore;

    public QueryServices(EntityStore graph) {
        this.entityStore = graph;
    }


    public Flux<BindingSet> queryValues(String query, Authentication authentication) {
        return this.entityStore.query(query, authentication)
                .doOnSubscribe(subscription -> {
                    log.trace("Running query in entity store.");
                });
    }

    public Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication) {
        return this.queryValues(query.getQueryString(), authentication);
    }

    public Flux<NamespaceAwareStatement> queryGraph(String query, Authentication authentication) {
        return this.entityStore.construct(query, authentication)
                .doOnSubscribe(subscription -> {
                    log.trace("Running query in entity store.");
                });

    }


    @Autowired
    public void linkTransformers(DelegatingTransformer transformers) {
        transformers.registerQueryService(this);
    }


}
