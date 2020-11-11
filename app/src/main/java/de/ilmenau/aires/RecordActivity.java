package de.ilmenau.aires;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static de.ilmenau.aires.MainActivity.AIRES_TAG;

public class RecordActivity extends AppCompatActivity {

    int rate, channel, encoding;
    int minBufferFactor = 4; //can be changed
    int myBufferSize = 8192;
    int minInternalBufferSize;
    boolean isRecording, isPlaying;
    String filePath;
    String fileName = "AIRES_record.pcm";
    String[] sources = {"CAMCORDER", //5
            "DEFAULT", //0
            "MIC", //1
            "UNPROCESSED", //9
            "VOICE_CALL", //4
            "VOICE_COMMUNICATION", //7
            "VOICE_DOWNLINK", //3
            "VOICE_PERFORMANCE", //10
            "VOICE_RECOGNITION", //6
            "VOICE_UPLINK"}; //2
    int[] intSources = {5, 0, 1, 9, 4, 7, 3, 10, 6, 2};

    TextView status;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    Spinner sourceSpinner;
    TextView currentSource;
    View rootView;

    BarFragment fragment = new BarFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        rootView = findViewById(R.id.currentSource);

        currentSource = findViewById(R.id.currentSource);
        status = findViewById(R.id.status);
        sourceSpinner = findViewById(R.id.sourceSpinner);
        sourceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sources));

        rate = getIntent().getIntExtra(getString(R.string.rate), 8000);
        channel = getIntent().getIntExtra(getString(R.string.channel), 12);
        encoding = getIntent().getIntExtra(getString(R.string.encoding), 2);

        sourceSpinner.setSelection(8);

        createAudioRecorder();

        filePath = Environment.getExternalStorageDirectory().getPath() + "/AIRES/" + fileName;

        getFragmentManager().beginTransaction().add(R.id.frgmCont, fragment).commit();
    }

    void createAudioRecorder() {
        minInternalBufferSize = AudioRecord.getMinBufferSize(rate, channel, encoding);
        int internalBufferSize = minInternalBufferSize * minBufferFactor; //recommended to use higher value than min supported

        Log.d(AIRES_TAG, "minInternalBufferSize = " + minInternalBufferSize + ", internalBufferSize = " + internalBufferSize
                + ", myBufferSize = " + myBufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                rate, channel, encoding, internalBufferSize);
        int ch = (channel == AudioFormat.CHANNEL_IN_STEREO) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                rate, ch, encoding, internalBufferSize,
                AudioTrack.MODE_STREAM);
        setCurrentSource();
    }

    void setCurrentSource() {
        try {
            if (audioRecord == null) return;
            for (int i = 0; i < intSources.length; i++) {
                if (audioRecord.getAudioSource() == intSources[i]) {
                    currentSource.setText(sources[i]);
                }
            }
        } catch (Exception ex) {
            Log.e(AIRES_TAG, "exxxx: " + ex.toString());
        }
    }

    byte[][] deinterleaveData(byte[] samples, int numChannels) {
        // assert(samples.length() % numChannels == 0);
        int numFrames = samples.length / numChannels;

        byte[][] result = new byte[numChannels][];
        for (int ch = 0; ch < numChannels; ch++) {
            result[ch] = new byte[numFrames];
            for (int i = 0; i < numFrames; i++) {
                result[ch][i] = samples[numChannels * i + ch];
            }
        }
        return result;
    }


    public void startRecording(View v) {
        setCurrentSource();
        //registerReceiver();
        startRecording();
    }

    private void startRecording() {
        if (isRecording) return;
        if (isPlaying) stopPlaying(null);

        try {
            audioRecord.startRecording();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            Log.e(AIRES_TAG, "ex: " + ex.toString());
        }
        isRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os = null;
                try {
                    os = new FileOutputStream(filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(AIRES_TAG, "FileNotFoundException while opening file to record : " + e.getMessage());
                }

                byte[] audioData = new byte[myBufferSize],
                        leftChannelAudioData = new byte[myBufferSize / 2],
                        rightChannelAudioData = new byte[myBufferSize / 2];
                int read;
                while (isRecording) {
                    read = audioRecord.read(audioData, 0, myBufferSize);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            os.write(audioData);
                            fragment.process(audioData, audioData.length);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(AIRES_TAG, "IOException while writing to file: " + e.getMessage());
                        }
                    } else {
                    }
                }

                try {
                    os.close();
                    if (channel == 12) {
                        Utils.rawToWave(new File(filePath), new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES/" + "cellphone_stereo_record"), 2, rate);
                    }
                    else {
                        Utils.rawToWave(new File(filePath), new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES/" + "cellphone_mono_record"), 1, rate);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(AIRES_TAG, "IOException while closing file after recording: " + e.getMessage());
                }
            }
        }).start();
    }

    public void startPlaying(View v) {
        if (isPlaying) return;
        if (isRecording) stopRecording(null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] audioData;
                try {
                    InputStream inputStream = new FileInputStream(filePath);
                    int ch = (channel == AudioFormat.CHANNEL_IN_STEREO) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
                    int minBufferSize = AudioTrack.getMinBufferSize(rate, ch, encoding);
                    audioData = new byte[1024];

                    audioTrack.play();
                    isPlaying = true;
                    int i = 0;
                    while (((i = inputStream.read(audioData)) != -1) && isPlaying) {
                        audioTrack.write(audioData, 0, i);
                        fragment.process(audioData, audioData.length);
                    }

                    audioTrack.stop();
                    isPlaying = false;
                    inputStream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(AIRES_TAG, "FileNotFoundException while playing audio: " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(AIRES_TAG, "IOException while playing audio: " + e.getMessage());
                }
            }
        }).start();
    }

    public void stopRecording(View v) {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    public void stopPlaying(View v) {
        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isRecording = false;
        if (audioRecord != null) {
            audioRecord.release();
        }

        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.release();
        }
    }

}
