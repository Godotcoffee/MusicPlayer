package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.MediaUtils;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private SeekBar seekBar;
    private TextView currentTextView;
    private TextView durationTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private ImageView albumImageView;

    private Object mLock = new Object();

    private String mTitle;
    private String mArtist;
    private int mAlbumId = -1;

    /**
     * UI更新
     * @param bundle 包含音乐信息的bundle
     */
    private void updateUI(Bundle bundle) {
        synchronized (mLock) {
            setContentView(R.layout.activity_player);

            findViewById(R.id.playPauseButton).setOnClickListener(this);
            findViewById(R.id.nextButton).setOnClickListener(this);
            findViewById(R.id.previousButton).setOnClickListener(this);

            seekBar = (SeekBar) findViewById(R.id.seekBar);
            currentTextView = (TextView) findViewById(R.id.current);
            durationTextView = (TextView) findViewById(R.id.duration);
            titleTextView = (TextView) findViewById(R.id.title);
            artistTextView = (TextView) findViewById(R.id.artist);
            albumImageView = (ImageView) findViewById(R.id.album);

            String title = bundle.getString(AudioPlayService.AUDIO_TITLE_STR);
            String artist = bundle.getString(AudioPlayService.AUDIO_ARTIST_STR);
            int albumId = bundle.getInt(AudioPlayService.AUDIO_ALBUM_ID_INT, -1);

            if (mTitle == null || !mTitle.equals(title)) {
                titleTextView.setText(mTitle = title);
            }
            if (mArtist == null || !mTitle.equals(artist)) {
                artistTextView.setText(mArtist = artist);
            }
            if (mAlbumId != albumId) {
                mAlbumId = albumId;
                BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(PlayerActivity.this, mAlbumId);
                if (drawable != null) {
                    albumImageView.setImageDrawable(drawable);
                } else {
                    albumImageView.setImageResource(R.drawable.no_album);
                }
            }

            int current = bundle.getInt(AudioPlayService.AUDIO_CURRENT_INT, 0);
            int duration = bundle.getInt(AudioPlayService.AUDIO_DURATION_INT, 0);

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

            titleTextView.setText(mTitle);
            artistTextView.setText(mArtist);

            BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(PlayerActivity.this, mAlbumId);
            if (drawable != null) {
                albumImageView.setImageDrawable(drawable);
            } else {
                albumImageView.setImageResource(R.drawable.no_album);
            }
        }
    }

    private void pauseMusic() {

    }

    private void stopMusic() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        findViewById(R.id.playPauseButton).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTextView = (TextView) findViewById(R.id.current);
        durationTextView = (TextView) findViewById(R.id.duration);
        titleTextView = (TextView) findViewById(R.id.title);
        artistTextView = (TextView) findViewById(R.id.artist);
        albumImageView = (ImageView) findViewById(R.id.album);

        updateUI(getIntent().getExtras());

        BroadcastReceiver playingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent.getExtras());
            }
        };

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(playingReceiver, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(this, AudioPlayService.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUI(intent.getExtras());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton:
                //pauseMusic();
                break;
            case R.id.nextButton:
                //playMusic(position = (position + 1) % audioList.size());
                break;
            case R.id.previousButton:
                //stopMusic();
                break;
        }
    }
}
