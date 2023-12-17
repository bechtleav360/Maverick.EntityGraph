package org.av360.maverick.graph.feature.objects.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.errors.InvalidConfiguration;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.ContentLocationResolverService;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Service
public class DefaultContentLocationResolver implements ContentLocationResolverService {

    @org.springframework.beans.factory.annotation.Value("${application.features.modules.objects.configuration.path:#{null}}")
    private String path;

    @Override
    public String getDefaultBaseDirectory() {
        return this.path;
    }

    @Override
    public Mono<ContentLocation> resolveContentLocation(IRI entityID, IRI contentId, String filename, @Nullable String language, SessionContext ctx) {
        Assert.isTrue(StringUtils.hasLength(path), "Path for local file storage is not configured in application properties.");


        if (ctx.getAuthentication().isEmpty() || !Authorities.satisfies(Authorities.READER, ctx.getAuthentication().get().getAuthorities())) {
            String msg = String.format("Required authority '%s' for resolving uri for filename '%s' and entity '%s' not met in authentication", Authorities.READER, filename, entityID.getLocalName());
            return Mono.error(new InsufficientPrivilegeException(msg));
        }



        return resolvePath(getDefaultBaseDirectory(), entityID.getLocalName(), filename, null, language)
                .map(path -> new ContentLocation(path.toUri(), "/content/%s".formatted(contentId.getLocalName()), filename, language));

    }

    public Mono<Path> resolvePath(String configuredPath, String entityKey, String filename, @Nullable String scope, @Nullable String language) {
        try {
            Path path = Path.of(configuredPath);

            if(StringUtils.hasLength(scope)) {
                path = path.resolve(scope);
            } else {
                path = path.resolve("default");
            }

            path = path.resolve(entityKey);
            if(path.toFile().mkdirs() || path.toFile().exists()) {
                Path updated_path = path.resolve(validateFilename(filename, language));
                path = updated_path;
            } else {
                return Mono.error(new IOException("Failed to created directories for path: "+path));
            }

            return Mono.just(path);
        } catch (SecurityException | InvalidPathException invalidPathException) {
            return Mono.error(new InvalidConfiguration("Invalid path for default content storage in configuration: "+invalidPathException.getMessage()));
        }
    }

    private String validateFilename(String filename, @Nullable String language) {
        String[] split = filename.split("\\.");
        if(StringUtils.hasLength(language)) {
            if(split.length > 1) return  "%s_%s.%s".formatted(split[0], language, split[1]);
            else return "%s_%s".formatted(filename, language);
        } else {
            return filename;
        }
    }

}
