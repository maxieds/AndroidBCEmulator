package com.example.mschmidt34.nfccardemulator;

import android.support.v4.content.res.TypedArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by mschmidt34 on 12/22/2017.
 */

public class PackageBytes {

    byte[] localBytes = {};

    public PackageBytes() { }
    public PackageBytes(byte[] bytes) { localBytes = bytes; }
    public PackageBytes(int intValue) { localBytes = Utils.int2Bytes(intValue, 4); }
    public PackageBytes(String byteString) { localBytes = byteString.getBytes(); }

    public PackageBytes setBytes(byte[] bytes) {
        localBytes = bytes;
        return this;
    }

    public byte[] getBytes() {
        return localBytes;
    }
    public Byte[] getByteArray() {
        return ArrayUtils.toObject(localBytes);
    }

    public int size() {
        return localBytes.length;
    }

    public boolean isEmpty() {
        return localBytes == null || size() == 0;
    }

    public boolean equals(byte[] rhsBytes) {
        return localBytes.equals(rhsBytes);
    }

    public void truncate(int newSize) {
        byte[] newLocalBytes = new byte[newSize];
        System.arraycopy(localBytes, 0, newLocalBytes, 0, newSize);
        localBytes = newLocalBytes;
    }

    public void padLeft(int padLength, byte padding) {
        byte[] nextBytes = new byte[localBytes.length + padLength];
        Arrays.fill(nextBytes, padding);
        System.arraycopy(localBytes, 0, nextBytes, padLength, localBytes.length);
        localBytes = nextBytes;
    }

    public void padRight(int padLength, byte padding) {
        byte[] nextBytes = new byte[localBytes.length + padLength];
        Arrays.fill(nextBytes, padding);
        System.arraycopy(localBytes, 0, nextBytes, 0, localBytes.length);
        localBytes = nextBytes;
    }

    public void zeroPad(int newLength, boolean fromLeft) {
        if(fromLeft)
            padLeft(newLength, (byte) 0x00);
        else
            padRight(newLength, (byte) 0x00);
    }

    public void fill(byte cb) {
        Arrays.fill(localBytes, cb);
    }

    public void XOR(byte[] rhsBytes) {
        if(localBytes.length != rhsBytes.length)
            return;
        for(int b = 0; b < localBytes.length; b++)
            localBytes[b] ^= rhsBytes[b];
    }

    public void AND(byte[] rhsBytes) {
        if(localBytes.length != rhsBytes.length)
            return;
        for(int b = 0; b < localBytes.length; b++) {
            byte rhsByte = rhsBytes[b];
            localBytes[b] = (byte) (localBytes[b] & rhsByte);
        }
    }

    public void OR(byte[] rhsBytes) {
        if(localBytes.length != rhsBytes.length)
            return;
        for(int b = 0; b < localBytes.length; b++) {
            localBytes[b] |= rhsBytes[b];
        }
    }

    public void NOT(byte[] rhsBytes) {
        for(int b = 0; b < rhsBytes.length; b++)
            rhsBytes[b] = (byte) ~rhsBytes[b];
        localBytes = rhsBytes;
    }

    public void cyclicShiftRight(int numBits) throws Exception {
        throw new Exception("NOT IMPLEMENTED!");
    }

    public void cyclicShiftLeft(int numBits) throws Exception {
        throw new Exception("NOT IMPLEMENTED!");
    }

    public int toInt32() {
        return Utils.bytes2Int32(localBytes);
    }
    public long toInt64() {
        int lsi = Utils.bytes2Int32(localBytes);
        long msi = (long) this.subarray(4, 4).toInt32();
        long int64 = (long) (msi << 32) | lsi;
        return int64;
    }

    public short[] getShorts() throws Exception {
        throw new Exception("NOT IMPLEMENTED!");
    }

    public int[] getInts() throws Exception {
        throw new Exception("NOT IMPLEMENTED!");
    }

    public byte[] reverseBytes() {
        //return localBytes;
        byte[] revArray = new byte[localBytes.length];
        for(int b = 0; b < revArray.length; b++)
            revArray[revArray.length - b - 1] = localBytes[b];
        System.arraycopy(revArray, 0, localBytes, 0, localBytes.length);
        return localBytes;
    }

    public byte[] reverseBits() {
        reverseBytes();
        for(int b = 0; b < localBytes.length; b++) {
            localBytes[b] = Utils.reverseBits(localBytes[b]);
        }
        return localBytes;
    }

    public void prepend(byte b) {
        padLeft(1, b);
    }

    public void append(byte[] bytes) {
        int prevLength = localBytes.length;
        padRight(bytes.length, (byte) 0x00);
        System.arraycopy(bytes, 0, localBytes, prevLength, bytes.length);
    }

    public PackageBytes append(byte b) {
        padRight(1, b);
        return this;
    }

    public void prepend(byte[] bytes) {
        int prevLength = localBytes.length;
        padLeft(bytes.length, (byte) 0x00);
        System.arraycopy(bytes, 0, localBytes, 0, bytes.length);
    }

    public PackageBytes subarray(int startIdx, int length) {
        byte[] newLocalBytes = Arrays.copyOfRange(localBytes, startIdx, startIdx + length);
        return new PackageBytes(newLocalBytes);
    }

}

