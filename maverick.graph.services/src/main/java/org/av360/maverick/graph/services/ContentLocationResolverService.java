package org.av360.maverick.graph.services;

import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;


/**
 * Part of the object storage feature. Required to resolve the storage location for a given filename belonging to an entity.
 *
 * This class is also responsible for asserting access rights (checking if given authentication has sufficient privilege)
 */
public interface ContentLocationResolverService {
    Mono<Path> resolvePath(String baseDirectory, String entityKey,  String filename, @Nullable String language);

    String getDefaultBaseDirectory();
    record ContentLocation(URI storageURI, String apiPath) {}

    Mono<ContentLocation> resolveContentLocation(IRI entityId, IRI contentId, String filename, @Nullable String language, Authentication authentication);
}
