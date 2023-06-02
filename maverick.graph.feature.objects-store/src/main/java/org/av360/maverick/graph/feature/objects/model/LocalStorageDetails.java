package org.av360.maverick.graph.feature.objects.model;

import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.eclipse.rdf4j.model.IRI;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

public class LocalStorageDetails implements StorageDetails{


    private final LocalIdentifier fileId;
    private final Path path;
    private final IRI entityId;
    private final IRI property;
    private final String filename;
    private final String language;
    private long length;
    private Date lastModified;

    public LocalStorageDetails(LocalIdentifier fileId, Path path, IRI entityId, IRI property, String filename, String language) {
        this.fileId = fileId;

        this.path = path;
        this.entityId = entityId;
        this.property = property;
        this.filename = filename;
        this.language = language;
    }




    public Path getPath() {
        return path;
    }

    @Override
    public String getStorageLocation() {
        return "file";
    }

    @Override
    public String getURI() {
        return "/content/%s".formatted(this.fileId.getLocalName());
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String getFilename() {
        return filename;
    }


    @Override
    public LocalIdentifier getIdentifier() {
        return fileId;
    }

    public StorageDetails setLength(long length) {
        this.length = length;
        return this;
    }

    public StorageDetails setDetails(File file) {
        this.length = file.length();
        this.lastModified = Date.from(Instant.ofEpochMilli(file.lastModified()));

        return this;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public IRI getEntityId() {
        return entityId;
    }

    @Override
    public IRI getProperty() {
        return property;
    }
}
