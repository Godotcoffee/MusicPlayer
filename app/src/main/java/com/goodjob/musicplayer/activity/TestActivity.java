package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.view.VisualizerView;

import java.util.ArrayList;

public class TestActivity extends AppCompatActivity {
    private BroadcastReceiver mReceiver;

    private long start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_layout);

        final VisualizerView view = new VisualizerView(this);
        FrameLayout layout = (FrameLayout) findViewById(R.id.frame);
        layout.addView(view);
        start = System.currentTimeMillis();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ArrayList<Integer> list = intent.getIntegerArrayListExtra("test");
                long end = System.currentTimeMillis();
                if (end - start >= 15) {
                    view.updateData(list, intent.getIntExtra("rate", 0));
                    start = System.currentTimeMillis();
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_VISUALIZER_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }
}
