package org.av360.maverick.graph.feature.admin.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service
@Slf4j(topic = "graph.feat.admin.svc")
public class AdminServices {


    private final Map<RepositoryType, Maintainable> stores;

    public AdminServices(Set<Maintainable> maintainables) {
        this.stores = new HashMap<>();
        maintainables.forEach(store -> stores.put(store.getRepositoryType(), store));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> reset(SessionContext ctx) {
        return this.stores.get(ctx.getEnvironment().getRepositoryType())
                .reset(ctx.getEnvironment())
                .doOnSubscribe(sub -> log.debug("Purging repository through admin services"));
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, SessionContext ctx) {
        return this.stores.get(ctx.getEnvironment().getRepositoryType())
                .importStatements(bytes, mimetype, ctx.getEnvironment())
                .doOnSubscribe(sub -> log.debug("Importing statements of type '{}' through admin services", mimetype));

    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> importPackage(FilePart file, SessionContext t1) {

        try {

            File out = File.createTempFile("import", file.filename());
            log.debug("Storing incoming zip file in temp as {} ", out.toString());
            DataBufferUtils.write(file.content(), out.toPath());

            log.debug("Reading zip file ", out.toString());
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(out))) {
                for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {

                }

            }
            return Mono.empty();


        } catch (IOException e) {
            return Mono.error(e);
        }
    }

}
