package org.av360.maverick.graph.feature.objects.model;

import org.eclipse.rdf4j.model.IRI;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

public class LocalStorageDetails implements StorageDetails {


    private  IRI fileId;
    private  String apiPath;
    private  Path storagePath;
    private  IRI entityId;
    private  IRI property;
    private  String filename;
    private  String language;
    private long length;
    private Date lastModified;

    public LocalStorageDetails() {
    }


    public Path getStoragePath() {
        return storagePath;
    }

    @Override
    public String getStorageLocation() {
        return "file";
    }

    @Override
    public String getUriPath() {
        return this.apiPath;
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
    public IRI getIdentifier() {
        return fileId;
    }

    public LocalStorageDetails setLength(long length) {
        this.length = length;
        return this;
    }

    public LocalStorageDetails setDetails(File file) {
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

    public LocalStorageDetails setFileId(IRI fileId) {
        this.fileId = fileId;
        return this;
    }

    public LocalStorageDetails setApiPath(String apiPath) {
        this.apiPath = apiPath;
        return this;
    }

    public LocalStorageDetails setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
        return this;
    }

    public LocalStorageDetails setEntityId(IRI entityId) {
        this.entityId = entityId;
        return this;
    }

    public LocalStorageDetails setProperty(IRI property) {
        this.property = property;
        return this;
    }

    public LocalStorageDetails setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public LocalStorageDetails setLanguage(String language) {
        this.language = language;
        return this;
    }
}
