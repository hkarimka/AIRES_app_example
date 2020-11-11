package de.ilmenau.aires;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;

public class BarFragment extends Fragment {
    TextView mdBTextView;
    TextView mdBFractionTextView;
    BarLevelDrawable mBarLevel;


    double mOffsetdB = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    //double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    double mGain = 1.0;
    // For displaying error in calibration.
    double mDifferenceFromNominal = 0.0;
    double mRmsSmoothed;  // Temporally filtered version of RMS.
    double mAlpha = 0.9;  // Coefficient of IIR smoothing filter for RMS.

    // Variables to monitor UI update and check for slow updates.
    private volatile boolean mDrawing;
    private volatile int mDrawingCollided;

    BarLevelDrawable mBarLevel2;


    double mOffsetdB2 = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    //double mGain2 = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    double mGain2 = 1.0;
    // For displaying error in calibration.
    double mDifferenceFromNominal2 = 0.0;
    double mRmsSmoothed2;  // Temporally filtered version of RMS.
    double mAlpha2 = 0.9;  // Coefficient of IIR smoothing filter for RMS.

    // Variables to monitor UI update and check for slow updates.
    private volatile boolean mDrawing2;
    private volatile int mDrawingCollided2;

    TextView mdBTextView2;
    TextView mdBFractionTextView2;

    public void process(byte[] buffer, int bufferSize) {
        byte[] leftChannelAudioData = new byte[bufferSize / 2],
                rightChannelAudioData = new byte[bufferSize / 2];
        for (int i = 0; i < buffer.length / 2; i = i + 2) {
            leftChannelAudioData[i] = buffer[2 * i];
            leftChannelAudioData[i + 1] = buffer[2 * i + 1];
            rightChannelAudioData[i] = buffer[2 * i + 2];
            rightChannelAudioData[i + 1] = buffer[2 * i + 3];
        }

        processAudioFrameLeft(leftChannelAudioData);
        processAudioFrameRight(rightChannelAudioData);
    }

    public void processAudioFrameLeft(byte[] audioFrame) {
        if (!mDrawing) {
            mDrawing = true;
            // Compute the RMS value. (Note that this does not remove DC).
            double rms = 0;
            for (int i = 0; i < audioFrame.length; i++) {
                rms += audioFrame[i] * audioFrame[i];
            }
            rms = Math.sqrt(rms / audioFrame.length);

            // Compute a smoothed version for less flickering of the display.
            mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
            final double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);

            // Set up a method that runs on the UI thread to update of the LED bar
            // and numerical display.
            mBarLevel.post(new Runnable() {
                @Override
                public void run() {
                    // The bar has an input range of [0.0 ; 1.0] and 10 segments.
                    // Each LED corresponds to 6 dB.
                    mBarLevel.setLevel((mOffsetdB + rmsdB) / 60);

                    DecimalFormat df = new DecimalFormat("##");
                    mdBTextView.setText(df.format(20 + rmsdB));

                    DecimalFormat df_fraction = new DecimalFormat("#");
                    int one_decimal = (int) (Math.round(Math.abs(rmsdB * 10))) % 10;
                    mdBFractionTextView.setText(Integer.toString(one_decimal));
                    mDrawing = false;
                }
            });
        }
    }

    public void processAudioFrameRight(byte[] audioFrame) {
        if (!mDrawing2) {
            mDrawing2 = true;
            // Compute the RMS value. (Note that this does not remove DC).
            double rms = 0;
            for (int i = 0; i < audioFrame.length; i++) {
                rms += audioFrame[i] * audioFrame[i];
            }
            rms = Math.sqrt(rms / audioFrame.length);

            // Compute a smoothed version for less flickering of the display.
            mRmsSmoothed2 = mRmsSmoothed2 * mAlpha2 + (1 - mAlpha2) * rms;
            final double rmsdB = 20.0 * Math.log10(mGain2 * mRmsSmoothed2);

            // Set up a method that runs on the UI thread to update of the LED bar
            // and numerical display.
            mBarLevel2.post(new Runnable() {
                @Override
                public void run() {
                    // The bar has an input range of [0.0 ; 1.0] and 10 segments.
                    // Each LED corresponds to 6 dB.
                    mBarLevel2.setLevel((mOffsetdB2 + rmsdB) / 60);

                    DecimalFormat df = new DecimalFormat("##");
                    mdBTextView2.setText(df.format(20 + rmsdB));

                    DecimalFormat df_fraction = new DecimalFormat("#");
                    int one_decimal = (int) (Math.round(Math.abs(rmsdB * 10))) % 10;
                    mdBFractionTextView2.setText(Integer.toString(one_decimal));
                    mDrawing2 = false;
                }
            });
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bar, container, false);
        mBarLevel = (BarLevelDrawable) view.findViewById(R.id.bar_level_drawable_view);
        mdBTextView = (TextView) view.findViewById(R.id.dBTextView);
        mdBFractionTextView = (TextView) view.findViewById(R.id.dBFractionTextView);

        mBarLevel2 = (BarLevelDrawable) view.findViewById(R.id.bar_level_drawable_view2);
        mdBTextView2 = (TextView) view.findViewById(R.id.dBTextView2);
        mdBFractionTextView2 = (TextView) view.findViewById(R.id.dBFractionTextView2);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}
