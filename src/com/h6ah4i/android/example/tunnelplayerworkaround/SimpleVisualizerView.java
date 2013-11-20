
package com.h6ah4i.android.example.tunnelplayerworkaround;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

public class SimpleVisualizerView
        extends View
        implements Visualizer.OnDataCaptureListener {

    private Visualizer mVisualizer;
    private volatile byte[] mWaveform;
    private Paint mPaint;

    public SimpleVisualizerView(Context context) {
        super(context);
    }

    public SimpleVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleVisualizerView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private Paint getPaint() {
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setColor(0xffff0000);
        }
        return mPaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final byte[] waveform = mWaveform;

        if (waveform == null)
            return;
        
        // draw waveform

        final Paint p = getPaint();
        final int width = getWidth();
        final int height = getHeight();

        final float hcoeff = 2.0f;
        final float voffset = height / 2;
        final float htick = (float) width / waveform.length;

        float pv = 0;
        for (int i = 0; i < waveform.length; i++) {
            float v = hcoeff * ((byte) (waveform[i] + 128));
            canvas.drawLine(
                    htick * i, (voffset + pv),
                    htick * (i + 1), (voffset + v),
                    p);
            pv = v;
        }
    }

    public void bindAudioSession(int audioSession) {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.setDataCaptureListener(null, 0, false, false);
        }

        mVisualizer = new Visualizer(audioSession);
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(this,
                Visualizer.getMaxCaptureRate() / 2, true, false);
    }

    public void onResume() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(true);
        }
    }

    public void onPause() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
        }
    }

    @Override
    public void onFftDataCapture(
            Visualizer visualizer, byte[] fft, int samplingRate) {
    }

    @Override
    public void onWaveFormDataCapture(
            Visualizer visualizer, byte[] waveform, int samplingRate) {
        mWaveform = waveform;
        this.postInvalidate();
    }
}
