package org.av360.maverick.graph.services.impl;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.NavigationServices;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Set;

@Service
public class NavigationServicesImpl implements NavigationServices {

    ValueFactory vf = SimpleValueFactory.getInstance();

    @Override
    public Flux<AnnotatedStatement> start() {
        return Flux.create(sink -> {
            ModelBuilder builder = new ModelBuilder();

            Resource root = vf.createIRI(Local.NAMESPACE, "Start");
            Resource swaggerLink = vf.createIRI(Local.NAMESPACE, "SwaggerUI");
            Resource openApiLink = vf.createIRI(Local.NAMESPACE, "OpenApiDocument"); //v3/api-docs

            builder.setNamespace(HYDRA.PREFIX, HYDRA.NAMESPACE)
                    .setNamespace(RDF.PREFIX, RDF.NAMESPACE)
                    .setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
                    .setNamespace(Local.PREFIX, Local.NAMESPACE);

            builder.subject(root)
                    .add(RDF.TYPE, HYDRA.API_DOCUMENTATION)
                    .add(HYDRA.TITLE, "Maverick.EntityGraph")
                    .add(HYDRA.DESCRIPTION, "Opinionated Web API to access linked data fragments in a knowledge graph.")
                    .add(HYDRA.ENTRYPOINT, "/api")
                    .add(vf.createIRI(Local.NAMESPACE, "swagger"), swaggerLink)
                    .add(vf.createIRI(Local.NAMESPACE, "openApi"), openApiLink);

            builder.subject(swaggerLink)
                    .add(RDF.TYPE, HYDRA.LINK)
                    .add(HYDRA.TITLE, "Swagger UI to interact with the API")
                    .add(HYDRA.RETURNS, vf.createLiteral(MediaType.TEXT_HTML_VALUE))
                    .add(HYDRA.ENTRYPOINT, "/webjars/swagger-ui/index.html?urls.primaryName=Entities API");
            builder.subject(openApiLink)
                    .add(RDF.TYPE, HYDRA.LINK)
                    .add(HYDRA.TITLE, "Machine-readable OpenApi Documentation")
                    .add(HYDRA.RETURNS, vf.createLiteral(MediaType.APPLICATION_JSON_VALUE))
                    .add(HYDRA.ENTRYPOINT, "/v3/api-docs");

            Set<Namespace> namespaces = builder.build().getNamespaces();
            builder.build().stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).forEach(sink::next);
            sink.complete();


        });
    }
}
