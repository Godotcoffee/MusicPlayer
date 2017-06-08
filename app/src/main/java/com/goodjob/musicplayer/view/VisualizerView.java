package com.goodjob.musicplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import java.util.List;

/**
 * Created by Godot on 2017/6/8.
 */

public class VisualizerView extends View {
    private List<Integer> mFFT;
    private Rect mRect;

    public VisualizerView(Context context) {
        super(context);
        mRect = new Rect();
    }

    public void updateData(List<Integer> fft) {
        mFFT = fft;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFFT == null) {
            return;
        }

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        mRect.set(0, 0, getWidth(), getHeight());

        float width = mRect.width() * 1.0f / mFFT.size();
        for (int i = 0; i < mFFT.size(); ++i) {
            Log.d("rect", 10 * Math.log10(mFFT.get(i)) + "");
            double val = 10 * Math.log10(mFFT.get(i));
            float left = i * width;
            float right = i * width + width;
            float bottom = mRect.bottom;
            float top = mRect.height() - (byte) (val * mRect.height() / 10);

            canvas.drawRect(left, top, right ,bottom, paint);
        }
    }
}
