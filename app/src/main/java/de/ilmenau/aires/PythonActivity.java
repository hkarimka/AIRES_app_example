package de.ilmenau.aires;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static de.ilmenau.aires.AdditionalSettingsActivity.KEY_COEFFS;
import static de.ilmenau.aires.AdditionalSettingsActivity.KEY_COEFFS_WEIGHTS;
import static de.ilmenau.aires.AdditionalSettingsActivity.KEY_MAX_ATTEN;
import static de.ilmenau.aires.AdditionalSettingsActivity.KEY_MAX_DELAY;
import static de.ilmenau.aires.AdditionalSettingsActivity.KEY_PLAY_VOLUME;
import static de.ilmenau.aires.AdditionalSettingsActivity.PREFS_NAME;
import static de.ilmenau.aires.MainActivity.AIRES_TAG;

public class PythonActivity extends AppCompatActivity {

    public static Integer[] channels = {AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO};
    public static String[] chans = {"STEREO", "MONO"};
    Spinner channel;
    EditText etIteration, etAlpha, etFilePath, etRate;
    TextView tvStatus, tvTime;
    boolean playChannel0 = true;
    String fileName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python);

        etRate = findViewById(R.id.etRate);
        channel = findViewById(R.id.channel).findViewById(R.id.item_spinner);
        etIteration = findViewById(R.id.etIteration);
        etAlpha = findViewById(R.id.etAlpha);
        tvStatus = findViewById(R.id.tvStatus);
        etFilePath = findViewById(R.id.etFilePath);
        setSpinnerValues();
    }

    private void setSpinnerValues() {
        channel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, chans));
    }

    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                Log.d(AIRES_TAG, "code " + resultCode);
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d(AIRES_TAG, "File Uri: " + uri.toString());
                    String path = getPath(uri);
                    etFilePath.setText(path);
                    Log.d(AIRES_TAG, "File Path: " + path);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void start(View v) {
        if (etFilePath.getText().toString().isEmpty()) {
            Toast.makeText(this, "Choose a file", Toast.LENGTH_SHORT).show();
            return;
        }
        new UnmixingTask().execute();
    }

    public void openAdditionalSettings(View v) {
        startActivity(new Intent(this, AdditionalSettingsActivity.class));
    }

    private class UnmixingTask extends AsyncTask<Void, Void, Void> {
        int rt = Integer.parseInt(etRate.getText().toString());
        int chn = channels[channel.getSelectedItemPosition()];
        int iteration = Integer.parseInt(etIteration.getText().toString());
        Float alpha = Float.parseFloat(etAlpha.getText().toString());
        String path = etFilePath.getText().toString();

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int maxDelay = Integer.parseInt(preferences.getString(KEY_MAX_DELAY, "20"));
        int maxAtten = Integer.parseInt(preferences.getString(KEY_MAX_ATTEN, "2"));
        String coeffs = preferences.getString(KEY_COEFFS, "1.0, 1.0, 1.0, 1.0");
        String coeffsWeights = preferences.getString(KEY_COEFFS_WEIGHTS, "0.1, 0.1, 1.0, 1.0");
        Float playVolume = Float.parseFloat(preferences.getString(KEY_PLAY_VOLUME, "1.0"));

        @Override
        protected void onPreExecute() {
            tvStatus.setText("In progress");
            String mes = "With values: " + maxDelay + "; " + maxAtten + "; " + coeffs + "; " + coeffsWeights + "; " + playVolume;
            Toast.makeText(getApplicationContext(), mes, Toast.LENGTH_LONG).show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvStatus.setText("Done");
            try {
                BufferedReader br = new BufferedReader(new FileReader("storage/emulated/0/AIRES/time.txt"));
                String line;
                line = br.readLine();
                br.close();
                tvStatus.setText("Done in " + line);
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Python.getInstance().getModule("main").callAttr("main", rt, iteration, alpha, path,
                    maxDelay, maxAtten, coeffs, coeffsWeights, playVolume);
            return null;
        }
    }

    private class GettingMixedChannelsTask extends AsyncTask<Void, Void, Void> {
        int rt = Integer.parseInt(etRate.getText().toString());
        String path = etFilePath.getText().toString();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MediaPlayer player = new MediaPlayer();
            try {
                if (playChannel0) {
                    player.setDataSource("storage/emulated/0/AIRES/mixed_" + fileName + "_channel_0.wav");
                } else {
                    player.setDataSource("storage/emulated/0/AIRES/mixed_" + fileName + "_channel_1.wav");
                }
                player.setLooping(false);
                player.prepare();
                player.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Python.getInstance().getModule("main").callAttr("get_two_channels", path, rt);
            return null;
        }
    }

    public void playCh0(View v) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource("storage/emulated/0/AIRES/separated_" + fileName + "_channel_0.wav");
            player.setLooping(false);
            player.prepare();
            player.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void playCh1(View v) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource("storage/emulated/0/AIRES/separated_" + fileName + "_channel_1.wav");
            player.setLooping(false);
            player.prepare();
            player.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void playCh0Before(View v) {
        playChannel0 = true;
        new GettingMixedChannelsTask().execute();
    }

    public void playCh1Before(View v) {
        playChannel0 = false;
        new GettingMixedChannelsTask().execute();
    }

    public void playBoth(View v) {
    }

    public void showFileChooser(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/AIRES/");
        intent.setDataAndType(uri, "*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public String getPath(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(0).replace(".wav", "");
                return "storage/emulated/0/AIRES/" + fileName + ".wav";
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }
}