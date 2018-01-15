package com.example.mschmidt34.nfccardemulator;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.example.mschmidt34.nfccardemulator.Utils.PP_ALL;
import static com.example.mschmidt34.nfccardemulator.Utils.PP_HEXSTRING;

/**
 * Created by mschmidt34 on 12/22/2017.
 */

public class BuzzcardCredentials implements Serializable{

    public static final int BCUID_BYTES = 8;
    public static final int BCIRST_BYTES = 16;
    public static final int OPERATION_OK = 0;

    public static final byte[] IRST_FIRST16_BYTES = new String("IRST XceedID 001").getBytes(StandardCharsets.UTF_8);

    public static int PENDING_ERROR = 0;
    private void setErrorFlag(int ecode) { PENDING_ERROR = ecode; }
    private void flushErrors() { PENDING_ERROR = 0; }

    public int gtID;
    public byte[] gtIDBytes;
    public int bocSecurityCode;
    public byte[] bocBytes;
    public byte[] buzzcardUID;
    public byte[] irstUniqueBytes;

    public BuzzcardCredentials(int gtid, int bcSecCode) {
        gtID = gtid;
        bocSecurityCode = bcSecCode;
        buzzcardUID = new byte[BCUID_BYTES];
        irstUniqueBytes = new byte[BCIRST_BYTES];
    }

    public int readBuzzcardFromTag(Tag nfcTag) throws Exception {
        IsoDep isoDep = IsoDep.get(nfcTag);
        isoDep.setTimeout(2000);
        if(!isoDep.isConnected())
             isoDep.connect();
        selectApplication(isoDep, "BBBBCD");
        byte[] rawFileData = readBinaryFile(isoDep, 0x01);
        rawFileData = new PackageBytes(rawFileData).reverseBytes();
        gtIDBytes = new PackageBytes(rawFileData).subarray(0, 9).getBytes();
        gtID = Integer.parseInt(new String(gtIDBytes, "UTF-8"), 10);
        bocBytes = new PackageBytes(rawFileData).subarray(10, 6).getBytes();
        bocSecurityCode = Integer.parseInt(new String(bocBytes, "UTF-8"), 10);
        buzzcardUID = getBuzzcardUID(isoDep);
        byte[] saResp = selectApplication(isoDep, "F532F0");
        byte[] irstFull = readBinaryFile(isoDep, 0x01);
        irstUniqueBytes = new PackageBytes(irstFull).subarray(2, 16).reverseBytes();
        isoDep.close();
        return OPERATION_OK;
    }

    public static final int CARD_HWVERSION = 0x01;
    public static final int CARD_HWSTORAGE = 0x02;
    public static final int CARD_SWVERSION = 0x04;
    public static final int CARD_SWSTORAGE = 0x08;
    public static final int CARD_DEVICEUID = 0x10;
    public static final int CARD_BATCHNUM = 0x20;
    public static final int CARD_WEEKOFPROD = 0x40;
    public static final int CARD_YEAROFPROD = 0x80;

    private Map<Integer, Byte[]> cardMapExtract(Map<Integer, Byte[]> dataMap, int cardDataSelect, int dataFlag, PackageBytes rawCardData, int startPos, int length) {
        if(Utils.hasMask(cardDataSelect, dataFlag))
            dataMap.put(dataFlag, rawCardData.subarray(startPos, length).getByteArray());
        return dataMap;
    }

