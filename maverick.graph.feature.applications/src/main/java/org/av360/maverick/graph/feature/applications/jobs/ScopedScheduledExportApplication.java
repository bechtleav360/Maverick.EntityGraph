package org.av360.maverick.graph.feature.applications.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Job
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.exportApplication.enabled","application.features.modules.applications.enabled"})
public class ScopedScheduledExportApplication extends ScopedJobScheduler {
    public static final String CONFIG_KEY_EXPORT_FREQUENCY = "export_frequency";


    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultFrequency:@midnight}")
    String defaultFrequency;

    public ScopedScheduledExportApplication() {
        ConfigurationKeysRegistry.add(CONFIG_KEY_EXPORT_FREQUENCY, "Frequency as Cron Job Pattern for exporting statements");
    }

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_EXPORT_FREQUENCY;
    }
    @Override
    String getJobLabel() {
        return "exportApplication";
    }
    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }

}
