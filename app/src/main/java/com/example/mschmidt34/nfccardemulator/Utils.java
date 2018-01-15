package com.example.mschmidt34.nfccardemulator;

import android.util.Log;

import java.lang.String;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by mschmidt34 on 12/16/2017.
 */

public class Utils {

    public static String byte2hex(byte b) {
        return String.format("%02x", (int) b & 0x00ff);
    }

    public static char byte2Ascii(byte b) {

        int decAsciiCode = (int) b;
        if (b >= 32 && b < 127) {
            char ch = (char) b;
            return ch;
        }
        else
            return '.';

    }

    public static String bytes2Ascii(byte[] bytes) {

        StringBuilder byteStr = new StringBuilder();
        for(int b = 0; b < bytes.length; b++)
            byteStr.append(String.valueOf(byte2Ascii(bytes[b])));
        return byteStr.toString();

    }

    public static String dumpAscii(byte[] bytes) {
        StringBuilder dumpStr = new StringBuilder();
        for(int i = 0; i < bytes.length; i++)
            dumpStr.append(byte2Ascii(bytes[i]));
        return dumpStr.toString();
    }

    public static String dumpHexAscii(byte[] bytes) {
        StringBuilder dumpStr = new StringBuilder();
        for(int b = 0; b < bytes.length; b++) {
            dumpStr.append(byte2hex(bytes[b]));
            if(b + 1 < bytes.length)
                dumpStr.append(":");
        }
        dumpStr.append(" | ");
        dumpStr.append(dumpAscii(bytes));
        return dumpStr.toString();
    }

    public static String bytes2Hex(byte[] bytes) {
        if(bytes == null)
            return "<NULL>";
        else if(bytes.length == 0)
            return "";
        StringBuilder hstr = new StringBuilder();
        hstr.append(String.format("%02x", bytes[0]));
        for(int b = 1; b < bytes.length; b++)
            hstr.append(":" + String.format("%02x", bytes[b]));
        return hstr.toString();
    }

    public static String byte2Binary(byte b) {
        String binStr = new String();
        int bint = (int) b, mask = 0x01;
        for(int s = 0; s < 8; s++) {
            String bitStr = (bint & mask) == 0 ? "0" : "1";
            binStr = bitStr + binStr;
            mask = mask << 1;
        }
        return binStr;
    }

    public static String bytes2Binary(byte[] bytes) {
        StringBuilder binStr = new StringBuilder();
        for(int b = 0; b < bytes.length; b++) {
            binStr.append(byte2Binary(bytes[b]));
            if (b + 1 < bytes.length)
                binStr.append(":");
        }
        return binStr.toString();
    }

    /*public static int bytes2Int32(byte[] bytes) {

        if(bytes.length < 4)
            return 0;
        short b0 = (short) bytes[0], b1 = (short) bytes[1];
        short b2 = (short) bytes[2], b3 = (short) bytes[3];
        return b0 | b1 << 8 | b2 << 16 | b3 << 24;

    }*/

    public static int bytes2Int32(byte[] bytes) {

        if(bytes.length < 4)
            return 0;
        int b0 = (int) bytes[0], b1 = (int) bytes[1];
        int b2 = (int) bytes[2], b3 = (int) bytes[3];
        return (b0 & 0xff) | ((b1 << 8) & 0xff00) | ((b2 << 16) & 0xff0000) | ((b3 << 24) & 0xff000000);

    }

    public static byte[] int2Bytes(int ivalue, int howMany) {
        byte[] bytes = new byte[howMany];
        int leftShift = 0x00, byteMask = 0xff;
        for(int i = 0; i < howMany; i++) {
            bytes[i] = (byte) ((ivalue >> leftShift) & byteMask);
            leftShift += 8;
        }
        return bytes;
    }

    public static byte[] short2Bytes(short svalue) {
        return int2Bytes((int) svalue, 2);
    }

    public static final int ONE = 0x01;
    public static final int ZERO = 0x00;

    public int extractBit(int bits, int bitNumber) {
        int bitmask = 0x01 << bitNumber;
        int bits2 = bits & bitmask;
        return bits2 >> bitNumber;
    }

    public int setBit(int bits, int bitNumber) {
        int bitmask = 0x01 << bitNumber;
        return bits | bitmask;
    }

    public int toggleBit(int bits, int bitNumber) {
        int bitValue = extractBit(bits, bitNumber) << bitNumber;
        bitValue ^= bitValue;
        return bits | bitValue;
    }

    public int replaceBit(int bits, int bit, int bitNumber) {
        int bitmask = ~(0x01 << bitNumber);
        int bits2 = bits & bitmask;
        return bits2 | (bit << bitNumber);
    }

    public int swapBits(int bits, int bitNumber0, int bitNumber1) {
        int bit0 = extractBit(bits, bitNumber0);
        int bit1 = extractBit(bits, bitNumber1);
        int bits2 = replaceBit(bits, bit0, bitNumber1);
        return replaceBit(bits2, bit1, bitNumber0);
    }

