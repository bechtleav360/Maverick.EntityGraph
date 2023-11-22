package org.av360.maverick.graph.feature.applications.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @see Sch
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Job
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.typeCoercion.enabled","application.features.modules.applications.enabled"})
public class ScopedScheduledTypeCoercion extends ScopedJobScheduler {
    public static final String CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY = "assign_internal_types_frequency";

    @Value("${application.features.modules.jobs.scheduled.typeCoercion.defaultFrequency:@midnight}")
    String defaultFrequency;
    public ScopedScheduledTypeCoercion() {
        ConfigurationKeysRegistry.add(CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY, "Frequency as Cron Job Pattern for assigning internal types.");
    }

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY;
    }

    @Override
    String getJobLabel() {
        return "typeCoercion";
    }

    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }
}