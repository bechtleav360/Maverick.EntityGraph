package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.requests.InvalidQuery;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.FragmentsStore;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.*;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Service
@Slf4j(topic = "graph.srvc.query")
public class QueryServicesImpl implements QueryServices {

    private final Map<RepositoryType, FragmentsStore> stores;
    private final QueryParser queryParser;

    public QueryServicesImpl(Set<FragmentsStore> storesSet) {
        this.stores = new HashMap<>();

        storesSet.forEach(store -> {
            if(store.isSearchable()) {
                stores.put(store.getRepositoryType(), store);
            }
        });

        queryParser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Flux<BindingSet> queryValues(String query, RepositoryType repositoryType, SessionContext ctx) {
        try {
            ctx.getEnvironment().withRepositoryType(repositoryType);
            ParsedQuery parsedQuery = queryParser.parseQuery(query, null);
            if(parsedQuery instanceof  ParsedTupleQuery) {
                return this.queryValuesTrusted(query, repositoryType, ctx);
            } else throw new InvalidQuery(query);
        } catch (Exception | InvalidQuery e) {
            return Flux.error(e);
        }
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<BindingSet> queryValues(SelectQuery query, RepositoryType repositoryType, SessionContext ctx) {
        return this.queryValuesTrusted(query.getQueryString(), repositoryType, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> queryGraph(String queryStr, RepositoryType repositoryType, SessionContext ctx) {
        try {
            ParsedQuery parsedQuery = queryParser.parseQuery(queryStr, null);
            if(parsedQuery instanceof ParsedGraphQuery) {
                return this.queryGraphTrusted(queryStr, repositoryType, ctx);
            } else throw new InvalidQuery(queryStr);
        } catch (Exception | InvalidQuery e) {
            return Flux.error(e);
        }
    }
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<AnnotatedStatement> queryGraph(ConstructQuery query,  RepositoryType repositoryType, SessionContext ctx) {
        return this.queryGraphTrusted(query.getQueryString(), repositoryType, ctx);

    }


    @Override
    public Flux<AnnotatedStatement> queryGraphTrusted(String query, RepositoryType target, SessionContext ctx) {
        try {
            if(Objects.isNull(ctx.getEnvironment().getRepositoryType())) ctx.updateEnvironment(env -> env.setRepositoryType(target));

            // check if we should set back to old repository type if needed
            return this.stores.get(target).asSearchable().construct(query, ctx.getEnvironment())
                    .doOnSubscribe(subscription -> {
                        if (log.isTraceEnabled())
                            log.trace("Running construct query in {}: {}", ctx.getEnvironment(), query.replace('\n', ' ').trim());
                    });
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    @Override
    public Flux<BindingSet> queryValuesTrusted(String query, RepositoryType repositoryType, SessionContext ctx) {
        try {
            if(Objects.isNull(ctx.getEnvironment().getRepositoryType())) ctx.updateEnvironment(env -> env.setRepositoryType(repositoryType));

            return this.stores.get(repositoryType).asSearchable().query(query, ctx.getEnvironment())
                    .doOnSubscribe(subscription -> {
                        if (log.isTraceEnabled())
                            log.trace("Running select query in [{}]: {}", ctx.getEnvironment(), query.replace('\n', ' ').trim());
                    });
        } catch (Exception e) {
            return Flux.error(e);
        }
    }



}
