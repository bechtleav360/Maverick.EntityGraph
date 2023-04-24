package org.av360.maverick.graph.services.transformers.replaceIdentifiers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import reactor.core.publisher.Mono;

import java.util.Set;

public class AbstractIdentifierReplace {

    protected final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

    public record IdentifierMapping(Resource oldIdentifier, IRI newIdentifier) {}

    protected Mono<Set<IdentifierMapping>> replaceIdentifiers(Set<IdentifierMapping> mappings, Model model) {
        Model res = new LinkedHashModel(model);

        mappings.forEach(mapping -> {
            model.filter(mapping.oldIdentifier, null, null)
                    .forEach(statement -> {
                            res.remove(statement);
                            res.add(mapping.newIdentifier(), statement.getPredicate(), statement.getObject());
                    });
        });

        model.clear();
        model.addAll(res);

        mappings.forEach(mapping -> {
            model.filter(null, null, mapping.oldIdentifier)
                    .forEach(statement -> {
                        res.remove(statement);
                        res.add(statement.getSubject(), statement.getPredicate(), mapping.newIdentifier());
                    });
        });

        model.clear();
        model.addAll(res);

        return Mono.just(mappings);
    }



    protected Mono<Set<IdentifierMapping>> preserveExternalIdentifiers(Set<IdentifierMapping> mappings, Model model) {
        // preserve old ids
        mappings.forEach(mapping -> {
            model.add(mapping.newIdentifier(), OWL.SAMEAS, mapping.oldIdentifier());
        });
        return Mono.just(mappings);
    }
}
