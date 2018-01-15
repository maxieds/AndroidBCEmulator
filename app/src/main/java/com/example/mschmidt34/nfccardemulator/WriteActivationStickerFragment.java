package com.example.mschmidt34.nfccardemulator;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by mschmidt34 on 12/22/2017.
 */

public class WriteActivationStickerFragment extends DialogFragment {

    public boolean READ_CARD_READY = false;
    public boolean WRITE_STICKER_READY = false;

    private BuzzcardCredentials bcCred;

    public WriteActivationStickerFragment() {
        bcCred = new BuzzcardCredentials(0, 0);
    }

    public static WriteActivationStickerFragment newInstance(String dialogTitle) {
        WriteActivationStickerFragment wasFrag = new WriteActivationStickerFragment();
        Bundle bargs = new Bundle();
        bargs.putString("title", dialogTitle);
        wasFrag.setArguments(bargs);
        return wasFrag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.configure_sticker, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
        String dialogTitle = getArguments().getString("title", "Configure Badge UI");
        getDialog().setTitle(dialogTitle);
    }

    public void actionButtonReadBuzzcard(View view) {
        READ_CARD_READY = true;
        TextView bcDisplayTextView = (TextView) getDialog().findViewById(R.id.buzzcard_data_display);
        if(bcDisplayTextView == null) {
            MainActivity.logToConsole("ERROR: Buzzcard data display TextView is null!");
            return;
        }
        else
             bcDisplayTextView.setText("Ready to Read: Tap BuzzCard to back of the phone ... ");
    }

    public void updateBuzzcardData(BuzzcardCredentials bc) {
        if(bc == null)
            return;
        bcCred = bc;
        TextView bcDisplayTextView = (TextView) getDialog().findViewById(R.id.buzzcard_data_display);
        bcDisplayTextView.setText(bcCred.toFancyString());
    }

    public BuzzcardCredentials getBuzzcardData() {
        return bcCred;
    }

    public static class WriteActivationStickerSettings {

        public boolean lockTag;
        public boolean setDebit;
        public boolean verify;
        public boolean setAuthLim;
        public boolean blankTag;
        public String securityAuthCode;

        WriteActivationStickerSettings() {
            lockTag = false;
            setDebit = false;
            verify = false;
            setAuthLim = false;
            blankTag = true;
            securityAuthCode = ActivationStickerIO.DEFAULT_SECAUTHCODE;
        }

    }

    public WriteActivationStickerSettings getWriteSettings() {
        WriteActivationStickerSettings writeSettings = new WriteActivationStickerSettings();
        writeSettings.lockTag = ((CheckBox) getDialog().findViewById(R.id.lock_sticker_tag)).isChecked();
        writeSettings.verify = ((CheckBox) getDialog().findViewById(R.id.verify_sticker_tag)).isChecked();
        writeSettings.setAuthLim = ((CheckBox) getDialog().findViewById(R.id.set_sticker_authlim)).isChecked();
        writeSettings.blankTag = ((CheckBox) getDialog().findViewById(R.id.sticker_is_blank)).isChecked();
        String securityAuthCode = ((EditText) getDialog().findViewById(R.id.security_tag_code)).getText().toString();
        if(securityAuthCode.length() < ActivationStickerIO.SECAUTHCODE_LENGTH)
            writeSettings.securityAuthCode = ActivationStickerIO.DEFAULT_SECAUTHCODE;
        else
            writeSettings.securityAuthCode = securityAuthCode.substring(0, ActivationStickerIO.SECAUTHCODE_LENGTH);
        Log.w(writeSettings.securityAuthCode + " / " + writeSettings.securityAuthCode.length(), ", Set sec auth code string");
        return writeSettings;
    }

}
