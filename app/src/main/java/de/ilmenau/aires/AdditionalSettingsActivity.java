package de.ilmenau.aires;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdditionalSettingsActivity extends AppCompatActivity {
    EditText etMaxDelay, etMaxAtten, etCoeffs, etCoeffsWeights, etPlayVolume;
    Button bSave;
    public static String PREFS_NAME = "AIRES_PREFS";
    public static String KEY_MAX_DELAY = "KEY_MAX_DELAY";
    public static String KEY_MAX_ATTEN = "KEY_MAX_ATTEN";
    public static String KEY_COEFFS = "KEY_COEFFS";
    public static String KEY_COEFFS_WEIGHTS = "KEY_COEFFS_WEIGHTS";
    public static String KEY_PLAY_VOLUME = "KEY_PLAY_VOLUME";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_settings);
        etMaxDelay = findViewById(R.id.etMaxDelay);
        etMaxAtten = findViewById(R.id.etMaxAtten);
        etCoeffs = findViewById(R.id.etCoeffs);
        etCoeffsWeights = findViewById(R.id.etCoeffsWeights);
        etPlayVolume = findViewById(R.id.etPlayVolume);
        bSave = findViewById(R.id.bSave);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String maxDelay = preferences.getString(KEY_MAX_DELAY, "20");
        String maxAtten = preferences.getString(KEY_MAX_ATTEN, "2");
        String coeffs = preferences.getString(KEY_COEFFS, "1.0, 1.0, 1.0, 1.0");
        String coeffsWeights = preferences.getString(KEY_COEFFS_WEIGHTS, "0.1, 0.1, 1.0, 1.0");
        String playVolume = preferences.getString(KEY_PLAY_VOLUME, "1.0");

        etMaxDelay.setText(maxDelay);
        etMaxAtten.setText(maxAtten);
        etCoeffs.setText(coeffs);
        etCoeffsWeights.setText(coeffsWeights);
        etPlayVolume.setText(playVolume);

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString(KEY_MAX_DELAY, etMaxDelay.getText().toString());
                editor.putString(KEY_MAX_ATTEN, etMaxAtten.getText().toString());
                editor.putString(KEY_COEFFS, etCoeffs.getText().toString());
                editor.putString(KEY_COEFFS_WEIGHTS, etCoeffsWeights.getText().toString());
                editor.putString(KEY_PLAY_VOLUME, etPlayVolume.getText().toString());
                editor.apply();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            }
        });
    }
}