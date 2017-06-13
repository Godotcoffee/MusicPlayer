package com.goodjob.musicplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Godot on 2017/6/8.
 */

public class VisualizerView extends View {
    private List<Integer> mFFT;
    private Rect mRect;
    private float mPoints[] = new float[4];
    private Paint mMainPaint = new Paint();
    private Paint mBlockPaint = new Paint();

    private int mBlockWidth = 4;

    LinearGradient mLinearGradient;

    private static final int[] SAMPLE_HZ = new int []{
            0, 50, 94, 129, 176, 241, 331, 453, 620, 850, 1100, 1400, 1600, 2100, 3000, 4100, 6000, 8000, 11000, 15000, 19000
    };

    private float[] mLastHeight = new float[SAMPLE_HZ.length];
    private int[] mLastCnt = new int[SAMPLE_HZ.length];

    private int[] mColorGradient = new int [] {
            Color.rgb(50, 15, 200), Color.rgb(50, 50, 200), Color.rgb(50, 50, 150),
            Color.rgb(20, 15, 50), Color.rgb(20, 15, 50), Color.rgb(20, 15, 50)
    };

    private int mSampleRate;

    public VisualizerView(Context context) {
        super(context);
        mRect = new Rect();

        mMainPaint.setStyle(Paint.Style.STROKE);
        mBlockPaint.setColor(Color.RED);
        mBlockPaint.setStyle(Paint.Style.STROKE);
        mBlockPaint.setStrokeWidth(mBlockWidth);
        mBlockPaint.setAntiAlias(true);

        Arrays.fill(mLastHeight, -1);

        mLinearGradient = new LinearGradient(0, 0, 0, mRect.bottom,
                mColorGradient,
                null, LinearGradient.TileMode.CLAMP);
    }

    public void updateData(List<Integer> fft, int sampleRate) {
        mFFT = fft;
        mSampleRate = sampleRate / 1000;
        invalidate();
    }

    private static boolean indexRangeInFFT(List<Integer> fft, int pos) {
        return (pos + 1) << 1 > 0 && ((pos + 1) << 1 | 1) < fft.size();
    }

    private static double getSwingFromFFT(List<Integer> fft, int pos) {
        return Math.hypot(fft.get((pos + 1) << 1), fft.get((pos + 1) << 1 | 1));
    }

    private static final double[] AVG_WEIGHT = new double[] {0.5, 0.2, 0.1};
    private static double getAverageFromFFT(List<Integer> fft, int pos) {
        double sum = 0;
        double sumWeight = 0;

        for (int i = 0; i < AVG_WEIGHT.length && indexRangeInFFT(fft, pos + i); ++i) {
            sum += getSwingFromFFT(fft, pos + i);
            sumWeight += AVG_WEIGHT[i];
        }

        for (int i = 1; i < AVG_WEIGHT.length && indexRangeInFFT(fft, pos - i); ++i) {
            sum += getSwingFromFFT(fft, pos - i);
            sumWeight += AVG_WEIGHT[i];
        }

        return sum / sumWeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFFT == null) {
            return;
        }
        if (mRect.width() != getWidth() || mRect.height() != getHeight()) {
            mRect.set(0, 0, getWidth(), getHeight());
            mLinearGradient = new LinearGradient(0, 0, 0, mRect.bottom,
                    mColorGradient,
                    null, LinearGradient.TileMode.CLAMP);
        }

        float width = mRect.width() * 1.0f / SAMPLE_HZ.length;

        mMainPaint.setStrokeWidth(width / 2);
        mMainPaint.setShader(mLinearGradient);
        for (int i = 0; i < SAMPLE_HZ.length; ++i) {
            double val = getAverageFromFFT(mFFT, (int) Math.round(SAMPLE_HZ[i] * 1.0 / mSampleRate * mFFT.size()));
            val = 32 * Math.log10(val / 8);
            float left = i * width;
            float right = i * width + width;
            float bottom = mRect.bottom;
            float top = mRect.height() - (float) (val * mRect.height() / 64.0);

            mPoints[0] = (right - left) / 2 + left;
            mPoints[1] = bottom;
            mPoints[2] = mPoints[0];
            mPoints[3] = top;

            canvas.drawLines(mPoints, mMainPaint);
            if (top < mLastHeight[i] || mLastHeight[i] < 0) {
                mLastHeight[i] = top;
                mLastCnt[i] = 0;
                canvas.drawLine(left + (right - left) / 4, top - mBlockWidth / 2, right - (right - left) / 4,
                        top - mBlockWidth / 2, mBlockPaint);
            } else {
                if (mLastHeight[i] + (mLastCnt[i] + 1) * 0.5f <= mRect.bottom - mBlockWidth) {
                    ++mLastCnt[i];
                    mLastHeight[i] = mLastHeight[i] + mLastCnt[i] * 0.5f;
                } else {
                    mLastHeight[i] = mRect.bottom - mBlockWidth;
                }
                canvas.drawLine(left + (right - left) / 4, ++mLastHeight[i], right - (right - left) / 4, ++mLastHeight[i], mBlockPaint);
            }
        }
    }
}
