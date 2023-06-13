package org.av360.maverick.graph.feature.applications.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @see Sch
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.typeCoercion.enabled", havingValue = "true")
public class ScopedScheduledTypeCoercion extends ScopedJobScheduler {
    public static final String CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY = "assign_internal_types_frequency";

    @Value("${application.features.modules.jobs.scheduled.typeCoercion.defaultFrequency:0 */5 * * * ?}")
    String defaultFrequency;

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