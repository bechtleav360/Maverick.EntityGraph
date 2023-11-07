package trials;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;
import java.util.zip.Adler32;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class HashIdGenerator {

    @Test
    public void createMd5Hash_b32() throws NoSuchAlgorithmException {
        String in = "sdo:VideoObject#-w219x83m";

        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] digest = md.digest(in.getBytes());

        BigInteger bigInteger = new BigInteger(1, digest);

        String res = bigInteger.toString(36);
        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Hash: "+ res + "/" + res.length());
    }


    @Test
    public void createMd5Hash_1() throws NoSuchAlgorithmException {
        String in = "sdo:VideoObject#-w219x83m";

        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] digest = md.digest(in.getBytes());

        BigInteger bigInteger = new BigInteger(1, digest);

        String res = dec2Base(bigInteger);


        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Hash: "+ res + "/" + res.length());
    }

    @Test
    public void createMd5Hash_2() throws NoSuchAlgorithmException {
        String in = "sdo:VideoObject#-w219x82m";

        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] digest = md.digest(in.getBytes());

        BigInteger bigInteger = new BigInteger(1, digest);
        String res = dec2Base(bigInteger);
        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Hash: "+ res + "/" + res.length());
    }

    @Test
    public void createChecksumAdler32() throws NoSuchAlgorithmException {
        String in = "https://schema.org/VideoObject#-w219x82m";
        Checksum adler32 = new Adler32();
        adler32.update(in.getBytes(), 0, in.getBytes().length);


        BigInteger bigInteger = BigInteger.valueOf(adler32.getValue());
        String res = dec2Base(bigInteger);
        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Sum: "+ res + "/" + res.length());
    }

    @Test
    public void createChecksumCRC32() throws NoSuchAlgorithmException {
        String in = "https://schema.org/VideoObject#-w219x82m";
        Checksum checksum = new CRC32C();
        checksum.update(in.getBytes(), 0, in.getBytes().length);


        BigInteger bigInteger = BigInteger.valueOf(checksum.getValue());
        String res = dec2Base(bigInteger);
        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Sum: "+ res + "/" + res.length());
    }

    @Test
    public void createHashcode() throws NoSuchAlgorithmException {
        String in = "https://schema.org/VideoObject#-w219x82m";
        HashCode hashCode = Hashing.fingerprint2011().hashString(in, StandardCharsets.UTF_8);


        BigInteger bigInteger = BigInteger.valueOf(hashCode.asLong());
        String res = dec2Base(bigInteger);
        System.out.println(in);
        System.out.println(bigInteger +"/"+bigInteger.toString().length());
        System.out.println("Sum: "+ res + "/" + res.length());
    }

        private char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();


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
            return result.toString();
        }




}
