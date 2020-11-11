package de.ilmenau.aires;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Date;

import static de.ilmenau.aires.MainActivity.AIRES_TAG;
import static de.ilmenau.aires.MainActivity.channels;
import static de.ilmenau.aires.MainActivity.chans;
import static de.ilmenau.aires.MainActivity.encodings;
import static de.ilmenau.aires.MainActivity.encs;
import static de.ilmenau.aires.MainActivity.rates;


public class ClientActivity extends Activity {

    String filePath;
    String fileName = "AIRES_record.pcm";

    private int PORT = 50005;
    private int RECORDING_RATE = 44100;
    private int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int BUFFER = 1024;

    private EditText etPort, etBuffer, etFilename;
    private Spinner spRate, spChannel, spEncoding;
    View rootView;

    // the minimum buffer size needed for audio recording
    private int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, ENCODING);

    // are we currently sending audio data
    private boolean isReceiving, isPlaying;
    private DatagramSocket socket;
    private AudioTrack track;
    private BarFragment fragment = new BarFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        rootView = findViewById(R.id.llClient);

        Log.i(AIRES_TAG, "Creating the Audio Client with minimum buffer of "
                + BUFFER_SIZE + " bytes");
        filePath = Environment.getExternalStorageDirectory().getPath() + "/AIRES/";
        File file = new File(filePath);
        if (!file.exists()) file.mkdir();

        RECORDING_RATE = getIntent().getIntExtra(getString(R.string.rate), 44100);
        CHANNEL = getIntent().getIntExtra(getString(R.string.channel), 12);
        ENCODING = getIntent().getIntExtra(getString(R.string.encoding), 2);

        etPort = findViewById(R.id.etPort);
        spChannel = findViewById(R.id.spChannel);
        spRate = findViewById(R.id.spRate);
        spEncoding = findViewById(R.id.spEncoding);
        etBuffer = findViewById(R.id.etBuffer);
        etFilename = findViewById(R.id.etFilename);

        getFragmentManager().beginTransaction().add(R.id.frgmCont, fragment).commit();

        // set up the button
        Button startReceiving = findViewById(R.id.bStartReceiving);
        startReceiving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStreamingAudio();
            }
        });
        Button stopStreaming = findViewById(R.id.bStopReceiving);
        stopStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopStreamingAudio();
            }
        });
        Button play = findViewById(R.id.bStartPlay);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPlaying();
            }
        });
        Button stopPlay = findViewById(R.id.bStopPlay);
        stopPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPlaying();
            }
        });

        initSpinners();

        Log.d(AIRES_TAG, "Creating the AudioTrack");
        track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE, CHANNEL, ENCODING, BUFFER_SIZE, AudioTrack.MODE_STREAM);

    }

    private void initSpinners() {
        spRate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rates));
        spChannel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, chans));
        spEncoding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, encs));

        etFilename.setText(fileName);
        etPort.setText(String.valueOf(PORT));
        etBuffer.setText(String.valueOf(BUFFER));
        spEncoding.setSelection((ENCODING == 2) ? 0 : 1);
        spChannel.setSelection((CHANNEL == 12) ? 0 : 1);
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] == RECORDING_RATE) {
                Log.d(AIRES_TAG, "rate: " + RECORDING_RATE + ", i: " + i);
                spRate.setSelection(i);
            }
        }
    }

    private void startStreamingAudio() {
        if (isReceiving) {
            updateMessage(getString(R.string.already_receiving));
            return;
        }
        if (isPlaying) stopPlaying();
        RECORDING_RATE = (int) spRate.getSelectedItem();
        CHANNEL = channels[spChannel.getSelectedItemPosition()];
        ENCODING = encodings[spEncoding.getSelectedItemPosition()];
        String port = etPort.getText().toString();
        String buffer = etBuffer.getText().toString();
        String fName = etFilename.getText().toString();
        if (!port.isEmpty() && !buffer.isEmpty() && !fileName.isEmpty()) {
            try {
                PORT = Integer.parseInt(port);
                BUFFER = Integer.parseInt(buffer);
            } catch (Exception ex) {
                updateMessage(ex.getMessage());
                etPort.setText(String.valueOf(PORT));
                etBuffer.setText(String.valueOf(BUFFER));
            }
        } else {
            etPort.setText(String.valueOf(PORT));
            etBuffer.setText(String.valueOf(BUFFER));
            updateMessage(getString(R.string.enter_port_and_buffer));
            return;
        }

        Log.i(AIRES_TAG, "Starting the audio stream, " + PORT + BUFFER);
        isReceiving = true;
        hideKeyboard();
        startStreaming();
    }

    private void stopStreamingAudio() {
        Log.i(AIRES_TAG, "Stopping the audio stream");
        hideKeyboard();
        isReceiving = false;
        if (socket != null) socket.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) socket.close();
    }

    private void startStreaming() {
        Log.i(AIRES_TAG, "Starting the background thread to stream the audio data");
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    String fName = etFilename.getText().toString();
                    File file = new File(filePath + fName);
                    if (!fName.isEmpty()) {
                        if ((file.isFile() && file.delete() && file.createNewFile()) || file.createNewFile()) {
                            fileName = fName;
                        }
                    } else {
                        updateMessage(getString(R.string.entered_fname_error));
                        return;
                    }

                    Log.d(AIRES_TAG, "Creating the datagram socket");
                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(PORT));

                    OutputStream os = null;
                    os = new FileOutputStream(filePath + fileName);

                    Log.d(AIRES_TAG, "Creating the buffer of size " + BUFFER);
                    byte[] buffer = new byte[BUFFER];

                    Log.d(AIRES_TAG, "Creating the reuseable DatagramPacket");
                    DatagramPacket packet;

                    track.play();
                    Log.d(AIRES_TAG, "AudioTrack playing...");
                    boolean isToastShown = false;

                    updateMessage(getString(R.string.waiting_packets));
                    while (isReceiving) {
                        // place contents of buffer into the packet
                        packet = new DatagramPacket(buffer, buffer.length);
                        Log.d(AIRES_TAG, "time 1: " + new Date());
                        socket.receive(packet);
                        if (!isToastShown) updateMessage(getString(R.string.receiving_started));
                        isToastShown = true;
                        Log.d(AIRES_TAG, "time 2: " + new Date());

                        track.write(packet.getData(), 0, buffer.length);
                        os.write(packet.getData());
                        fragment.process(packet.getData(), BUFFER);
                    }
                    if (isToastShown) updateMessage(getString(R.string.receiving_stopped));
                    else updateMessage(getString(R.string.receiving_not_started));
                    Log.d(AIRES_TAG, "AudioRecord finished recording");
                    if (CHANNEL == 12) {
                        Utils.rawToWave(new File(filePath), new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES/" + "wifi_stereo_record"), 2, RECORDING_RATE);
                    }
                    else {
                        Utils.rawToWave(new File(filePath), new File(Environment.getExternalStorageDirectory().getPath() + "/AIRES/" + "wifi_mono_record"), 1, RECORDING_RATE);
                    }
                } catch (Exception e) {
                    isReceiving = false;
                    e.printStackTrace();
                    if (!e.getMessage().equals("Socket closed")) updateMessage(e.getMessage());
                    Log.e(AIRES_TAG, "Exception: " + e);
                }
            }
        });
        // start the thread
        streamThread.start();
    }


    private void stopPlaying() {
        hideKeyboard();
        isPlaying = false;
        if (track != null) {
            track.stop();
        }
    }

    private void updateMessage(final String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void startPlaying() {
        hideKeyboard();
        if (isPlaying) return;
        if (isReceiving) stopStreamingAudio();
        String fName = etFilename.getText().toString();
        if (!fName.isEmpty() && new File(filePath + fName).isFile()) {
            fileName = fName;
        } else {
            updateMessage(getString(R.string.file_not_found));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] audioData;
                try {

                    InputStream inputStream = new FileInputStream(filePath + fileName);
                    int ch = (CHANNEL == AudioFormat.CHANNEL_IN_STEREO) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
                    int minBufferSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNEL, ENCODING);
                    audioData = new byte[BUFFER];

                    track.play();
                    isPlaying = true;
                    int i = 0;

                    boolean isToastShown = false;
                    while (((i = inputStream.read(audioData)) != -1) && isPlaying) {
                        if (!isToastShown) updateMessage(getString(R.string.playing_started));
                        isToastShown = true;
                        track.write(audioData, 0, i);
                        fragment.process(audioData, BUFFER);
                    }

                    if (isToastShown) updateMessage(getString(R.string.playing_stopped));
                    else updateMessage(getString(R.string.playing_not_started));

                    track.stop();
                    isPlaying = false;
                    inputStream.close();

                } catch (Exception e) {
                    isPlaying = false;
                    updateMessage(e.getMessage());
                    e.printStackTrace();
                    Log.e(AIRES_TAG, "Exception while playing audio: " + e.getMessage());
                }
            }
        }).start();
    }
}
