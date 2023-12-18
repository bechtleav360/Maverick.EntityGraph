package org.av360.maverick.graph.store.rdf4j.repository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.av360.maverick.graph.store.SchemaStore;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRdfRepository;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j(topic = "graph.repo.schema")
@Component
public class VocabularyStoreImpl extends AbstractRdfRepository implements SchemaStore {

    private final Map<String, String> mappings = new HashMap<>();
    @org.springframework.beans.factory.annotation.Value("${application.storage.vocabularies.path:#{null}}")
    private String path;



    @PostConstruct
    public void loadNamespaces() {
        Namespaces.DEFAULT_RDF4J.forEach(ns -> {
            mappings.put(ns.getPrefix(), ns.getName());
        });
        mappings.putAll(this.loadNamespacesFromFile("ns/namespaces_extended.json"));
        mappings.putAll(this.loadNamespacesFromFile("ns/namespaces_default.json"));

        log.debug("Loaded locally configured prefixes, having {} defined namespaces", mappings.size());

    }

    private Map<String, String> loadNamespacesFromFile(String path) {
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path)) {
            assert resourceAsStream != null;

            String s = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
            return (LinkedHashMap<String, String>) new BasicJsonParser().parseMap(s).get("@context");
        } catch (IOException e) {
            log.error("Failed to load namespaces from file {} due to error.", path, e);

            return Collections.emptyMap();
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
            String name = mappings.get(key);
            return Optional.of(new SimpleNamespace(key, mappings.get(key)));
        } else return Optional.empty();
    }

    @Override
    public Optional<String> getPrefixForNamespace(String name) {
        return mappings.entrySet()
                .stream()
                .filter(entry -> name.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
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
