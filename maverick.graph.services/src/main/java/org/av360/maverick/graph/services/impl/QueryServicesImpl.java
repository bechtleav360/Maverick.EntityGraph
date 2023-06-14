package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.requests.InvalidQuery;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.behaviours.Searchable;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
@Slf4j(topic = "graph.srvc.query")
public class QueryServicesImpl implements QueryServices {

    private final Map<RepositoryType, Searchable> stores;
    private final QueryParser queryParser;

    public QueryServicesImpl(Set<Searchable> searchables) {
        this.stores = new HashMap<>();
        searchables.forEach(searchable -> stores.put(searchable.getRepositoryType(), searchable));

        queryParser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
    }


    @Override
    public Flux<BindingSet> queryValues(String query, SessionContext ctx) {

        try {
            ParsedQuery parsedQuery = queryParser.parseQuery(query, null);
            if(parsedQuery instanceof  ParsedTupleQuery) {
                return this.queryValuesTrusted(query, ctx);
            } else return Flux.error(new InvalidQuery(query));
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private Flux<BindingSet> queryValuesTrusted(String query, SessionContext ctx) {
        try {
            Validate.notNull(ctx.getEnvironment().getRepositoryType());
            Validate.isTrue(this.stores.containsKey(ctx.getEnvironment().getRepositoryType()), "No repository configured and found for type: %s".formatted(ctx.getEnvironment().getRepositoryType()));

            return this.stores.get(ctx.getEnvironment().getRepositoryType()).query(query, ctx);

        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    @Override
    public Flux<BindingSet> queryValues(SelectQuery query, SessionContext ctx) {
        return this.queryValuesTrusted(query.getQueryString(), ctx);
    }

    @Override
    public Flux<AnnotatedStatement> queryGraph(String queryStr, SessionContext ctx) {
        return ValidateReactive.notNull(ctx.getEnvironment().getRepositoryType())
                .map(q -> queryParser.parseQuery(queryStr, null))
                .flatMapMany(ptq -> this.stores.get(ctx.getEnvironment().getRepositoryType()).construct(queryStr, ctx))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", ctx.getEnvironment(), queryStr);
                });

    }

    public Flux<AnnotatedStatement> queryGraph(ConstructQuery query, SessionContext ctx) {
        return this.stores.get(ctx.getEnvironment().getRepositoryType()).construct(query.getQueryString(), ctx)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", ctx.getEnvironment(), query.getQueryString().replace('\n', ' ').trim());
                });
    }

}
