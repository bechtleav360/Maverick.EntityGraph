package org.av360.maverick.graph.model.identifier;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

/**
 * The generated entity identifier needs to resolve, it should be in the form
 * <p>
 * http://example.org/api/entities/{id}
 * http://example.org/api/transactions/{id}
 * <p>
 * FIXME: should require the current id (either bnode or externally set) to create reproducible ids
 * FIXME: should also keep track of the original id (we should store this in the provenance)
 */
@Slf4j
public class ChecksumIdentifier extends LocalIRI implements LocalIdentifier  {
    private static final char[][] range = {{'a', 'z'}, {'0', '9'}};

    private static final Checksum checksum = new CRC32C();


    /**
     * Generates a new and reproducible identifier from the old resource identifier (its namespace) and a characteristic property
     * @param namespace the new local namespace
     * @param parts characteristic property (rdfs:label, dc:identifier, ...)
     */
    public ChecksumIdentifier(String namespace, Serializable ... parts) {

        super(namespace);

        String collect = Arrays.stream(parts).map(Object::toString).collect(Collectors.joining());
        Assert.hasLength(collect, "No content to generate reproducible identifier.");
        super.setLocalName(ChecksumGenerator.generateChecksum(collect, LENGTH, PADDING_CHAR));
    }










}
