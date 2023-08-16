package org.av360.maverick.graph.store.rdf4j.repository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.SchemaStore;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.slf4j.Logger;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j(topic = "graph.repo.schema")
@Component
public class VocabularyStoreImpl extends AbstractStore implements SchemaStore {

    private final Map<String, String> mappings = new HashMap<>();
    @org.springframework.beans.factory.annotation.Value("${application.storage.vocabularies.path:#{null}}")
    private String path;


    public VocabularyStoreImpl() {
        super(RepositoryType.SCHEMA);
    }


    @PostConstruct
    public void loadNamespaces() {
        Namespaces.DEFAULT_RDF4J.forEach(ns -> {
            mappings.put(ns.getPrefix(), ns.getPrefix());
        });


        try {
            Path folder = Paths.get(Objects.requireNonNull(this.getClass().getClassLoader().getResource("ns")).toURI());
            Files.list(folder).forEach(filepath -> {
                try {
                    String s = Files.readString(filepath, StandardCharsets.UTF_8);
                    LinkedHashMap<String, String> map = (LinkedHashMap<String, String>) new BasicJsonParser().parseMap(s).get("@context");
                    mappings.putAll(map);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });

            log.debug("Loaded locally configured prefixes, having {} defined namespaces", mappings.size());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }

    @Override
    public Flux<Namespace> listNamespaces() {
        return Flux.fromIterable(mappings.entrySet()).map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()));
    }

    @Override
    public Optional<Namespace> getNamespaceForPrefix(String key) {
        if(mappings.containsKey(key)) {
            return Optional.of(new SimpleNamespace(key, mappings.get(key)));
        } else return Optional.empty();
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getDirectory() {
        return this.path;
    }


}
