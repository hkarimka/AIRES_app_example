package de.ilmenau.aires;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;


class VisualizerView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();

    private Paint mForePaint = new Paint();
    public boolean isFFT = false;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(getResources().getColor(R.color.colorAccent));
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        if (isFFT) {
            int n = mBytes.length;
            float[] magnitudes = new float[n / 2 + 1];
            float[] phases = new float[n / 2 + 1];
            magnitudes[0] = (float) Math.abs(mBytes[0]);      // DC
            magnitudes[n / 2] = (float) Math.abs(mBytes[1]);  // Nyquist
            phases[0] = phases[n / 2] = 0;
            for (int k = 1; k < n / 2; k++) {
                int i = k * 2;
                magnitudes[k] = (float) Math.hypot(mBytes[i], mBytes[i + 1]);
                phases[k] = (float) Math.atan2(mBytes[i + 1], mBytes[i]);
            }
            canvas.drawLines(magnitudes, mForePaint);
        } else {
            mRect.set(0, 0, getWidth(), getHeight());

            for (int i = 0; i < mBytes.length - 1; i++) {
                mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
                mPoints[i * 4 + 1] = mRect.height() / 2
                        + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
                mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
                mPoints[i * 4 + 3] = mRect.height() / 2
                        + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            }
            canvas.drawLines(mPoints, mForePaint);
        }

    }
}
