package com.example.mschmidt34.nfccardemulator;

import android.app.DialogFragment;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by mschmidt34 on 12/25/2017.
 */

public class ActivateFragment extends DialogFragment {

    public static boolean ACTIVATE_STICKER_READY = false;

    public ActivateFragment() {}

    public static ActivateFragment newInstance(String dialogTitle) {
        ActivateFragment aFrag = new ActivateFragment();
        Bundle bargs = new Bundle();
        bargs.putString("title", dialogTitle);
        aFrag.setArguments(bargs);
        return aFrag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activate_sticker, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
        String dialogTitle = getArguments().getString("title", "Activate Application");
        getDialog().setTitle(dialogTitle);
    }

    public MainActivity.AppConfig activate(MifareUltralight stickerTag) {

        String authCode = ((EditText) getDialog().findViewById(R.id.activation_code)).getText().toString();
        WriteActivationStickerFragment.WriteActivationStickerSettings writeSettings = new WriteActivationStickerFragment.WriteActivationStickerSettings();
        if(authCode.length() < ActivationStickerIO.SECAUTHCODE_LENGTH) {
            writeSettings.securityAuthCode = ActivationStickerIO.DEFAULT_SECAUTHCODE;
        }
        else {
            writeSettings.securityAuthCode = authCode.substring(0, ActivationStickerIO.SECAUTHCODE_LENGTH);
        }

        MainActivity.AppConfig activateAppConfig = new MainActivity.AppConfig();
        BuzzcardCredentials bcCred;
        try {
            ActivationStickerIO asio = new ActivationStickerIO(null, writeSettings);
            bcCred = asio.readActivationTag(stickerTag);
        } catch(Exception e) {
            MainActivity.logToConsole("ACTIVATE ERROR: " + e.getMessage());
            e.printStackTrace();
            return activateAppConfig;
        }
        if(bcCred != null) {
            activateAppConfig.activated = true;
            activateAppConfig.buzzcard = bcCred;
        }
        return activateAppConfig;

    }

}