    public Map<Integer, Byte[]> getCardData(IsoDep isoDep, int cardDataSelect) throws Exception {
        byte[] apdu = NFCUtils.wrapNativeDesfireAPDU(0x60, null);
        PackageBytes rawCardData = new PackageBytes();
        try {
            byte[] dataResp = isoDep.transceive(apdu);
            dataResp = NFCUtils.adjustDesfireResponseData(dataResp);
            rawCardData.append(dataResp);
            while(NFCUtils.MORE_DATA_READY(dataResp)) {
                dataResp = isoDep.transceive(NFCUtils.wrapNativeDesfireAPDU(0xaf, null));
                dataResp = NFCUtils.adjustDesfireResponseData(dataResp);
                rawCardData.append(dataResp);
            }
        } catch(Exception e) {
            MainActivity.logToConsole("ISODEP / APDU TRANSCEIVE: " + e.getMessage());
            throw new Exception(e);
        }
        Map<Integer, Byte[]> cardDataMap = new HashMap<Integer, Byte[]>();
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_HWVERSION, rawCardData, 4, 2);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_HWSTORAGE, rawCardData, 6, 1);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_SWVERSION, rawCardData, 12, 2);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_SWSTORAGE, rawCardData, 14, 1);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_DEVICEUID, rawCardData, 20, 7);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_BATCHNUM, rawCardData, 27, 5);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_WEEKOFPROD, rawCardData, 33, 1);
        cardDataMap = cardMapExtract(cardDataMap, cardDataSelect, CARD_YEAROFPROD, rawCardData, 34, 1);
        return cardDataMap;
    }

    public byte[] getBuzzcardUID(IsoDep isoDep) throws Exception {
        Byte[] uidBytes = getCardData(isoDep, CARD_DEVICEUID).get(CARD_DEVICEUID);
        return ArrayUtils.toPrimitive(uidBytes);
    }

    public byte[] selectApplication(IsoDep isoDep, String appID) throws Exception {
        byte[] appIDBytes = Utils.packBytes(appID);
        appIDBytes = new PackageBytes(appIDBytes).reverseBytes();
        byte[][] apduPayload = { appIDBytes };
        byte[] selectApdu = NFCUtils.wrapNativeDesfireAPDU(0x5a, apduPayload);
        try {
            byte[] dataResp = isoDep.transceive(selectApdu);
            dataResp = new PackageBytes(dataResp).reverseBytes();
            return dataResp;
        } catch(Exception e) {
            MainActivity.logToConsole("ISODEP / APDU TRANSCEIVE: " + e.getMessage());
            throw new Exception(e);
        }
    }

    public byte[] readBinaryFile(IsoDep isoDep, int fid) throws Exception {
        byte[][] fargs = {
                {(byte) fid},
                Utils.int2Bytes(0, 3),
                Utils.int2Bytes(0, 3),
        };
        byte[] apdu = NFCUtils.wrapNativeDesfireAPDU(0xbd, fargs);
        try {
            byte[] dataResp = isoDep.transceive(apdu);
            dataResp = new PackageBytes(dataResp).reverseBytes();
            return dataResp;
        } catch(Exception e) {
            MainActivity.logToConsole("ISODEP / APDU TRANSCEIVE: " + e.getMessage());
            throw new Exception(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder shortDesc = new StringBuilder("BuzzcardCredentials(");
        shortDesc.append(String.valueOf(gtID) + ", " + bocSecurityCode + ", ");
        shortDesc.append(Utils.bytes2Hex(buzzcardUID) + ", ");
        shortDesc.append(Utils.bytes2Hex(irstUniqueBytes) + ")");
        return shortDesc.toString();
    }

    public String toFancyString() {
        StringBuilder bcSummaryStr = new StringBuilder("GT BUZZCARD INFO:\n");
        bcSummaryStr.append("    ► GTID (decimal):         " + gtID + "\n");
        bcSummaryStr.append("    ► GTID (hex): " + Utils.bytes2Hex(String.valueOf(gtID).getBytes(StandardCharsets.UTF_8)) + "\n");
        bcSummaryStr.append("    ► GTID (hex): " + Utils.bytes2Hex(Utils.int2Bytes(gtID, 4)) + "\n");
        bcSummaryStr.append("    ► 6-digit Code (decimal): " + bocSecurityCode + "\n");
        bcSummaryStr.append("    ► bocData (hex): " + Utils.bytes2Hex(Utils.int2Bytes(bocSecurityCode, 4)) + "\n");
        bcSummaryStr.append("    ► GTID (hex): " + Utils.bytes2Hex(String.valueOf(bocSecurityCode).getBytes(StandardCharsets.UTF_8)) + "\n");
        bcSummaryStr.append("    ► Buzzcard UID (hex):     " + Utils.bytes2Hex(buzzcardUID) + "\n");
        bcSummaryStr.append("    ► IRST: " + Utils.bytes2Hex(irstUniqueBytes));
        return bcSummaryStr.toString();
    }

}
