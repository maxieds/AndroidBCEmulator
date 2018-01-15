package com.example.mschmidt34.nfccardemulator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import android.text.format.Time;
import android.widget.Toolbar;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.StrictMath.max;
import static org.apache.commons.lang3.math.NumberUtils.min;

public class MainActivity extends Activity implements ReaderCallback, NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback, NfcAdapter.CreateBeamUrisCallback {

    private static Context appContext;

    public static class AppConfig implements Serializable {

        public boolean activated;
        public BuzzcardCredentials buzzcard;
        private boolean admin;

        public AppConfig() {
            activated = false;
            buzzcard = null;
            admin = false;
        }

        public boolean getAdmin() {
            return admin;
        }

        public boolean readFromFile(String filePath) throws Exception {
            FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream in = new ObjectInputStream(fis);
            AppConfig oldConfig = (AppConfig) in.readObject();
            activated = oldConfig.activated;
            buzzcard = oldConfig.buzzcard;
            admin = oldConfig.admin;
            in.close();
            return true;
        }

        public boolean writeToFile(String filePath) throws Exception {
            FileOutputStream fos = new FileOutputStream(filePath);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.close();
            return true;
        }

        @Override
        public String toString() {
            String shortDesc = "AppConfig(activated=" + activated + ", " + buzzcard.toString() + ")";
            return shortDesc;
        }

    }

    public static int LONGER_TIMEOUT = 651; // milliseconds
    public static int DEFAULT_RPDELAY = 1000;
    public static boolean NO_DISABLE_NFC = false; // prevent the app from getting backgrounded on context shift
    public static final String APP_CONFIG_FILE = "config.cfg";

    protected static TextView consoleLoggerRef;
    protected static TextView apduLoggerRef;
    protected static TextView ndefLoggerRef;
    protected static TextView rpsSeekBarCountRef;
    protected NfcAdapter nfcAdapter;
    protected PendingIntent nfcPendingIntent;
    protected IntentFilter[] nfcIntentFilters;
    protected WriteActivationStickerFragment stickerWriterFrag;
    protected ActivateFragment activateFrag;
    protected static AppConfig appConfig;

