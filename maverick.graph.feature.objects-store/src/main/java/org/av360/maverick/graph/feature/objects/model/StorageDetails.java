package org.av360.maverick.graph.feature.objects.model;

import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.eclipse.rdf4j.model.IRI;

import java.util.Date;

public interface StorageDetails {


    String getStorageLocation();

    String getURI();

    String getLanguage();

    String getFilename();

    LocalIdentifier getIdentifier();

    long getLength();

    Date getLastModified();

    IRI getEntityId();

    IRI getProperty();
}
