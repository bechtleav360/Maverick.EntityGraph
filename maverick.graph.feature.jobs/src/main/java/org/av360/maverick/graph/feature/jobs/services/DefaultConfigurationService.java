package org.av360.maverick.graph.feature.jobs.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DefaultConfigurationService implements ConfigurationService {

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultLocalPath:}")
    private String defaultLocalPath;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultS3Host:}")
    private String defaultS3Host;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultS3BucketId:}")
    private String defaultS3BucketId;
    @Override
    public Mono<String> getValue(String key, SessionContext context) {
        return switch (key) {
            case "export_local_path" -> Mono.just(defaultLocalPath);
            case "export_s3_host" -> Mono.just(defaultS3Host);
            case "export_s3_bucket" -> Mono.just(defaultS3BucketId);
            default -> Mono.empty();
        };
    }
}
