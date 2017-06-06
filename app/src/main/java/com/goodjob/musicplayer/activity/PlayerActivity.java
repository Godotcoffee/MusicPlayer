package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.entity.Audio;
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
    private int mDuration;
    private int mAlbumId = -1;

    private boolean onDrag = false;

    private BroadcastReceiver mPlayingReceiver;

    /**
     * UI更新
     * @param bundle 包含音乐信息的bundle
     */
    private void updateUI(Bundle bundle) {
        synchronized (mLock) {
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
                BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(this, mAlbumId);
                if (drawable != null) {
                    albumImageView.setImageDrawable(drawable);
                } else {
                    albumImageView.setImageResource(R.drawable.no_album);
                }
            }

            int current = bundle.getInt(AudioPlayService.AUDIO_CURRENT_INT, 0);
            int duration = bundle.getInt(AudioPlayService.AUDIO_DURATION_INT, 0);

            if (!onDrag) {
                int min = 0, max = seekBar.getMax();
                int pos = 0;
                if (duration != 0 && (max - min) != 0) {
                    pos = (int) ((current * 1.0 / duration) * (max - min));
                }
                seekBar.setProgress(pos);
            }

            int totalSecond = current / 1000;
            int minute = totalSecond / 60;
            int second = totalSecond % 60;
            if (!onDrag) {
                currentTextView.setText(String.format("%02d:%02d", minute, second));
            }
            if (mDuration != duration) {
                totalSecond = (mDuration = duration) / 1000;
                minute = totalSecond / 60;
                second = totalSecond % 60;
                durationTextView.setText(String.format("%02d:%02d", minute, second));
            }

            titleTextView.setText(mTitle);
            artistTextView.setText(mArtist);

            BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(this, mAlbumId);
            if (drawable != null) {
                albumImageView.setImageDrawable(drawable);
            } else {
                albumImageView.setImageResource(R.drawable.no_album);
            }
        }
    }

    // 下一首
    private void nextMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.NEXT_ACTION);
        startService(intent);
    }

    // 上一首
    private void previousMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PREVIOUS_ACTION);
        startService(intent);
    }

    // 切换暂停
    private void pauseMusic() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PAUSE_ACTION);
        startService(intent);
    }

    private void stopMusic() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        findViewById(R.id.playPauseButtonBackground).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTextView = (TextView) findViewById(R.id.current);
        durationTextView = (TextView) findViewById(R.id.duration);
        titleTextView = (TextView) findViewById(R.id.title);
        artistTextView = (TextView) findViewById(R.id.artist);
        albumImageView = (ImageView) findViewById(R.id.album);

        // 进度条事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int min = 0, max = seekBar.getMax();
                    int changedCurrent = (int) (mDuration * 1.0 / (max - min) * progress);
                    int totalSecond = changedCurrent / 1000;
                    int minute = totalSecond / 60;
                    int second = totalSecond % 60;
                    currentTextView.setText(String.format("%02d:%02d", minute, second));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                synchronized (mLock) {
                    onDrag = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int min = 0, max = seekBar.getMax();
                int changedCurrent = (int) (mDuration * 1.0 / (max - min) * seekBar.getProgress());
                Intent intent = new Intent(PlayerActivity.this, AudioPlayService.class);
                intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.SEEK_ACTION);
                intent.putExtra(AudioPlayService.AUDIO_SEEK_POS_INT, changedCurrent);
                startService(intent);
                synchronized (mLock) {
                    onDrag = false;
                }
            }
        });

        updateUI(getIntent().getExtras());

        mPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent.getExtras());
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayingReceiver,
                new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayingReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUI(intent.getExtras());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButtonBackground:
                pauseMusic();
                Log.d("pause", "push");
                break;
            case R.id.nextButton:
                nextMusic();
                break;
            case R.id.previousButton:
                previousMusic();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
