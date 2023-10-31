package org.av360.maverick.graph.feature.applications.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.aspects.Job;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Compute application dataset statistics
 * - number of entities
 * - number of classifiers
 * - last modification date
 */
@Job
@Slf4j(topic = "graph.jobs.duplicates")
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.computeStatistics.enabled","application.features.modules.applications.enabled"})

public class ScopedComputeStatistics extends ScopedJobScheduler {

    @Value("${application.features.modules.jobs.scheduled.computeStatistics:0 10 */2 * * ?}")
    String defaultFrequency;

    public static final String CONFIG_KEY_DETECT_COMPUTE_STATISTICS = "compute_statistics_frequency";


    public ScopedComputeStatistics() {
        ConfigurationKeysRegistry.add(CONFIG_KEY_DETECT_COMPUTE_STATISTICS, "Frequency as Cron Job Pattern for computing applications statistics");
    }

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_DETECT_COMPUTE_STATISTICS;
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