package org.av360.maverick.graph.feature.applications.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.av360.maverick.graph.feature.applications.jobs.ScopedScheduledReplaceIdentifiers.CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY;

/**
 * If we have any global identifiers (externally set) in the repo, we have to replace them with our internal identifiers.
 * Otherwise we cannot address the entities through our API.
 * <p>
 * Periodically runs the following sparql queries, grabs the entity definition for it and regenerates the identifiers
 * <p>
 * SELECT ?a WHERE { ?a a ?c . }
 * FILTER NOT EXISTS {
 * FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
 * }
 * LIMIT 100
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Job
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.replaceIdentifiers.enabled","application.features.modules.applications.enabled"})
public class ScopedScheduledReplaceLinkedIdentifiers extends ScopedJobScheduler {
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    @Value("${application.features.modules.jobs.scheduled.replaceIdentifiers.defaultFrequency:@midnight}")
    String defaultFrequency;
    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY;
    }

    @Override
    String getJobLabel() {
        return "replaceLinkedIdentifiers";
    }
    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }

}