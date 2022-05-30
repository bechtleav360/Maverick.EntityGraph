package com.bechtle.cougar.graph.repository;

import com.bechtle.cougar.graph.repository.behaviours.*;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.cougar.graph.domain.model.wrapper.Entity;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import org.eclipse.rdf4j.model.*;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


public interface EntityStore extends Searchable, Resettable, ModelUpdates, Selectable, Statements {

    Mono<Entity> getEntity(IRI id, Authentication authentication);











}
