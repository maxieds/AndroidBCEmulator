package com.example.mschmidt34.nfccardemulator;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static android.nfc.NdefRecord.RTD_ALTERNATIVE_CARRIER;
import static android.nfc.NdefRecord.RTD_HANDOVER_CARRIER;
import static android.nfc.NdefRecord.RTD_HANDOVER_REQUEST;
import static android.nfc.NdefRecord.RTD_HANDOVER_SELECT;
import static android.nfc.NdefRecord.RTD_SMART_POSTER;
import static android.nfc.NdefRecord.RTD_TEXT;
import static android.nfc.NdefRecord.RTD_URI;
import static android.nfc.NdefRecord.TNF_ABSOLUTE_URI;
import static android.nfc.NdefRecord.TNF_EMPTY;
import static android.nfc.NdefRecord.TNF_EXTERNAL_TYPE;
import static android.nfc.NdefRecord.TNF_MIME_MEDIA;
import static android.nfc.NdefRecord.TNF_UNCHANGED;
import static android.nfc.NdefRecord.TNF_UNKNOWN;
import static android.nfc.NdefRecord.TNF_WELL_KNOWN;

/**
 * Created by mschmidt34 on 12/22/2017.
 */

public class NFCUtils {

    public static byte INS(byte[] apdu) {
        if(apdu.length >= 2)
            return apdu[1];
        else
            return (byte)0x00;
    }

    public static byte[] AID(String appID) {
        if(appID.length() >=2 && appID.substring(0, 1) == "0x")
            appID = appID.substring(2);
        byte[] appIDBytes = Utils.packBytes(appID);
        return Utils.PackageBytesStatic.setBytes(appIDBytes).reverseBytes();
    }

    public static boolean MORE_DATA_READY(byte[] resp) {
        return resp.length >= 2 && resp[0] == (byte) 0x91 && resp[1] == (byte) 0xaf;
    }

    public static DesfireStatusWord getResponseStatus(byte[] dataResp) {
        int status1 = (int) dataResp[0], status0 = (int) dataResp[1];
        short statusWord = (short) (status0 | (status1 << 4));
        return DesfireStatusWord.getStatus(statusWord);
    }

    public static byte[] adjustDesfireResponseData(byte[] resp) {
        int dataBytes = resp.length;
        byte[] respCode = {resp[dataBytes - 2], resp[dataBytes - 1]};
        PackageBytes adjustedBytes = new PackageBytes(resp).subarray(0, dataBytes - 2);
        adjustedBytes.prepend(respCode);
        return adjustedBytes.getBytes();
    }

    public static boolean STATUS_OK(byte[] resp) {
        return getResponseStatus(resp) == DesfireStatusWord.OPERATION_OK;
    }

    String[] resolveApduError(byte[] respMsg) {
        String[] strErrorMsgs = new String[2]; // returns "ENUM", "Error String"
        return strErrorMsgs; // TODO
    }

    // options: reverse the bytes, etc.
    public class DesFireKeyBuilder {

        public static final int AESKEY = 0x0a;
        public static final int DESKEY = 0x0d;
        public static final int DES3KEY = 0x03d;
        public static final int MIFAREKEY = 0x0c;
        public static final int UNKNOWNKEY = 0xff;

        private byte[] requiredSalt = null;
        private byte[] optionalSalt = null;
        private byte[] rawBytes = null;
        private String passphrase = "";
        private Charset passEncoding = StandardCharsets.US_ASCII;
        private int desiredBits = 0;

        DesFireKeyBuilder(String pass, int numBits, byte[] requiredSalt, byte[] optionalSalt) {
            passphrase = pass;
            desiredBits = numBits;
            this.requiredSalt = requiredSalt;
            this.optionalSalt = optionalSalt;
            rawBytes = null;
        }

        public void setRawBytes(byte[] bytes) {
            rawBytes = bytes;
        }

        public byte[] buildKey() {
            return null;
        }

        public byte[][] buildKeys(int optimism) {
            return null;
        }

        public byte[] generateRandomKey(byte[] salt) {
            return null;
        }

        public byte[] ones() {
            return null;
        }

        public byte[] zeros() {
            return null;
        }

        public byte[][] getKnownKeysIndex() {
            return null;
        }

        public byte[][] getKnownKeysBuilderBytes() {
            return null;
        }

    }

    public static byte[] wrapNativeDesfireAPDU(int ins, byte[][] payloadParams) {
        byte[] cmdHeader = {
                (byte) 0x90,
                (byte) ins,
                (byte) 0x00,
                (byte) 0x00,
        };
        int payloadBytes = 0;
        PackageBytes workingAPDU = new PackageBytes(cmdHeader);
        PackageBytes payloadData = new PackageBytes();
        if(payloadParams != null) {
            payloadData = new PackageBytes();
            for (int p = 0; p < payloadParams.length; p++) {
                payloadBytes += payloadParams[p].length;
                payloadData.append(payloadParams[p]);
            }
            workingAPDU.append((byte) payloadBytes);
        }
        workingAPDU.append(payloadData.getBytes());
        workingAPDU.append((byte) 0x00);
        return workingAPDU.getBytes();
    }

    private static String resolveNdefRecordTNF(short tnfCode) {
        switch(tnfCode) {
            case TNF_ABSOLUTE_URI:
                return "TNF_ABSOLUTE_URI";
            case TNF_EMPTY:
                return "TNF_EMPTY";
            case TNF_EXTERNAL_TYPE:
                return "TNF_EXTERNAL_TYPE";
            case TNF_MIME_MEDIA:
                return "TNF_MIME_MEDIA";
            case TNF_UNCHANGED:
                return "TNF_UNCHANGED";
            case TNF_UNKNOWN:
                return "TNF_UNKNOWN";
            case TNF_WELL_KNOWN:
                return "TNF_WELL_KNOWN";
            default:
                return "<????>";
        }
    }

    private static String resolveNdefRecordType(byte[] type) {
        if(Arrays.equals(RTD_ALTERNATIVE_CARRIER, type))
            return "RTD_ALTERNATIVE_CARRIER";
        else if(Arrays.equals(RTD_HANDOVER_CARRIER, type))
            return "RTD_HANDOVER_CARRIER";
        else if(Arrays.equals(RTD_HANDOVER_REQUEST, type))
            return "RTD_HANDOVER_REQUEST";
        else if(Arrays.equals(RTD_HANDOVER_SELECT, type))
            return "RTD_HANDOVER_SELECT";
        else if(Arrays.equals(RTD_SMART_POSTER, type))
            return "RTD_SMART_POSTER";
        else if(Arrays.equals(RTD_TEXT, type))
            return "RTD_TEXT";
        else if(Arrays.equals(RTD_URI, type))
            return "RTD_URI";
        else
            return Utils.bytes2Hex(type);
    }

    public static String dumpNdefRecord(NdefRecord ndefRec) {
        StringBuilder recordSummary = new StringBuilder();
        recordSummary.append(resolveNdefRecordTNF(ndefRec.getTnf()) + ", ");
        recordSummary.append(resolveNdefRecordType(ndefRec.getType()) + ", id='");
        recordSummary.append(Utils.bytes2Hex(ndefRec.getId()) + "',\n");
        recordSummary.append("     " + Utils.dumpAscii(ndefRec.getPayload()));
        return recordSummary.toString();
    }


}
