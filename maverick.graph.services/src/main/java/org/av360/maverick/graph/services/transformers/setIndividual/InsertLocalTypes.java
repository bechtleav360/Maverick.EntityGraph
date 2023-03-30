package org.av360.maverick.graph.services.transformers.setIndividual;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.transformers.Transformer;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j(topic = "graph.srvc.transformer.coercion")
@Component
@ConditionalOnProperty(name = "application.features.transformers.typeCoercion", havingValue = "true")
public class InsertLocalTypes implements Transformer {

    static Set<IRI> characteristicProperties = new HashSet<>();

    static Set<IRI> classifierTypes = new HashSet<>();
    static ValueFactory valueFactory = SimpleValueFactory.getInstance();

    static {
        characteristicProperties.add(DC.IDENTIFIER);
        characteristicProperties.add(DCTERMS.IDENTIFIER);
        characteristicProperties.add(RDFS.LABEL);
        characteristicProperties.add(SKOS.PREF_LABEL);
        characteristicProperties.add(SDO.IDENTIFIER);


        classifierTypes.add(SDO.DEFINED_TERM);
        classifierTypes.add(SKOS.CONCEPT);
        classifierTypes.add(SDO.CATEGORY_CODE);
    }

    @Override
    public Mono<? extends TripleModel> handle(TripleModel model, Map<String, String> parameters, Authentication authentication) {
        return Flux.fromIterable(model.listFragments().entrySet())
                .filter(entry -> isNotIndividual(entry.getKey(), entry.getValue()))
                .filter(entry -> isNotClassifier(entry.getKey(), entry.getValue()))
                .filter(entry -> isNotEmbedded(entry.getKey(), entry.getValue()))
                .doOnNext(fragment -> {
                    log.warn("Fragment with the following statements could not be identified for local type: \n {}", fragment.getValue().stream().toList());
                })
                .then(Mono.just(model));
    }

    public Flux<Statement> getStatements(Model model) {
        return Flux.create(sink -> {
            model.subjects().forEach(subj -> {
                this.handleIndividual(subj, model).ifPresent(sink::next);
                this.handleClassifier(subj, model).ifPresent(sink::next);
                this.handleEmbedded(subj, model).ifPresent(sink::next);
            });
            sink.complete();
        });
    }

    private boolean isNotEmbedded(Resource subject, Model fragment) {
        Optional<Statement> statement = this.handleEmbedded(subject, fragment);
        return statement.map(fragment::add).isEmpty();
    }


    private boolean isNotIndividual(Resource subject, Model fragment) {
        Optional<Statement> statement = this.handleIndividual(subject, fragment);
        return statement.map(fragment::add).isEmpty();
    }

    private Optional<Statement> handleEmbedded(Resource subject, Model fragment) {
        Optional<IRI> cp = characteristicProperties.stream().filter(iri -> fragment.contains(subject, iri, null)).findFirst();
        Optional<IRI> classifier = classifierTypes.stream().filter(iri -> fragment.contains(subject, RDF.TYPE, iri)).findFirst();
        if (cp.isEmpty() && classifier.isEmpty()) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.EMBEDDED);
            log.trace("Fragment for subject '{}' typed as Embedded.", subject);
            return Optional.of(statement);
        } else return Optional.empty();

    }
    private Optional<Statement> handleIndividual(Resource subject, Model fragment) {
        Optional<IRI> cp = characteristicProperties.stream().filter(iri -> fragment.contains(subject, iri, null)).findFirst();
        Optional<IRI> classifier = classifierTypes.stream().filter(iri -> fragment.contains(subject, RDF.TYPE, iri)).findFirst();
        if (cp.isPresent() && classifier.isEmpty()) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.INDIVIDUAL);
            log.trace("Fragment for subject '{}' typed as Individual.", subject);
            return Optional.of(statement);
        } else {
            return Optional.empty();
        }
    }

    private boolean isNotClassifier(Resource subject, Model fragment) {
        Optional<Statement> statement = this.handleClassifier(subject, fragment);
        return statement.map(fragment::add).isEmpty();
    }

    private Optional<Statement>  handleClassifier(Resource subject, Model fragment) {
        Optional<IRI> cp = classifierTypes.stream().filter(iri -> fragment.contains(subject, RDF.TYPE, iri)).findFirst();
        if (cp.isPresent()) {
            Statement statement = valueFactory.createStatement(subject, RDF.TYPE, Local.Entities.CLASSIFIER);
            log.trace("Fragment for subject {} typed as Classifier.", subject);
            return Optional.of(statement);
        } else return Optional.empty();
    }


}
