package org.av360.maverick.graph.feature.applications.decorators;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.security.SubscriptionToken;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.ContentLocationResolverService;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.nio.file.Path;

@Slf4j(topic = "graph.feat.app")
public class DelegatingContentResolver implements ContentLocationResolverService {
    private final ContentLocationResolverService delegate;
    private final ApplicationsService applicationsService;
    public static final String CONFIG_KEY_CONTENT_PATH = "content_path";


    public DelegatingContentResolver(ContentLocationResolverService delegate, ApplicationsService applicationsService) {
        this.delegate = delegate;
        this.applicationsService = applicationsService;
        ConfigurationKeysRegistry.add(CONFIG_KEY_CONTENT_PATH, "Directory where binary objects and files for this application should be stored.");
    }



    @Override
    public Mono<ContentLocation> resolveContentLocation(IRI entityID, IRI contentId, String filename, @Nullable String language, SessionContext ctx) {
        return Mono.just(ctx.getEnvironment().getScope())
                .flatMap(scope -> applicationsService.getApplicationByLabel(scope.label(), ctx))
                .flatMap(application -> {


                    try {
                        Authentication authentication = ctx.getAuthenticationOrThrow();


                        if (application.flags().isPublic() && ! Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
                            String msg = String.format("Required authority '%s' for resolving uri for filename '%s' and entity '%s' not met in authentication with authorities '%s'", Authorities.READER, filename, entityID, authentication.getAuthorities());
                            throw new InsufficientPrivilegeException(msg);
                        } else if (! application.flags().isPublic() && ctx.getAuthentication().isPresent() && authentication instanceof SubscriptionToken subscriptionToken) {
                            // TODO: check if authentication is of type
                            if(! subscriptionToken.getApplication().key().equalsIgnoreCase(application.key())) {
                                String msg = String.format("Required authority '%s' for resolving uri for filename '%s' and entity '%s' not met in authentication with authorities '%s'", Authorities.READER, filename, entityID, authentication.getAuthorities());
                                throw new InsufficientPrivilegeException(msg);
                            }
                        }
                    } catch (Exception e) {
                        return Mono.error(e);
                    }



                    String contentDir = delegate.getDefaultBaseDirectory();
                    if(application.configuration().containsKey(CONFIG_KEY_CONTENT_PATH)) {
                        contentDir = application.configuration().get(CONFIG_KEY_CONTENT_PATH).toString();
                    }

                    return this.resolvePath(contentDir, entityID.getLocalName(), filename, language)
                            .map(path -> new ContentLocation(path.toUri(), "/content/s/%s/%s".formatted(application.label(), this.normalizeLocalname(contentId)), filename, language));


                })
                .switchIfEmpty(delegate.resolveContentLocation(entityID, contentId, filename, language, ctx));

    }

    private String normalizeLocalname(IRI entityId) {
        String result =  entityId.getLocalName().substring(entityId.getLocalName().indexOf('.')+1);
        return result;
    }

    @Override
    public Mono<Path> resolvePath(String baseDirectory, String entityKey, String filename,  @Nullable String language) {
        return delegate.resolvePath(baseDirectory, entityKey, filename, language);
    }

    @Override
    public String getDefaultBaseDirectory() {
        return delegate.getDefaultBaseDirectory();
    }
}
