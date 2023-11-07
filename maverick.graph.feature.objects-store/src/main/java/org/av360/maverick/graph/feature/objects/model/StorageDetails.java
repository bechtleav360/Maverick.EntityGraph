package org.av360.maverick.graph.feature.objects.model;

import org.eclipse.rdf4j.model.IRI;

import java.util.Date;

public interface StorageDetails {


    String getStorageLocation();

    String getUriPath();

    String getLanguage();

    String getFilename();

    IRI getIdentifier();

    long getLength();

    Date getLastModified();

    IRI getEntityId();

    IRI getProperty();
}
