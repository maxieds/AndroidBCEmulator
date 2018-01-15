package com.example.mschmidt34.nfccardemulator;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Vibrator;

/**
 * Created by mschmidt34 on 12/16/2017.
 */

public class LocalHostAPDUService extends HostApduService {

    private static final String TAG = "LocalHostApduService";
    private byte[] currentAID;
    public static int CYCLE_RESP = 1;

    public static final byte[] RESPONSE_OK = DesfireStatusWord.OPERATION_OK.toBytes();
    public static final byte[] UNKNOWN_INS_RESP = {(byte) 0x00, (byte) 0x00};

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {

        MainActivity.logAPDU(apdu, true);
        if(selectAIDApdu(apdu)) {
            // the card reader sent us a SELECT command, let's copy the requested AID:
            // 00 A4 04 00 xx AID                   00 (where xx = 08 here)
            //int aidBytes = apdu.length - 6;
            //currentAID = new byte[aidBytes];
            //System.arraycopy(apdu, 5, currentAID, 0, aidBytes);
            switch(CYCLE_RESP++ % 6) {
                case 0:
                    return sendResponseApduLocal(RESPONSE_OK);
                case 1:
                    return sendResponseApduLocal(Utils.packBytes("9320"));
                case 2:
                    return sendResponseApduLocal(Utils.packBytes("9520"));
                case 3:
                    return sendResponseApduLocal(Utils.packBytes("937088043c7bcbbb34"));
                case 4:
                    return sendResponseApduLocal(Utils.packBytes("95704a9849801b3abf"));
                case 5:
                    return sendResponseApduLocal(Utils.packBytes("e0803173"));
                case 6:
                    return sendResponseApduLocal(Utils.packBytes("027002400100ce0c"));
                default:
                    return sendResponseApduLocal(RESPONSE_OK);
            }
        }
        else if(getCardUIDApdu(apdu)) {
            int uidBytes = MainActivity.appConfig.buzzcard.buzzcardUID.length;
            byte[] uidResp = new byte[uidBytes + 2];
            System.arraycopy(RESPONSE_OK, 0, uidResp, uidBytes, 2); // STATUS_OK: 0x9100 [SW1 SW2]
            System.arraycopy(MainActivity.appConfig.buzzcard.buzzcardUID, 0, uidResp, 0, uidBytes);
            return sendResponseApduLocal(uidResp);
        }
        else if(authenticateApdu(apdu)) {
            MainActivity.logToConsole("STATUS: We have a problem, the reader wants to authenticate, so balk.");
            byte[] noSuchKeyResp = DesfireStatusWord.NO_SUCH_KEY.toBytes();
            return sendResponseApduLocal(noSuchKeyResp);
        }
        else {
            byte apduins = NFCUtils.INS(apdu);
            String dfins = DesFireInstruction.parseInstruction(apduins).name();
            MainActivity.logToConsole("STATUS: Received unknown/unhandled INS of " + dfins);
            return sendResponseApduLocal(UNKNOWN_INS_RESP);
        }

    }

    private byte[] sendResponseApduLocal(byte[] apduResp) {
        MainActivity.logAPDU(apduResp, false);
        //sendResponseApdu(apduResp);
        return apduResp;
    }

    private boolean selectAIDApdu(byte[] apdu) {
        return apdu.length >= 3 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4 && apdu[2] == (byte) 0x04;
    }

    private boolean getCardUIDApdu(byte[] apdu) {
        if(NFCUtils.INS(apdu) == (short) 0x51)
            return true;
        else
            return false;
    }

    private boolean authenticateApdu(byte[] apdu) {
        return NFCUtils.INS(apdu) == (byte) 0x0a ||
                NFCUtils.INS(apdu) == (byte) 0x1a ||
                NFCUtils.INS(apdu) == (byte) 0xaa;
    }

    @Override
    public void onDeactivated(int reason) {
        switch (reason) {
            case DEACTIVATION_LINK_LOSS:
                MainActivity.logToConsole("CONNECTION DEACTIVATED: NFC link lost.");
                MainActivity.logAPDUStatus("[Link Lost]");
                break;
            case DEACTIVATION_DESELECTED:
                MainActivity.logToConsole("CONNECTION DEACTIVATED: Another active AID was selected.");
                MainActivity.logAPDUStatus("[Another AID Selected]");
                break;
            default:
                MainActivity.logToConsole("CONNECTION LOST: For unknown reason.");
        }
    }

}
