package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.service.AudioPlayService;

public class PlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);

        BroadcastReceiver broadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int current = intent.getIntExtra("current", 0);
                int total = intent.getIntExtra("total", 1);
                int min = 0, max = seekBar.getMax();
                int pos = 0;
                if (total != 0 && (max - min) != 0) {
                    pos = (int) ((current * 1.0 / total) * (max - min));
                }

                seekBar.setProgress(pos);
            }
        };

        Audio audio = (Audio) getIntent().getSerializableExtra("audio");
        if (audio == null) {
            return;
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadReceiver, new IntentFilter(AudioPlayService.BROADCAST_SENDER_FILTER));
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra("path", audio.getPath());
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, AudioPlayService.class));
    }


}
