package org.av360.maverick.graph.model.identifier;

import java.math.BigInteger;
import java.util.Stack;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class ChecksumGenerator {

    private static final Checksum checksum = new CRC32C();
    private static final char[] alphabet = "abcdefghijklmnopqrstuvwyz0123456789_".toCharArray();


    public static String generateChecksum(String val, int length, char paddingChar) {
        checksum.reset();
        checksum.update(val.getBytes(), 0, val.length());

        return dec2Base(BigInteger.valueOf(checksum.getValue()), length, paddingChar);

    }


    private static String dec2Base(BigInteger number, int length, char paddingChar) {
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
        if(ser.length() > length) return ser.substring(0, length-1);
        if(ser.length() < length) return normalize(ser, length, paddingChar);
        else return ser;


    }

    private static String normalize(String str, int length, char paddingChar) {
        if (str.length() >= length) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(paddingChar);
        }

        return sb.toString();

    }

}
