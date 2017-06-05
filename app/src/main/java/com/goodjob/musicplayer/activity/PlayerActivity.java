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

    private Object mLock = new Object();

    private String mTitle;
    private String mArtist;
    private int mAlbumId;

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

        mTitle = getIntent().getStringExtra("title");
        mArtist = getIntent().getStringExtra("artist");
        mAlbumId = getIntent().getIntExtra("albumId", -1);
        int current = getIntent().getIntExtra("current", 0);
        int duration = getIntent().getIntExtra("duration", 0);

        int min = 0, max = seekBar.getMax();
        int pos = 0;
        if (duration != 0 && (max - min) != 0) {
            pos = (int) ((current * 1.0 / duration) * (max - min));
        }
        seekBar.setProgress(pos);

        titleTextView.setText(mTitle);
        artistTextView.setText(mArtist);

        BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(PlayerActivity.this, mAlbumId);
        if (drawable != null) {
            albumImageView.setImageDrawable(drawable);
        } else {
            albumImageView.setImageResource(R.drawable.no_album);
        }

        BroadcastReceiver playingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (mLock) {
                    if (intent.getBooleanExtra("isPlaying", false)) {
                        int current = intent.getIntExtra("current", 0);
                        int duration = intent.getIntExtra("duration", 1);
                        int min = 0, max = seekBar.getMax();
                        int pos = 0;
                        if (duration != 0 && (max - min) != 0) {
                            pos = (int) ((current * 1.0 / duration) * (max - min));
                        }
                        seekBar.setProgress(pos);
                        int totalSecond = current / 1000;
                        int minute = totalSecond / 60;
                        int second = totalSecond % 60;
                        currentTextView.setText(String.format("%02d:%02d", minute, second));
                        totalSecond = duration / 1000;
                        minute = totalSecond / 60;
                        second = totalSecond % 60;
                        durationTextView.setText(String.format("%02d:%02d", minute, second));

                        String title = intent.getStringExtra("title");
                        String artist = intent.getStringExtra("artist");
                        int albumId = intent.getIntExtra("albumId", -1);

                        if (title != null && !title.equals(mTitle)) {
                            titleTextView.setText(title);
                            mTitle = title;
                        }
                        if (artist != null && !artist.equals(mArtist)) {
                            artistTextView.setText(artist);
                            mArtist = artist;
                        }
                        if (albumId != mAlbumId) {
                            BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(PlayerActivity.this, albumId);
                            if (drawable != null) {
                                albumImageView.setImageDrawable(drawable);
                            } else {
                                albumImageView.setImageResource(R.drawable.no_album);
                            }
                            mAlbumId = albumId;
                        }
                    }
                }
            }
        };

        BroadcastReceiver eventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra("event");
                switch (event) {
                }
            }
        };
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(playingReceiver, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));
        lbm.registerReceiver(eventReceiver, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(this, AudioPlayService.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        int current = intent.getIntExtra("current", 0);
        int duration = intent.getIntExtra("duration", 0);

        int min = 0, max = seekBar.getMax();
        int pos = 0;
        if (duration != 0 && (max - min) != 0) {
            pos = (int) ((current * 1.0 / duration) * (max - min));
        }
        seekBar.setProgress(pos);

        titleTextView.setText(title);
        artistTextView.setText(artist);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pause:
                //pauseMusic();
                break;
            case R.id.next:
                //playMusic(position = (position + 1) % audioList.size());
                break;
            case R.id.previous:
                //stopMusic();
                break;
        }
    }
}