    public static byte reverseBits(byte b) {
        int bint = (int) b;
        int rb = 0x00;
        int mask = 0x01 << 7;
        for(int s = 0; s < 4; s++) {
            rb = rb | ((bint & mask) >> (8 / (b + 1) - 1));
            mask = mask >>> 1;
        }
        //return (byte) rb;
        mask = 0x01;
        for(int s = 0; s < 4; s++) {
            rb = rb | ((bint & mask) << (8 / (b + 1) - 1));
            mask = mask << 1;
            //Log.w(String.valueOf(mask), " MASK");
            //Log.w(String.valueOf(rb), " RB");
            //Log.w(String.valueOf(b), " INPUT BYTE");
            //Log.w(String.valueOf(bint & mask), "CURRENT BIT");
        }
        return (byte) rb;
    }

    public static byte packByte(int lsb, int msb) {
        int rb = 0x00;
        rb |= 0x0f & lsb;
        rb |= 0xf0 & (msb << 4);
        return (byte) rb;
    }

    public static int char2Int(char ch) {
        return Integer.parseInt(String.valueOf(ch), 16);
    }

    public static byte[] packBytes(String istr) {
        int numBytes = (istr.length() + 1) / 2; // ceiling of the length
        byte[] bytes = new byte[numBytes];
        for(int b = 0; b < istr.length() / 2; b++) {
            int msb = char2Int(istr.charAt(2 * b)), lsb = char2Int(istr.charAt(2 * b + 1));
            bytes[b] = packByte(lsb, msb);
        }
        if(istr.length() % 2 == 1) {
            bytes[numBytes - 1] = packByte(0x00, istr.charAt(istr.length() - 1));
        }
        return bytes;
    }

    public static int cyclicLeftShift(int data, int shiftBits) {
        int rdata = data << shiftBits;
        int lmostMask = 0xffffffff << (32 - shiftBits);
        int lmostBits = data & lmostMask;
        rdata |= lmostBits >> shiftBits;
        return rdata;
    }

    public static int cyclicRightShift(int data, int shiftBits) {
         int rdata = data >> shiftBits;
         int rmostMask = 0xffffffff >> (32 - shiftBits);
         int rmostBits = data & rmostMask;
         rdata |= rmostBits << (32 - shiftBits);
         return rdata;
    }

    public static byte LSB(int intValue) {
        return (byte) (0x000000ff & intValue);
    }

    public static byte MSB(int intValue) {
        return (byte) ((0xff000000 & intValue) >> 24);
    }

    public static int signBit(int intValue) {
        return (0x80000000 & intValue) >> 31;
    }

    public static byte[] getFixedBytes(String str, int numBytes, int padLength, boolean fromLeft) {
        if(fromLeft) {
            byte[] fixedBytes = str.substring(0, numBytes - 1).getBytes();
            PackageBytesStatic.setBytes(fixedBytes).padRight(padLength, (byte) 0x00);
            return PackageBytesStatic.getBytes();
        }
        else {
            byte[] fixedBytes = str.substring(str.length() - numBytes - 1, str.length() - 1).getBytes();
            PackageBytesStatic.setBytes(fixedBytes).padLeft(padLength, (byte) 0x00);
            return PackageBytesStatic.getBytes();
        }
    }

    public static String PP_INDENT = "    ";

    public static final int PP_HEXSTRING = 0x01;
    public static final int PP_BYTESTR = 0x40;
    public static final int PP_BYTESTR_FORMATTED = 0x02;
    public static final int PP_ASCII_FULL = 0x04;
    public static final int PP_ASCII_PRINTABLE = 0x08;
    public static final int PP_REVERSEDHEX = 0x10;
    public static final int PP_BINARYSTR = 0x20;
    public static final int PP_ALL = PP_HEXSTRING | PP_ASCII_FULL | PP_REVERSEDHEX | PP_BINARYSTR;

    public static boolean hasMask(int format, int mask) {
        return (format & mask) != 0;
    }

    public static String prettyPrint(byte[] bytes, int formatSelect) {
        StringBuilder ppStr = new StringBuilder("PRETTY PRINTING BYTE REPRESENTATION:\n");
        if(hasMask(formatSelect, PP_BYTESTR | PP_BYTESTR_FORMATTED | PP_HEXSTRING))
            ppStr.append(PP_INDENT + "HEX | " + bytes2Hex(bytes) + "\n");
        if(hasMask(formatSelect, PP_ASCII_FULL | PP_ASCII_PRINTABLE))
            ppStr.append(PP_INDENT + "ASC | " + bytes2Ascii(bytes) + "\n");
        if(hasMask(formatSelect, PP_REVERSEDHEX)) {
            byte[] revHexBytes = PackageBytesStatic.setBytes(bytes).reverseBits();
            ppStr.append(PP_INDENT + "RHX | " + bytes2Hex(revHexBytes) + "\n");
        }
        if(hasMask(formatSelect, PP_BINARYSTR))
            ppStr.append(PP_INDENT + "BIN | " + bytes2Binary(bytes) + "\n");
        return ppStr.toString();
    }

    public static String prettyPrint(int intValue, int formatSelect) {
        return prettyPrint(int2Bytes(intValue, 4), formatSelect);
    }

    public static String prettyPrint(String byteString, int formatSelect) {
        return prettyPrint(byteString.getBytes(), formatSelect);
    }

    public static PackageBytes PackageBytesStatic = new PackageBytes();

}
