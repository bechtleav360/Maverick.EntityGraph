package org.av360.maverick.graph.model.identifier;

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;
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
        super.setLocalName(generateChecksum(collect));
    }




    private String generateIdentifier(IRI iri) {
        return this.generateIdentifier(iri.getNamespace(), iri.getLocalName());
    }

    private String generateIdentifier(String first, String ... others) {
        StringBuilder stringBuilder = new StringBuilder(first);
        Arrays.stream(others).forEach(stringBuilder::append);

        return this.generateFingerprint(stringBuilder.toString());
    }






    private String generateChecksum(String val) {
        checksum.reset();
        checksum.update(val.getBytes(), 0, val.length());

        return this.dec2Base(BigInteger.valueOf(checksum.getValue()));

    }

    private  String generateFingerprint(String val) {
        long l = Hashing.murmur3_32_fixed().hashString(val, StandardCharsets.UTF_8).padToLong();
        return this.dec2Base(BigInteger.valueOf(l));
    }

    //private final char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final char[] alphabet = "abcdefghijklmnopqrstuvwyz0123456789_".toCharArray();

    private String dec2Base(BigInteger number) {
        Stack<Integer> stack = new Stack<>();

        do {
            BigInteger[] divisionResultAndReminder = number.divideAndRemainder( BigInteger.valueOf(alphabet.length) );
            stack.push(divisionResultAndReminder[1].intValue());
            number = divisionResultAndReminder[0];
        } while(!number.equals(BigInteger.ZERO));

        StringBuilder result = new StringBuilder();
        while(! stack.empty()) {
            result.append(alphabet[stack.pop()]);
        }
        String ser = result.toString();
        if(ser.length() > LENGTH) return ser.substring(0, LENGTH-1);
        if(ser.length() < LENGTH) return normalize(ser);
        else return ser;


    }

    private String normalize(String str) {
        if (str.length() >= LENGTH) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < LENGTH) {
            sb.append(PADDING_CHAR);
        }

        return sb.toString();

    }


}
