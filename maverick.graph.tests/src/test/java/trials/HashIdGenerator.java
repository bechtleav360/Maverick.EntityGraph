package trials;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashIdGenerator {

    @Test
    public void createHash() throws NoSuchAlgorithmException {
        String in = "https://schema.org/VideoObject#-w219x83m";

        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] digest = md.digest(in.getBytes());

        BigInteger bigInteger = new BigInteger(1, digest);

        System.out.println(bigInteger.toString(36));
    }
}
