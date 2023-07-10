package org.av360.maverick.graph.feature.applications.schedulers;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.schedulers.jobs.ComputeStatisticsJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Compute application dataset statistics
 * - number of entities
 * - number of classifiers
 * - last modification date
 */
@Component
@Slf4j(topic = "graph.jobs.duplicates")
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.computeStatistics.enabled","application.features.modules.applications.enabled"})

public class ScopedComputeStatistics extends ScopedJobScheduler {

    @Value("${application.features.modules.jobs.scheduled.computeStatistics:0 10 */2 * * ?}")
    String defaultFrequency;

    public static final String CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY = "computeStatistics";

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY;
    }

    @Override
    String getJobLabel() {
        return ComputeStatisticsJob.NAME;
    }

    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }


}