    public static AppConfig getAppConfig() {
        return appConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView consoleLogger = (TextView) findViewById(R.id.consoleLogger);
        consoleLogger.setSelected(true);
        consoleLogger.setMovementMethod(new ScrollingMovementMethod());
        consoleLogger.setVerticalScrollBarEnabled(true);
        consoleLogger.setHorizontallyScrolling(false);
        consoleLoggerRef = consoleLogger;

        TextView apduLogger = (TextView) findViewById(R.id.apduLogger);
        apduLogger.setSelected(true);
        apduLogger.setMovementMethod(new ScrollingMovementMethod());
        apduLogger.setVerticalScrollBarEnabled(true);
        apduLogger.setHorizontallyScrolling(false);
        apduLoggerRef = apduLogger;
        logAPDUStatus("APDU INST / RESP (<--=Incoming from reader, -->=Outgoing):\n");

        TextView ndefLogger = (TextView) findViewById(R.id.ndefLogger);
        ndefLogger.setSelected(true);
        ndefLogger.setMovementMethod(new ScrollingMovementMethod());
        ndefLogger.setVerticalScrollBarEnabled(true);
        ndefLogger.setHorizontallyScrolling(false);
        ndefLoggerRef = ndefLogger;
        logNdefStatus("NDEF COMMUNICATIONS (<--=Incoming from reader, -->=Outgoing):\n");

        logDatestampToConsole();

        appContext = getApplicationContext();
        appConfig = new AppConfig();
        try {
            appConfig.readFromFile(getFilesDir() + "/" + APP_CONFIG_FILE);
        } catch(Exception ioe) {
            logToConsole("NO PREVIOUS CONFIG: Config File Must Not Exist.");
        }

        ActionBar actionbar = getActionBar();
        actionbar.setSubtitle(appConfig.getAdmin() ? "# Admin View" : "$ User View");
        actionbar.setTitle(R.string.actionbar_title);

        SeekBar rpsSeekBar = (SeekBar) findViewById(R.id.rpsSeekBar);
        rpsSeekBar.incrementProgressBy(100);
        rpsSeekBar.setOnSeekBarChangeListener(rpsSeekBarChangeListener);
        rpsSeekBarCountRef = (TextView) findViewById(R.id.rpsSeekBarCount);

        if(appConfig.getAdmin()) {
            //Button writeStickersButton = (Button) findViewById(R.id.write_stickers_button);
            //writeStickersButton.setEnabled(true);
            //writeStickersButton.setVisibility(View.VISIBLE);
        }
        //else { // setup icons for user view:
        //    MenuItem userIcon = (MenuItem) findViewById(R.id.action_ui_status);
        //    userIcon.setIcon(R.drawable.dollarsignicon32);
        //    userIcon.setTitle("User View");
        //}

        NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
        nfcAdapter =  nfcManager.getDefaultAdapter();
        if (nfcAdapter == null) {
            logToConsole(" ==== NFC ERROR ==== : This device does not appear to have appropriate NFC/NDEF capabilities ... \n");
            return;
        }
        else if (!nfcAdapter.isEnabled()) {
            logToConsole(" ==== NFC ERROR ==== : Enable NFC in Settings before using this app ... \n");
            Intent displaySettings = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(displaySettings);
        }

        // set this application as the default service for the default AID channel:
        //CardEmulation.getInstance(nfcAdapter).setPreferredService(this, new ComponentName(this, LocalHostAPDUService.class));
        //setPreferredService(this);

        // disable default beaming app behavior:
        nfcAdapter.setNdefPushMessage(null, this);
        NdefMessage setDefaultNdefRespMsg = getDefaultNdefResponseMessage();
        nfcAdapter.setNdefPushMessage(setDefaultNdefRespMsg, this);
        nfcAdapter.setNdefPushMessageCallback(this, this);
        if(!nfcAdapter.isNdefPushEnabled()) {
            logToConsole(" ==== NFC ERROR ==== : NFC adapter is (still) not NDEF push enabled.");
        }

        // status to see if the foreground process is capable of handling the initial reader messages:
        CardEmulation cardEmu = CardEmulation.getInstance(nfcAdapter);
        boolean[] canProcessAIDInForeground = new boolean[2];
        canProcessAIDInForeground[0] = cardEmu.categoryAllowsForegroundPreference("android.intent.category.DEFAULT");
        canProcessAIDInForeground[1] = cardEmu.categoryAllowsForegroundPreference("android.intent.category.OTHER");
        logToConsole("STATUS: App can process AID category DEFAULT/OTHER = " + canProcessAIDInForeground[0] + "/" + canProcessAIDInForeground[1]);

        Intent nfcIntent = new Intent(this, getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);
        nfcIntentFilters = new IntentFilter[4];
        nfcIntentFilters[0] = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        nfcIntentFilters[1] = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        nfcIntentFilters[2] = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        nfcIntentFilters[3] = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            nfcIntentFilters[2].addDataType("*/*");
            nfcIntentFilters[3].addDataType("*/*");
            nfcIntentFilters[3].addDataScheme("http");
            nfcIntentFilters[3].addDataAuthority("aptiqmobile.com", null);
        } catch (IntentFilter.MalformedMimeTypeException e) {}

    }

    @Override
    public void onResume() {

        super.onResume();
        logToConsole("STATUS: isResumed() = TRUE");
        try {
            //nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null);
            SeekBar rpsSeekBar = (SeekBar) findViewById(R.id.rpsSeekBar);
            int rpsValue = max(1000, rpsSeekBar.getProgress()) ;
            Bundle bundleExtras = new Bundle();
            bundleExtras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, rpsValue);
            logToConsole("STATUS: Set app EXTRA_READER_PRESENCE_CHECK_DELAY to " + rpsValue);
            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, bundleExtras);
            nfcAdapter.setNdefPushMessageCallback(this, this);
            setPreferredService(this);
            CardEmulation.getInstance(nfcAdapter).setPreferredService(this, new ComponentName(this, LocalHostAPDUService.class));
        } catch(Exception e) {
            if(nfcAdapter == null)
                logToConsole("STATUS: nfcAdapter is NULL!");
            else {
                logToConsole("EXCEPTION: " + e.getMessage());
                e.printStackTrace();
            }
        }
        logToConsole("STATUS: Enabled NFC adapter (foreground) reader mode.");

        // process intent:
        Intent intent = getIntent();
        onNewIntent(intent);

    }

    @Override
    public void onPause() {

        super.onPause();
        if(!NO_DISABLE_NFC) {
            //nfcAdapter.disableForegroundDispatch(this);
            nfcAdapter.disableReaderMode(this);
            CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this);
            logToConsole("STATUS: Paused NFC adapter (foreground) reader mode.");
        }

    }

    @Override
    public void onTagDiscovered(Tag nfcTag) {

        if(nfcTag == null) {
            logToConsole("STATUS: onTagDiscovered passed a null tag.");
            return;
        }

        NfcA nfcaTag = NfcA.get(nfcTag);
        nfcaTag.setTimeout(2000);

        logDatestampToConsole();
        logToConsole("NEW TAG DISCOVERED:");
        logToConsole("    [ID]   " + Utils.bytes2Hex(nfcTag.getId()));
        logToConsole("    [TECH] " + StringUtils.join(nfcTag.getTechList(), ","));

        if(stickerWriterFrag != null && stickerWriterFrag.READ_CARD_READY) {
            BuzzcardCredentials bcCred = new BuzzcardCredentials(0, 0);
            try {
                bcCred.readBuzzcardFromTag(nfcTag);
            } catch(Exception e) {
                logToConsole("TAG READ ERROR: Unable to read from buzzcard. \n=> " + e.getMessage());
                e.printStackTrace();
            }
            String bcContents = bcCred.toFancyString();
            logToConsole(bcContents);
            stickerWriterFrag.updateBuzzcardData(bcCred);
            stickerWriterFrag.READ_CARD_READY = false;
        }
        else if(stickerWriterFrag != null && stickerWriterFrag.WRITE_STICKER_READY) {
            if(!Arrays.asList(nfcTag.getTechList()).contains("android.nfc.tech.MifareUltralight")) {
                logToConsole("STICKER WRITE ERROR: Tag found is not an expected MifareUltralight sticker.");
                return;
            }
            BuzzcardCredentials bcCred = stickerWriterFrag.getBuzzcardData();
            WriteActivationStickerFragment.WriteActivationStickerSettings writeSettings = stickerWriterFrag.getWriteSettings();
            ActivationStickerIO stickerWriter = new ActivationStickerIO(bcCred, writeSettings);
            try {
                stickerWriter.formatNewStickerData(MifareUltralight.get(nfcTag));
            } catch(Exception ioe) {
                logToConsole("STICKER WRITE ERROR: " + ioe.getMessage());
                ioe.printStackTrace();
            }
            stickerWriterFrag.WRITE_STICKER_READY = false;
            stickerWriterFrag.dismiss();
            stickerWriterFrag = null;
        }
        else if(activateFrag != null && activateFrag.ACTIVATE_STICKER_READY) {
            if(!Arrays.asList(nfcTag.getTechList()).contains("android.nfc.tech.MifareUltralight")) {
                logToConsole("STICKER WRITE ERROR: Tag found is not an expected MifareUltralight sticker.");
                return;
            }
            appConfig = activateFrag.activate(MifareUltralight.get(nfcTag));
            logToConsole("ACTIVATION: " + (appConfig.activated ? "Success." : "Failed."));
            try {
                if(appConfig.activated) {
                    File cfgFile = new File(getFilesDir(), APP_CONFIG_FILE);
                    if (!cfgFile.exists()) {
                        cfgFile.mkdirs();
                        cfgFile.createNewFile();
                    }
                    appConfig.writeToFile(cfgFile.getAbsolutePath());
                }
            } catch(Exception ioe) {
                logToConsole("CONFIG WRITE ERROR: " + ioe.getMessage());
            }
            activateFrag.dismiss();
            activateFrag.ACTIVATE_STICKER_READY = false;
            activateFrag = null;
        }

    }

    public void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        if(intent == null)
            return;
        //setIntent(intent);
        logDatestampToConsole();
        logToConsole("NEW INTENT: Action of " + intent.getAction() + " of type " + intent.getType());

        // set a default timeout on all of these:
        if(intent.getAction().equals(nfcAdapter.ACTION_TAG_DISCOVERED) ||
                intent.getAction().equals(nfcAdapter.ACTION_TECH_DISCOVERED) ||
                intent.getAction().equals(nfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Tag nfcTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(nfcTag != null) {
                NfcA nfcaTag = NfcA.get(nfcTag);
                nfcaTag.setTimeout(2000);
            }
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(350);
        }

        boolean parseNdefDiscoveredAsTag = false;
        if(intent.getAction().equals(nfcAdapter.ACTION_TAG_DISCOVERED)) {
            Tag nfcTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            onTagDiscovered(nfcTag);
            if(nfcTag == null && intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) == null) {
                logToConsole("STATUS: Something's weird here, null tag and no extra NDEF messages.");
            }
            else if(nfcTag == null)
                parseNdefDiscoveredAsTag = true;
        }
        if (parseNdefDiscoveredAsTag || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                    NdefRecord[] msgRecords = messages[i].getRecords();
                    for(int m = 0; m < msgRecords.length; m++) {
                        logNdef(msgRecords[m], true);
                        // then parse the record later
                    }
                }
            }
        }

    }

    @Override
    public Uri[] createBeamUris(NfcEvent event) {
        logToConsole("STATUS: createBeamUris called.");
        if(event != null)
             logToConsole("NFC EVENT: " + event.peerLlcpMajorVersion + " / " + event.peerLlcpMinorVersion);
        return new Uri[0];
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        logToConsole("STATUS: createNdefMessage called.");
        if(event != null)
             logToConsole("NFC EVENT: " + event.peerLlcpMajorVersion + " / " + event.peerLlcpMinorVersion);
        NdefMessage returnMsg = getDefaultNdefResponseMessage();
        logNdef(returnMsg.getRecords()[0], false);
        return returnMsg;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        logToConsole("STATUS: onNdefPushComplete called.");
        if(event !=  null)
             logToConsole("NFC EVENT: " + event.peerLlcpMajorVersion + " / " + event.peerLlcpMinorVersion);
    }

    public boolean setPreferredService(Activity activity) {

        // check to see if we are the default service for the default AID:
        ComponentName service = new ComponentName(getApplicationContext(), LocalHostAPDUService.class);
        CardEmulation cardEmu = CardEmulation.getInstance(nfcAdapter);
        boolean isDefaultService = cardEmu.isDefaultServiceForAid(service, "@string/default_AID");
        boolean isDefaultService2 = cardEmu.isDefaultServiceForAid(service, String.format("%08x", R.string.default_AID));
        try {
            cardEmu.setPreferredService(this, service);
            logToConsole("STATUS: Default service for AID #" + String.format("%08x", R.string.default_AID) + " " + (isDefaultService | isDefaultService2));
            return true;
        } catch(Exception re) {
            logToConsole("NO PREFERRED SERVICE: " + re.getMessage());
            return false;
        }

    }

    public NdefMessage getDefaultNdefResponseMessage() {

        byte[] respText = "OPERATION_OK".getBytes(StandardCharsets.US_ASCII); // TODO: figure out exactly what needs to get sent here ...
        byte[] recordPayload = new byte[respText.length + 3];
        recordPayload[0] = (byte) 0x02; // UTF-8, 2-byte language code
        recordPayload[1] = (byte) 0x65; // 'e'
        recordPayload[2] = (byte) 0x6e; // 'n'
        System.arraycopy(respText, 0, recordPayload, 3, respText.length);
        NdefRecord ndefRec = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, null, recordPayload);
        return new NdefMessage(ndefRec);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rhs_app_navigation, menu);
        return true;
    }

    SeekBar.OnSeekBarChangeListener rpsSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // updated continuously as the user slides the thumb
            String progressString = String.format("% 4d ms", progress);
            rpsSeekBarCountRef.setText(progressString);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
            // signal to call onResume() to reset this flag in enableReader*:
            MainActivity.this.onResume();
        }

    };

    public void actionButtonNotImplemented(View view) {

        logToConsole("BUTTON ACTION FOR \'" + ((Button) view).getText().toString() + "\' NOT YET IMPLEMENTED ...");

    }

    public void actionButtonWriteActivationSticker(View view) {
        FragmentManager fm = getFragmentManager();
        stickerWriterFrag = WriteActivationStickerFragment.newInstance("Admin Activation Badge UI");
        stickerWriterFrag.show(fm, "sticker_writer_dialog_frag");
    }

    public void actionButtonReadBuzzcard(View view) {
        if(stickerWriterFrag == null)
            return;
        stickerWriterFrag.actionButtonReadBuzzcard(view);
    }

    public void actionButtonWriteActivationTag(View view) {
        if(stickerWriterFrag == null)
            return;
        stickerWriterFrag.WRITE_STICKER_READY = true;
        stickerWriterFrag.getDialog().setTitle("Scan Sticker Tag to Begin ...");
    }

    public void actionButtonCloseWriteActivationTagDialog(View view) {
        if(stickerWriterFrag == null)
            return;
        stickerWriterFrag.dismiss();
        stickerWriterFrag = null;
    }

    public void actionButtonClearLog(View view) {
        logToConsole("", false);
        ndefLoggerRef.setText("");
        apduLoggerRef.setText("");
    }

    public void actionButtonSaveLog(View view) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("buzzcard-emulator-Ymmdd-HHmmss.log", Locale.getDefault());
        String logFilePath  = dateFormat.format(new Date());
        String logFileData = ((TextView) findViewById(R.id.consoleLogger)).getText().toString();
        logFileData += "\n\nAPDU Exchange Log:\n" + ((TextView) findViewById(R.id.apduLogger)).getText().toString();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(view.getContext().openFileOutput(logFilePath, Context.MODE_PRIVATE));
            outputStreamWriter.write(logFileData);
            outputStreamWriter.close();
            logToConsole("LOG OUTPUT: Log file written to \"" + getFilesDir() + "/" + logFilePath +"\".");
        } catch(IOException ioe) {
            logToConsole("WRITE ERROR: " + ioe.getMessage());
        }
    }

    public void actionButtonAboutApp(View view) {
        StringBuilder aboutStr = new StringBuilder("ABOUT THE APPLICATION:\n");
        aboutStr.append("    ► Author:  Maxie D. Schmidt (maxieds@gmail.com)\n");
        aboutStr.append("    ► Repo:    https://bitbucket.org/maxieds/nfccardemulator.git\n");
        aboutStr.append("    ► Version: " + getResources().getString(R.string.app_version));
        logToConsole(aboutStr.toString());
    }

    public void actionButtonActivateDisplayDialog(View view) {
        FragmentManager fm = getFragmentManager();
        activateFrag = ActivateFragment.newInstance("Activate Application");
        activateFrag.show(fm, "activate_dialog_frag");
    }

    public void actionButtonActivate(View view) {
        if(activateFrag != null) {
            activateFrag.ACTIVATE_STICKER_READY = true;
            activateFrag.getDialog().setTitle("Scan Tag to Activate ...");
            EditText authCodeText = (EditText) activateFrag.getDialog().findViewById(R.id.activation_code);
            if(authCodeText.getText().toString().length() < ActivationStickerIO.SECAUTHCODE_LENGTH) {
                authCodeText.setText(ActivationStickerIO.DEFAULT_SECAUTHCODE);
            }
        }
    }

    public void actionButtonCancelActivate(View view) {
        if(activateFrag != null) {
            activateFrag.dismiss();
            activateFrag = null;
        }
    }

    public void actionButtonDisplayBuzzcardData(View view) {
        if(!appConfig.activated) {
            logToConsole("NO BUZZCARD DATA FOUND: Application not activated.");
            return;
        }
        else {
            logToConsole(appConfig.buzzcard.toFancyString());
            return;
        }
    }

    public void actionButtonInvokeBeam(View view) {
        if(nfcAdapter == null) {
            logToConsole("ERROR: Cannot invoke Beam on a null adapter.");
        }
        logToConsole("STATUS: Manually invoking Beam.");
        boolean beamSuccess = nfcAdapter.invokeBeam(this);
        logToConsole("STATUS: Invoking Beam " + (beamSuccess ? "succeeded" : "failed") + ".");
    }

    public void actionButtonOpenDoor(View view) {
        MainActivity.this.moveTaskToBack(true);
        logToConsole("STATUS: Moved app to background.");
    }

    public void moveAppToForeground() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        logToConsole("STATUS: Moved app to foreground.");
    }

    public static void logDatestampToConsole() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("(Y.mm.dd @ HH:mm:ss)", Locale.getDefault());
        String dateStamp  = dateFormat.format(new Date());
        appendHighlightedTextToConsole(consoleLoggerRef, dateStamp, Color.GREEN);
    }

    private static void appendHighlightedTextToConsole(TextView console, String msg, int color) {
        SpannableString sstr = new SpannableString(msg);
        sstr.setSpan(new ForegroundColorSpan(color), 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString prevText = (SpannableString) console.getText();
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(prevText);
        ssBuilder.append(sstr + "\n");
        console.setText(ssBuilder, TextView.BufferType.SPANNABLE);
    }

    private static void scrollToBottom(TextView tv) {
        final int scrollAmount = tv.getLayout().getLineTop(tv.getLineCount()) - tv.getHeight();
        if (scrollAmount > 0)
            tv.scrollTo(0, scrollAmount);
        else
            tv.scrollTo(0, 0);
    }

    public static void logToConsole(String msg, boolean append) {
        TextView console = consoleLoggerRef;
        if (append) {
            console.setText(console.getText() + msg);
        }
        else
            console.setText(msg);
        try {
            scrollToBottom(consoleLoggerRef);
        } catch(NullPointerException npe) {}
    }

    public static void logToConsole(String msg) {
        String msgLine = " >> " + msg + "\n";
        logToConsole(msgLine, true);
        Log.w(msgLine, "logToConsole");
        try {
            scrollToBottom(consoleLoggerRef);
        } catch(NullPointerException npe) {}
    }

    public static void logAPDU(byte[] apduResp, boolean incoming) {
        TextView console = apduLoggerRef;
        if (incoming) {
            String incomingIns = DesFireInstruction.parseInstruction(NFCUtils.INS(apduResp)).name();
            console.setText(console.getText() + "<-- " + Utils.dumpHexAscii(apduResp) + "\n[" + incomingIns + "]\n");
        }
        else {
            int outgoingRespCode = (((int) apduResp[0]) << 8) & 0xff00 | ((int) apduResp[1]) & 0x00ff;
            String outgoingResp = DesfireStatusWord.parseStatus((short) outgoingRespCode).name();
            console.setText(console.getText() + "--> " + Utils.dumpHexAscii(apduResp) + "\n[" + outgoingResp + "]\n");
        }
        try {
            scrollToBottom(apduLoggerRef);
        } catch(NullPointerException npe) {}
    }

    public static void logAPDUStatus(String statusMsg) {
        TextView console = apduLoggerRef;
        console.setText(console.getText() + statusMsg + "\n");
        try {
            scrollToBottom(apduLoggerRef);
        } catch(NullPointerException npe) {}
    }

    public static void logNdef(NdefRecord ndefRec, boolean incoming) {
        TextView console = ndefLoggerRef;
        if (incoming) {
            console.setText(console.getText() + "<-- " + NFCUtils.dumpNdefRecord(ndefRec) + "\n");
        }
        else
            console.setText(console.getText() + "--> " + NFCUtils.dumpNdefRecord(ndefRec) + "\n");
        try {
            scrollToBottom(ndefLoggerRef);
        } catch(NullPointerException npe) {}
    }

    public static void logNdefStatus(String statusMsg) {
        TextView console = ndefLoggerRef;
        console.setText(console.getText() + statusMsg + "\n");
        try {
            scrollToBottom(ndefLoggerRef);
        } catch(NullPointerException npe) {}
    }

}
