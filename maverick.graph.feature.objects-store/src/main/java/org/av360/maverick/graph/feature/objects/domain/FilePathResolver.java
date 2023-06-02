package org.av360.maverick.graph.feature.objects.domain;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FilePathResolver {
    @org.springframework.beans.factory.annotation.Value("${application.features.modules.objects.configuration.path:#{null}}")
    private String path;
    public Mono<Path> resolvePath(String entityKey, String filename, @Nullable String language) {
        Assert.isTrue(StringUtils.hasLength(path), "Path for local file storage is not configured in application properties.");

        String[] split = filename.split("\\.");
        if(StringUtils.hasLength(language)) {
            if(split.length > 1) filename = "%s_%s.%s".formatted(filename, language, split[1]);
            else filename = "%s_%s".formatted(filename, language);
        }

        Path path = Paths.get(this.path, entityKey);
        path.toFile().mkdirs();
        path = path.resolve(filename);
        return Mono.just(path);
    }

}
