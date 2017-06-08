package com.goodjob.musicplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Godot on 2017/6/8.
 */

public class VisualizerView extends View {
    private List<Integer> mFFT;
    private Rect mRect;
    private float points[] = new float[4];
    private Paint mPaint = new Paint();

    private int mSoundMinHz = 20;
    private int mSoundMaxHz = 20000;

    private int mSampleRate;

    public VisualizerView(Context context) {
        super(context);
        mRect = new Rect();
    }

    public void updateData(List<Integer> fft, int sampleRate) {
        mFFT = fft;
        mSampleRate = sampleRate / 1000;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFFT == null) {
            return;
        }

        List<Double> list = new ArrayList<>(mFFT.size());

        for (int i = 1; i < mFFT.size() / 2; ++i) {
            int hz = i * mSampleRate / 2 / mFFT.size();
            //Log.d("hz", hz + "");
            if (hz >= mSoundMinHz && hz <= mSoundMaxHz) {
                list.add(Math.hypot(mFFT.get(i << 1), mFFT.get(i << 1 | 1)));
            } else {
                Log.d("lose", hz + "");
            }
        }

        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);

        mRect.set(0, 0, getWidth(), getHeight());

        int sampleSize = (list.size() - 2) / 2;
        int partCount = 20;
        int partSize = sampleSize / partCount;

        float width = mRect.width() * 1.0f / partCount;

        mPaint.setStrokeWidth(width / 2);
        for (int i = 0; i < partCount; ++i) {
            double sum = 0;
            for (int j = 0; j < partSize; ++j) {
                sum += list.get(i * partSize + j);
            }
            double val = sum / partCount;
            //val = val;
            float left = i * width;
            float right = i * width + width;
            float bottom = mRect.bottom;
            float top = mRect.height() - (float) (val * mRect.height() / 32.0);

            points[0] = (right - left) / 2 + left;
            points[1] = bottom;
            points[2] = points[0];
            points[3] = top;

            canvas.drawLines(points, mPaint);

        }
    }
}
