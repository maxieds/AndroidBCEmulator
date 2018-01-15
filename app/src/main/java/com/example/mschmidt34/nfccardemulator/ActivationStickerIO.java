package com.example.mschmidt34.nfccardemulator;

import android.nfc.tech.MifareUltralight;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by mschmidt34 on 12/22/2017.
 */

public class ActivationStickerIO {

    public static final boolean WRITE_TAG_DIRTY_BITS = false;
    public static final boolean DEBIT_UNLOCKS = false;
    public static int DEFAULT_AUTHLIM = 25;
    public static int DEFAULT_UNLOCKS = 5;
    public static int SECAUTHCODE_LENGTH = 5;
    public static String DEFAULT_SECAUTHCODE = "12345";

    public static final byte[] DEFAULT_STICKER_PWD = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
    };
    public static final byte[] PACK_BYTES = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };
    private static final byte[] NUMERIC_SALT = {
            (byte) 0x57, (byte) 0x72, (byte) 0x15, (byte) 0x66,
    };
    private static final byte[] DEV_KEYSIG = "Drap!swo5!$e%1GWakdeibs=".getBytes();

    public BuzzcardCredentials bcCred = null;
    protected byte[] securityStickerKey = null;
    public boolean setAuthLimField = false;
    public boolean lockCard = false;
    public boolean setDebits = false;
    public boolean verify = false;
    public boolean blankTag = true;

    public ActivationStickerIO(BuzzcardCredentials bc, WriteActivationStickerFragment.WriteActivationStickerSettings writeSettings) {
        bcCred = bc;
        setWriteSettings(writeSettings);
    }

    public void setWriteSettings(WriteActivationStickerFragment.WriteActivationStickerSettings writeSettings) {
        lockCard = writeSettings.lockTag;
        verify = writeSettings.verify;
        setDebits = writeSettings.setDebit;
        setAuthLimField = writeSettings.setAuthLim;
        blankTag = writeSettings.blankTag;
        String ssKey = writeSettings.securityAuthCode;
        securityStickerKey = new PackageBytes(Utils.packBytes(ssKey)).append((byte) 0x00).getBytes();
    }

    public byte[] getStickerUID(MifareUltralight stickerTag) throws Exception {
        // get the new sticker's UID:
        byte[] initPageReadData = null;
        try {
            initPageReadData = stickerTag.readPages(0);
        } catch(IOException ioe) {
            MainActivity.logToConsole("TAG READ ERROR: " + ioe.getMessage());
            //return DesfireStatusWord.FILE_NOT_FOUND.toInt();
            throw ioe;
        }
        byte[] localStickerUID = new byte[8];
        System.arraycopy(initPageReadData, 0, localStickerUID, 0, 8);
        return localStickerUID;
    }

    private byte[] getPasswordBytes(byte[] securityStickerKey, byte[] stickerUID) {
        return diversifyPassword(securityStickerKey, stickerUID, DEV_KEYSIG, NUMERIC_SALT);
    }

    public int formatNewStickerData(MifareUltralight stickerTag) throws Exception{

        if(stickerTag == null) {
            return DesfireStatusWord.NO_CHANGES.toInt();
        }
        try {
            stickerTag.setTimeout(2000);
            if(!stickerTag.isConnected())
                 stickerTag.connect();
        } catch(IOException ioe) {
            throw ioe;
            //return DesfireStatusWord.DEVICE_CONNECT_ERROR.toInt();
        }

        // authenticate with the expected password first:
        byte[] stickerUID = getStickerUID(stickerTag);
        byte[] stickerPwdBytes = getPasswordBytes(securityStickerKey, stickerUID);
        authenticate(stickerTag, stickerPwdBytes);

        // prepare the data fields and write the important buzzcard fields to the sticker:
        byte[] bcuidSecondPageBytes = new byte[4];
        System.arraycopy(bcCred.buzzcardUID, 4, bcuidSecondPageBytes, 0, 3);
        bcuidSecondPageBytes[3] = (byte) 0x00;
        try {
            stickerTag.writePage(0x04, Utils.int2Bytes(bcCred.gtID, 4));
            stickerTag.writePage(0x05, Utils.int2Bytes(bcCred.bocSecurityCode, 4));
            stickerTag.writePage(0x06, new PackageBytes(bcCred.buzzcardUID).subarray(0, 4).getBytes());
            stickerTag.writePage(0x07, bcuidSecondPageBytes);
            stickerTag.writePage(0x08, new PackageBytes(bcCred.irstUniqueBytes).subarray(0, 4).getBytes());
            stickerTag.writePage(0x09, Arrays.copyOfRange(bcCred.irstUniqueBytes, 4, 8));
            stickerTag.writePage(0x0a, Arrays.copyOfRange(bcCred.irstUniqueBytes, 8, 12));
            stickerTag.writePage(0x0b, Arrays.copyOfRange(bcCred.irstUniqueBytes, 12, 16));
        } catch(IOException ioe) {
            MainActivity.logToConsole("TAG WRITE ERROR: " + ioe.getMessage());
            //return DesfireStatusWord.PAGE_WRITE_ERROR.toInt();
            throw ioe;
        }

        // now write the trailing, password protected fields to the end of the sticker:
        try {
            stickerTag.writePage(0x0c, new PackageBytes(DEV_KEYSIG).subarray(0, 4).getBytes());
            stickerTag.writePage(0x0d, Arrays.copyOfRange(DEV_KEYSIG, 4, 8));
            stickerTag.writePage(0x0e, Arrays.copyOfRange(DEV_KEYSIG, 8, 12));
            stickerTag.writePage(0x0f, NUMERIC_SALT);
        } catch(IOException ioe) {
            MainActivity.logToConsole("TAG WRITE ERROR: " + ioe.getMessage());
            //return DesfireStatusWord.PAGE_WRITE_ERROR.toInt();
            throw ioe;
        }

        // set the password on the protected sticker fields:
        try {
            stickerTag.writePage(0x12, stickerPwdBytes);
            stickerTag.writePage(0x13, PACK_BYTES);
        } catch(IOException ioe) {
            //return DesfireStatusWord.PAGE_WRITE_ERROR.toInt();
            throw ioe;
        }

        // final password protection (read / write), authlim, and lock sticker config:
        byte ACCESS_BYTE = (byte) 0x80;
        if(setAuthLimField) {
            ACCESS_BYTE |= (byte) DEFAULT_AUTHLIM;
        }
        byte[] finalConfigBytesPage1 = {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, // MEMORY PAGE PROT
        };
        byte[] finalConfigBytesPage2 = {
                ACCESS_BYTE, (byte) 0x00, (byte) 0x00, (byte) 0x00, // MEMORY PAGE PROT
        };
        try {
            stickerTag.writePage(0x10, finalConfigBytesPage1);
            stickerTag.writePage(0x11, finalConfigBytesPage2);
        } catch(IOException ioe) {
            //return DesfireStatusWord.PAGE_WRITE_ERROR.toInt();
            throw ioe;
        }

        if(lockCard) { // have to call another operation here:
            byte[] lockPageBytes = new PackageBytes(stickerTag.readPages(0x02)).subarray(0, 4).getBytes();
            lockPageBytes[2] = (byte) 0xfd;
            lockPageBytes[3] = (byte) 0xff;
            stickerTag.writePage(0x02, lockPageBytes);
        }
        if(verify) {
            BuzzcardCredentials bc = readActivationTag(stickerTag);
            if(bc == null || bc.gtID != bcCred.gtID || bc.bocSecurityCode != bcCred.bocSecurityCode ||
                    !Arrays.equals(bc.buzzcardUID, bcCred.buzzcardUID) ||
                    !Arrays.equals(bc.irstUniqueBytes, bcCred.irstUniqueBytes)) {
                throw new Exception("Verify of sticker write data failed.");
            }
        }

        // cleanup and close I/O on the sticker:
        try {
            if(stickerTag.isConnected())
                 stickerTag.close();
        } catch(IOException ioe) {
            throw ioe;
        }
        return BuzzcardCredentials.OPERATION_OK;

    }

    private boolean authenticate(MifareUltralight stickerTag,  byte[] passwordBytes) {
        byte[] authCmdBytes = new byte[5];
        authCmdBytes[0] = 0x1b;
        if(blankTag)
            System.arraycopy(DEFAULT_STICKER_PWD, 0, authCmdBytes, 1, 4);
        else
            System.arraycopy(new PackageBytes(passwordBytes).getBytes(), 0, authCmdBytes, 1, 4);
        try {
            byte[] authResp = stickerTag.transceive(authCmdBytes);
            if (authResp.length < 2 || authResp[0] != PACK_BYTES[0] || authResp[1] != PACK_BYTES[1]) {
                MainActivity.logToConsole("NFC STICKER AUTH ERROR: Unable to authenticate password.");
                return false;
            }
        } catch(Exception ioe) {
            MainActivity.logToConsole("AUTH ERROR: " + ioe.getMessage());
            ioe.printStackTrace();
        }
        return true;
    }

    private boolean validateStickerTag(MifareUltralight stickerTag) throws Exception {
        byte[] stickerUID = getStickerUID(stickerTag);
        byte[] stickerPwdBytes = getPasswordBytes(securityStickerKey, stickerUID);
        if(!authenticate(stickerTag, stickerPwdBytes))
            return false;
        PackageBytes protMemData = new PackageBytes();
        protMemData.append(stickerTag.readPages(0x0c));
        PackageBytes expectedMemData = new PackageBytes(DEV_KEYSIG).subarray(0, 12);
        expectedMemData.append(NUMERIC_SALT);
        return Arrays.equals(expectedMemData.getBytes(), protMemData.getBytes());
    }

    public BuzzcardCredentials readActivationTag(MifareUltralight stickerTag) throws Exception {
        if(!stickerTag.isConnected())
            stickerTag.connect();
        blankTag = false;
        if(!validateStickerTag(stickerTag)) {
            return null;
        }
        BuzzcardCredentials bc = new BuzzcardCredentials(0, 0);
        PackageBytes bcDataBytes = new PackageBytes();
        bcDataBytes.append(stickerTag.readPages(0x04));
        bcDataBytes.append(stickerTag.readPages(0x08));
        bc.gtID = Utils.bytes2Int32(bcDataBytes.subarray(0, 4).getBytes());
        bc.bocSecurityCode = Utils.bytes2Int32(bcDataBytes.subarray(4, 4).getBytes());
        bc.buzzcardUID = bcDataBytes.subarray(8, 7).getBytes();
        bc.irstUniqueBytes = bcDataBytes.subarray(16, 16).getBytes();
        stickerTag.close();
        return bc;
    }

    private byte[] diversifyPassword(byte[] pwd, byte[] uid, byte[] dkey, byte[] salt) {
         int pwdInt = Utils.bytes2Int32(pwd);
         int uidInt = Utils.bytes2Int32(uid);
         int dkeyInt = Utils.bytes2Int32(dkey);
         int saltInt = Utils.bytes2Int32(salt);
         pwdInt ^= uidInt;
         pwdInt ^= dkeyInt;
         pwdInt = Utils.cyclicRightShift(pwdInt, saltInt % 32);
         return Utils.int2Bytes(pwdInt, 4);
    }

}
