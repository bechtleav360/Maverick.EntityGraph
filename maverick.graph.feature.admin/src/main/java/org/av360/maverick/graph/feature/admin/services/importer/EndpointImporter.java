/*
 * Copyright (c) 2023.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.feature.admin.services.importer;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.store.FragmentsStore;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

/**
 * Imports the complete content of a source repository (a sparql endpoint) into an application.
 */
@Slf4j(topic = "graph.feat.admin.svc.import")
public class EndpointImporter {
    private final String endpoint;
    private final Map<String, String> headers;
    private final Map<RepositoryType, FragmentsStore> stores;
    private final IdentifierServices identifierServices;

    public EndpointImporter(String endpoint, Map<String, String> headers, Map<RepositoryType, FragmentsStore> stores, IdentifierServices identifierServices) {
        this.endpoint = endpoint;
        this.headers = headers;
        this.stores = stores;
        this.identifierServices = identifierServices;
    }

    public Mono<Void> runImport(SessionContext ctx) {

        SPARQLRepository repository = new SPARQLRepository(endpoint);
        repository.setAdditionalHttpHeaders(headers);

        return this.importFromEndpoint(repository, 1000, 0, ctx);
    }

    private Mono<Void> importFromEndpoint(SPARQLRepository repository, int limit, int offset, SessionContext ctx) {
        String query = """
                SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT %d OFFSET %d
                """
                .formatted(limit, offset);

        try (RepositoryConnection connection = repository.getConnection()) {
            TupleQuery tupleQuery = connection.prepareTupleQuery(query);
            ValueFactory vf = SimpleValueFactory.getInstance();
            Model resultingModel = null;
            try (TupleQueryResult resultingBindings = tupleQuery.evaluate()) {
                resultingModel = resultingBindings.stream().map(bindings -> {
                    try {
                        Resource subject = (Resource) bindings.getValue("s");
                        IRI predicate = (IRI) bindings.getValue("p");
                        Value object = bindings.getValue("o");

                        subject = this.convertResource(subject, ctx, repository);
                        object = this.convertValue(object, ctx, repository);
                        return vf.createStatement(subject, predicate, object);
                    } catch (Exception e) {
                        log.warn("Failed importing binding: {}", bindings);
                        return null;
                    }

                }).filter(Objects::nonNull).collect(new ModelCollector());
            } catch (QueryEvaluationException qe) {
                log.error("Failed to evaluate query: %s with error: %s".formatted(query, qe.getMessage()));
            } catch (Exception e) {
                return Mono.error(e);
            }

            if (Objects.nonNull(resultingModel)) {
                if (resultingModel.isEmpty()) {
                    log.debug("Finished importing around {} statements from endpoint {}", offset, endpoint);
                }

                return this.stores.get(ctx.getEnvironment().getRepositoryType())
                        .asMaintainable()
                        .importStatements(resultingModel, ctx.getEnvironment())
                        .doOnSuccess(suc -> {
                            Mono.just(offset + limit)
                                    .delayElement(Duration.of(100, ChronoUnit.MILLIS))
                                    .flatMap(nlimit -> this.importFromEndpoint(repository, limit, nlimit, ctx))
                                    .doOnSubscribe(subscription -> {
                                        log.debug("Importing next {} statements from endpoint {} with offset {}", limit, endpoint, offset + limit);
                                    })
                                    .subscribeOn(Schedulers.newSingle("import"))
                                    .subscribe();

                        })
                        .doOnError(throwable -> {
                            log.error("Failed to import statements due to error {}", throwable.getMessage());
                        })
                        .doOnSubscribe(sub -> {
                            log.debug("Importing statements from endpoint {} into repository {} through admin services", endpoint, ctx.getEnvironment());
                        })
                        .then();
            } else {
                return Mono.empty();
            }

        }
    }


    /**
     * Converts IRI objects into local representations (with the target environment's scope in the identifier).
     * <p>
     * BUT:
     * urn:meg:scope_source_A.1234 pred urn:meg:scope_source_A.4232
     * pred urn:meg:scope_source_B.3412
     * <p>
     * will be transformed to
     * <p>
     * urn:meg:scope_target_X.1234 pred urn:meg:scope_target_X.4232
     * pred urn:meg:scope_target_X.3412
     * <p>
     * which is wrong. "scope_source_B" should not be touched (in fact, it should be converted to a URI pointing to the remote instance)
     *
     * @param object
     * @param ctx
     * @param repository
     * @return
     */
    private Value convertValue(Value object, SessionContext ctx, SPARQLRepository repository) {
        if (object instanceof Resource resource) {
            return this.convertResource(resource, ctx, repository);
        } else return object;
    }

    /**
     * will convert the source identifier to the correct identifier
     * <urn:pwid:meg:e:podigee.b6k7nvdx> -> </urn:pwid:meg:e:podcasts.b6k7nvdx>
     *
     * @param subject
     * @param ctx
     * @return
     */
    private Resource convertResource(Resource subject, SessionContext ctx, SPARQLRepository repository) {
        if (subject instanceof IRI iri) {
            return this.identifierServices.validateIRI(iri, ctx.getEnvironment(), repository.toString(), repository.getAdditionalHttpHeaders());
        } else return subject;
    }

}
