package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.AudioList;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.List;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private SeekBar seekBar;
    private TextView currentTextView;
    private TextView durationTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private ImageView albumImageView;

    private List<Audio> audioList;

    private int position;

    private Object mLock = new Object();

    private void playMusic(int position) {
        if (position < 0 || position > audioList.size()) {
            return;
        }
        synchronized (mLock) {
            Audio audio = audioList.get(position);
            int totalSecond = audio.getDuration() / 1000;
            int minute = totalSecond / 60;
            int second = totalSecond % 60;

            currentTextView.setText("00:00");
            durationTextView.setText(String.format("%02d:%02d", minute, second));
            seekBar.setProgress(0);
            titleTextView.setText(audio.getTitle());
            artistTextView.setText(audio.getArtist());
            BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(this, audio);
            if (drawable != null) {
                albumImageView.setImageDrawable(drawable);
            } else {
                albumImageView.setImageResource(R.drawable.no_album);
            }

            Intent intent = new Intent(this, AudioPlayService.class);
            intent.putExtra("action", "play");
            intent.putExtra("path", audio.getPath());
            intent.putExtra("title", audio.getTitle());
            intent.putExtra("artist", audio.getArtist());
            startService(intent);
        }
    }

    private void pauseMusic() {
        synchronized (mLock) {
            Intent intent = new Intent(this, AudioPlayService.class);
            intent.putExtra("action", "pause");
            startService(intent);
        }
    }

    private void stopMusic() {
        synchronized (mLock) {
            Intent intent = new Intent(this, AudioPlayService.class);
            intent.putExtra("action", "stop");
            startService(intent);
            seekBar.setProgress(0);
            currentTextView.setText("00:00");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.next).setOnClickListener(this);
        findViewById(R.id.previous).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTextView = (TextView) findViewById(R.id.current);
        durationTextView = (TextView) findViewById(R.id.duration);
        titleTextView = (TextView) findViewById(R.id.title);
        artistTextView = (TextView) findViewById(R.id.artist);
        albumImageView = (ImageView) findViewById(R.id.album);

        audioList = AudioList.getAudioList(this);

        BroadcastReceiver playingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (mLock) {
                    if (intent.getBooleanExtra("isPlaying", false)) {
                        int current = intent.getIntExtra("current", 0);
                        int total = intent.getIntExtra("total", 1);
                        int min = 0, max = seekBar.getMax();
                        int pos = 0;
                        if (total != 0 && (max - min) != 0) {
                            pos = (int) ((current * 1.0 / total) * (max - min));
                        }
                        seekBar.setProgress(pos);
                        int totalTime = current / 1000;
                        int minute = totalTime / 60;
                        int second = totalTime % 60;
                        currentTextView.setText(String.format("%02d:%02d", minute, second));
                    }
                }
            }
        };

        BroadcastReceiver eventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra("event");
                switch (event) {
                    case "finished":
                        Log.d("eventReceiver", "finished");
                        playMusic(position = (position + 1) % audioList.size());
                        break;
                }
            }
        };
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(playingReceiver, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));
        lbm.registerReceiver(eventReceiver, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));
        playMusic(position = getIntent().getIntExtra("audioPosition", -1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(this, AudioPlayService.class));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pause:
                pauseMusic();
                break;
            case R.id.next:
                playMusic(position = (position + 1) % audioList.size());
                break;
            case R.id.previous:
                stopMusic();
                break;
        }
    }
}
