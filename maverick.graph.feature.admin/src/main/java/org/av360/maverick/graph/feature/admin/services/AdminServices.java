package org.av360.maverick.graph.feature.admin.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.requests.SchedulingException;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Service
@Slf4j(topic = "graph.feat.admin.svc")
public class AdminServices {


    private final Map<RepositoryType, Maintainable> stores;

    private boolean maintenanceActive = false;

    public AdminServices(Set<Maintainable> maintainables) {
        this.stores = new HashMap<>();
        maintainables.forEach(store -> stores.put(store.getRepositoryType(), store));
    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> reset(SessionContext ctx) {
        // if(maintenanceActive) return Mono.error(new SchedulingException("Maintenance job still running."));
        // doesn't work for testing, ignore for now

        this.stores.get(ctx.getEnvironment().getRepositoryType())
                .reset(ctx.getEnvironment())
                .doOnSubscribe(sub -> {
                    this.maintenanceActive = true;
                    log.debug("Purging repository {} through admin services.", ctx.getEnvironment());
                })
                .doOnSuccess(suc -> {
                    this.maintenanceActive = false;
                    log.debug("Purging repository {} completed.", ctx.getEnvironment());
                })
                .subscribeOn(Schedulers.newSingle("import"))
                .subscribe();
        return Mono.empty();
    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, SessionContext ctx) {
        if(maintenanceActive) return Mono.error(new SchedulingException("Maintenance job still running."));

        this.stores.get(ctx.getEnvironment().getRepositoryType())
                .importStatements(bytes, mimetype, ctx.getEnvironment())
                .doOnSubscribe(sub -> {
                    this.maintenanceActive = true;
                    log.debug("Importing statements of type '{}' into repository {} through admin services", mimetype, ctx.getEnvironment());
                })
                .doOnSuccess(suc -> {
                    this.maintenanceActive = false;
                    log.debug("Importing statements completed into repository {} through admin services", ctx.getEnvironment());
                })
                .subscribeOn(Schedulers.newSingle("import"))
                .subscribe();
        return Mono.empty();

    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> importFromEndpoint(String endpoint, Map<String, String> headers, int limit, int offset, SessionContext ctx) {

        String query = """
                SELECT ?s ?p ?o  ?type WHERE { ?s ?p ?o } LIMIT %d OFFSET %d
                """
                .formatted(limit, offset);

        SPARQLRepository sparqlRepository = new SPARQLRepository(endpoint);
        sparqlRepository.setAdditionalHttpHeaders(headers);
        try (RepositoryConnection connection = sparqlRepository.getConnection()) {
            TupleQuery tupleQuery = connection.prepareTupleQuery(query);
            ValueFactory vf = SimpleValueFactory.getInstance();
            Model resultingModel = null;
            try (TupleQueryResult resultingBindings = tupleQuery.evaluate()) {
                resultingModel = resultingBindings.stream().map(bindings -> {
                    Resource subject = (Resource) bindings.getValue("s");
                    IRI predicate = (IRI) bindings.getValue("p");
                    Value object = bindings.getValue("o");
                    return vf.createStatement(subject, predicate, object);
                }).collect(new ModelCollector());
            } catch (Exception e) {
                return Mono.error(e);
            }

            if (Objects.nonNull(resultingModel)) {
                final int previousResultCount = resultingModel.size();
                return this.stores.get(ctx.getEnvironment().getRepositoryType())
                        .importModel(resultingModel, ctx.getEnvironment())
                        .doOnSuccess(suc -> {
                            if (previousResultCount == limit) {
                                Mono.just(offset+limit)
                                        .delayElement(Duration.of(2, ChronoUnit.SECONDS))
                                        .flatMap(nlimit -> this.importFromEndpoint(endpoint, headers, limit, nlimit, ctx))
                                        .doOnSubscribe(subscription -> {
                                            log.debug("Importing next {} items from endpoint: {}", limit, endpoint);
                                        })
                                        .subscribeOn(Schedulers.newSingle("import"))
                                        .subscribe();

                            } else {
                                this.maintenanceActive = false;
                                log.debug("Finished importing from endpoint: {}", endpoint);
                            }
                        })
                        .doOnSubscribe(sub -> {
                            this.maintenanceActive = true;
                            log.debug("Importing statements from endpoint {} into repository {} through admin services", endpoint, ctx.getEnvironment());
                        })
                        .then();
            } else {
                this.maintenanceActive = false;
                return Mono.empty();
            }
        }

    }
}
