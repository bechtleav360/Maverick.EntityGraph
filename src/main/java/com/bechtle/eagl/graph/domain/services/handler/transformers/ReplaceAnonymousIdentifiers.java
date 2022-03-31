package com.bechtle.eagl.graph.domain.services.handler.transformers;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Replaces blank nodes with valid IRIs
 * <p>
 * See also: https://www.w3.org/2011/rdf-wg/wiki/Skolemisation
 * <p>
 * <p>
 * Systems wishing to skolemise bNodes, and expose those skolem constants to external systems (e.g. in query results) SHOULD mint a "fresh" (globally unique) URI for each bNode.
 * All systems performing skolemisation SHOULD do so in a way that they can recognise the constants once skolemised, and map back to the source bNodes where possible.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceAnonymousIdentifiers", havingValue = "true")
public class ReplaceAnonymousIdentifiers implements Transformer {


    @Override
    public Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel triples, Map<String, String> parameters) {
        log.trace("(Transformer) Skolemizing identifiers");

        for (Resource obj : new ArrayList<>(triples.getModel().subjects())) {
            /* Handle Ids */
            if (obj.isBNode()) {
                // generate a new qualified identifier if it is an anonymous node
                this.skolemize(obj, triples);
            }
        }
        return Mono.just(triples);
    }

    public void skolemize(Resource subj, AbstractModel triples) {

        IRI identifier = new GeneratedIdentifier(Local.Entities.NS);

        List<NamespaceAwareStatement> copy = Collections.unmodifiableList(triples.streamNamespaceAwareStatements().toList());

        triples.reset();

        for (Statement st : copy) {
            if (st.getSubject().equals(subj)) {
                triples.getBuilder().add(identifier, st.getPredicate(), st.getObject());
            } else if (st.getObject().equals(subj)) {
                triples.getBuilder().add(st.getSubject(), st.getPredicate(), identifier);
            } else {
                triples.getBuilder().add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }
    }


}
