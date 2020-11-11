package de.ilmenau.aires;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.*;
import com.google.android.material.snackbar.Snackbar;
import com.rohitss.uceh.UCEHandler;

import java.io.File;

public class MainActivity extends Activity {

    public final static String AIRES_TAG = "AIRES_TAG";
    
    boolean permissionsGranted = false;
    public static Integer[] channels = {AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO};
    public static Integer[] encodings = {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT};
    public static Integer[] rates = {8000, 11025, 22050, 32000, 44100, 48000, 96000};
    public static String[] chans = {"STEREO", "MONO"};
    public static String[] encs = {"16BIT", "8BIT"};
    Spinner rate, channel, encoding;
    View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.llMain);

        // Initialize view
        rate = findViewById(R.id.rate).findViewById(R.id.item_spinner);
        channel = findViewById(R.id.channel).findViewById(R.id.item_spinner);
        encoding = findViewById(R.id.encoding).findViewById(R.id.item_spinner);

        TextView tvRate = findViewById(R.id.rate).findViewById(R.id.item_text);
        TextView tvChannel = findViewById(R.id.channel).findViewById(R.id.item_text);
        TextView tvEncoding = findViewById(R.id.encoding).findViewById(R.id.item_text);

        tvRate.setText(getString(R.string.rate) + ":");
        tvChannel.setText(getString(R.string.channel) + ":");
        tvEncoding.setText(getString(R.string.encoding) + ":");

        setSpinnerValues();
        checkPermissions();

        new UCEHandler.Builder(getApplicationContext()).build();
    }

    private void setSpinnerValues(){
        rate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rates));
        channel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, chans));
        encoding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, encs));
        rate.setSelection(3);
    }

    public void goToRecord(View v) {
        int rt = (int)rate.getSelectedItem();
        int chn = channels[channel.getSelectedItemPosition()];
        int enc = encodings[encoding.getSelectedItemPosition()];

        if (validateSampleRates(rt, chn, enc)) {
            Intent intent = new Intent(this, RecordActivity.class);
            intent.putExtra(getString(R.string.rate), rt);
            intent.putExtra(getString(R.string.channel), chn);
            intent.putExtra(getString(R.string.encoding), enc);
            startActivity(intent);
            Log.d(AIRES_TAG, "start RecordActivity");
        }
    }

    public void goToDevices(View v) {
        int rt = (int)rate.getSelectedItem();
        int chn = channels[channel.getSelectedItemPosition()];
        int enc = encodings[encoding.getSelectedItemPosition()];

        if (validateSampleRates(rt, chn, enc)) {
            Intent intent = new Intent(this, ClientActivity.class);
            intent.putExtra(getString(R.string.rate), rt);
            intent.putExtra(getString(R.string.channel), chn);
            intent.putExtra(getString(R.string.encoding), enc);
            startActivity(intent);
            Log.d(AIRES_TAG, "start AudioDevicesActivity");
        }
    }

    public void runPython(View v) {
        int rt = (int)rate.getSelectedItem();
        int chn = channels[channel.getSelectedItemPosition()];
        int enc = encodings[encoding.getSelectedItemPosition()];

        if (validateSampleRates(rt, chn, enc)) {
            Intent intent = new Intent(this, PythonActivity.class);
            intent.putExtra(getString(R.string.rate), rt);
            intent.putExtra(getString(R.string.channel), chn);
            intent.putExtra(getString(R.string.encoding), enc);
            startActivity(intent);
            Log.d(AIRES_TAG, "start PythonActivity");
        }
    }

    public void runEvaluation(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Python.getInstance().getModule("main_all_bss_example").callAttr("main");
            }
        }).start();
    }

    public void closeApp(View v) {
        if (true) throw new NullPointerException("Exception when closing app");
    }

    private boolean validateSampleRates(int rt, int chn, int enc) {
        int minBuf = AudioRecord.getMinBufferSize(rt, chn, enc);
        if ((minBuf != AudioRecord.ERROR) && (minBuf != AudioRecord.ERROR_BAD_VALUE)) {
            Log.d(AIRES_TAG, "supported: rate=" + rt + ", chan=" + channel.getSelectedItem().toString()
                    + ", enc=" + encoding.getSelectedItem().toString() + ", minBuf = " + minBuf);
            return true;
        } else {
            Log.w(AIRES_TAG, "UNSUPPORTED: rate=" + rt + ", chan=" + channel.getSelectedItem().toString()
                    + ", enc=" + encoding.getSelectedItem().toString() + ", minBuf = " + minBuf);
            Snackbar.make(rootView, R.string.choose_another_samples, Snackbar.LENGTH_SHORT).show();
            return false;
        }
    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.INTERNET
            }, 0);
        } else {
            File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES");
            if (!folder.exists()) {
                folder.mkdir();
            }
            Log.d(AIRES_TAG, "All permissions are granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                boolean allGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        allGranted = false;
                    }
                }
                if(allGranted) {
                    File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES");
                    if (!folder.exists()) {
                        folder.mkdir();
                    }
                    permissionsGranted = true;
                    Log.d(AIRES_TAG, "All permissions are granted");
                } else {
                    Snackbar.make(rootView, R.string.provide_permissions, Snackbar.LENGTH_SHORT).show();
                    Log.w(AIRES_TAG, "Not all permissions are granted");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (true) throw new NullPointerException("Exception when closing app");
        super.onDestroy();
    }
}