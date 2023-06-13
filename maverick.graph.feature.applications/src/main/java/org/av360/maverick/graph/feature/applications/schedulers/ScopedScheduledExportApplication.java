package org.av360.maverick.graph.feature.applications.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.exportApplication.enabled", havingValue = "true")
public class ScopedScheduledExportApplication extends ScopedJobScheduler {
    public static final String CONFIG_KEY_EXPORT_FREQUENCY = "export_frequency";
    public static final String CONFIG_KEY_EXPORT_LOCAL_PATH = "export_local_path";
    public static final String CONFIG_KEY_EXPORT_S3_HOST = "export_s3_host";
    public static final String CONFIG_KEY_EXPORT_S3_BUCKET = "export_s3_bucket";

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultFrequency:0 */5 * * * ?}")
    String defaultFrequency;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ScopedScheduledExportApplication() {

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