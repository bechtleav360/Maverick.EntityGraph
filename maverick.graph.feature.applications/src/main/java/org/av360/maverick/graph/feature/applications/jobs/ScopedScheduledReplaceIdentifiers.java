package org.av360.maverick.graph.feature.applications.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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
public class ScopedScheduledReplaceIdentifiers extends ScopedJobScheduler {


    public static final String CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY = "replace_identifiers_frequency";

    @Value("${application.features.modules.jobs.scheduled.replaceIdentifiers.defaultFrequency:@midnight}")
    String defaultFrequency;
    public ScopedScheduledReplaceIdentifiers() {
        ConfigurationKeysRegistry.add(CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY, "Frequency as Cron Job Pattern for replacing identifiers");
    }

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY;
    }

    @Override
    String getJobLabel() {
        return "replaceSubjectIdentifiers";
    }

    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }


}