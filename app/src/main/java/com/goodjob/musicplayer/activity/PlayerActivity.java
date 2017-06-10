package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.music.MusicCoverView;
import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.MediaUtils;
import com.goodjob.musicplayer.view.VisualizerView;

import java.io.File;
import java.util.ArrayList;

import me.zhengken.lyricview.LyricView;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private SeekBar seekBar;
    private TextView currentTextView;
    private TextView durationTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private MusicCoverView albumImageView;
    private VisualizerView visualizerView;
    private FrameLayout frameLayout;
    private ImageButton pauseButton;
    private LyricView lyricView;

    private Object mLock = new Object();

    private String mTitle;
    private String mArtist;
    private int mDuration;
    private int mAlbumId = -1;

    private boolean onDrag = false;
    private boolean mIsShuffle;
    private boolean mIsPlay;

    private int mLoopWay;

    private boolean mIsAlbum;

    private boolean mIsLastRunning;

    private BroadcastReceiver mPlayingReceiver;
    private BroadcastReceiver mVisualizerReceiver;

    private long mStartTime;

    /**
     * UI更新
     * @param bundle 包含音乐信息的bundle
     */
    private void updateUI(Bundle bundle, boolean isFirst) {
        synchronized (mLock) {
            String title = bundle.getString(AudioPlayService.AUDIO_TITLE_STR);
            String artist = bundle.getString(AudioPlayService.AUDIO_ARTIST_STR);
            int albumId = bundle.getInt(AudioPlayService.AUDIO_ALBUM_ID_INT, -1);
            mIsPlay = bundle.getBoolean(AudioPlayService.AUDIO_IS_PLAYING_BOOL, false);
            if (isFirst) {
                mIsShuffle = bundle.getBoolean(AudioPlayService.LIST_ORDER_BOOL, false);
                mLoopWay = bundle.getInt(AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
                if (mIsShuffle) {
                    Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT).show();
                }
            }

            if (mTitle == null || !mTitle.equals(title)) {
                titleTextView.setText(mTitle = title);
            }
            if (mArtist == null || !mArtist.equals(artist)) {
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

            if (mIsPlay != albumImageView.isRunning()) {
               if (mIsPlay) {
                   albumImageView.start();
               } else {
                   albumImageView.stop();
               }
            }
            int duration = bundle.getInt(AudioPlayService.AUDIO_DURATION_INT, 0);
            int current = Math.min(bundle.getInt(AudioPlayService.AUDIO_CURRENT_INT, 0), duration);

            if (!onDrag) {
                int min = 0, max = seekBar.getMax();
                int pos = 0;
                if (duration != 0 && (max - min) != 0) {
                    pos = (int) ((current * 1.0 / duration) * (max - min));
                }
                seekBar.setProgress(pos);
                lyricView.setCurrentTimeMillis(current);
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
        //Log.d("isplay", mIsPlay + "");
        if (mIsPlay) {
            intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PAUSE_ACTION);
            mIsPlay = false;
        } else {
            intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.REPLAY_ACTION);
            mIsPlay = true;
        }
        if (albumImageView.isRunning()) {
            if (mIsAlbum) {
                //albumImageView.stop();
            }
        } else {
            //albumImageView.start();
        }
        startService(intent);
    }

    private void stopMusic() {

    }

    /**
     * 切换播放顺序
     */
    private void changeListOrder() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LIST_SHUFFLE_ACTION);
        intent.putExtra(AudioPlayService.LIST_ORDER_BOOL, mIsShuffle = !mIsShuffle);
        startService(intent);
        Toast.makeText(this, "切换到" + (mIsShuffle ? "随机播放" : "顺序播放"), Toast.LENGTH_SHORT).show();
    }

    public void changeLoopWay() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LOOP_ACTION);
        if (mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
            mLoopWay = AudioPlayService.LIST_LOOP;
        } else if (mLoopWay == AudioPlayService.LIST_LOOP) {
            mLoopWay = AudioPlayService.AUDIO_REPEAT;
        } else {
            mLoopWay = AudioPlayService.LIST_NOT_LOOP;
        }
        intent.putExtra(AudioPlayService.LOOP_WAY_INT, mLoopWay);
        startService(intent);
        Toast.makeText(this, "切换到" + (mLoopWay == AudioPlayService.LIST_NOT_LOOP ?
                "顺序播放" : (mLoopWay == AudioPlayService.LIST_LOOP ?
                "循环播放" : "单曲循环")), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);
        findViewById(R.id.shuffleButton).setOnClickListener(this);
        findViewById(R.id.repeatButton).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTextView = (TextView) findViewById(R.id.current);
        durationTextView = (TextView) findViewById(R.id.duration);
        titleTextView = (TextView) findViewById(R.id.title);
        artistTextView = (TextView) findViewById(R.id.artist);
        frameLayout = (FrameLayout) findViewById(R.id.album);
        pauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        lyricView = (LyricView) findViewById(R.id.lyric_view);

        titleTextView.setHorizontallyScrolling(true);
        titleTextView.setSelected(true);
        artistTextView.setHorizontallyScrolling(true);
        artistTextView.setSelected(true);

        pauseButton.setOnClickListener(this);

        visualizerView = new VisualizerView(this);

        mIsAlbum = true;
        albumImageView = new MusicCoverView(this);
        albumImageView.setShape(MusicCoverView.SHAPE_CIRCLE);
        frameLayout.addView(albumImageView);

        //专辑封面旋转
        //albumImageView.start();

        File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
        File saveFile = new File(sdCardDir, "GARNiDELiA.lrc");
        lyricView.setLyricFile(saveFile);

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

        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameLayout.removeAllViews();
                if (mIsAlbum) {
                    mIsLastRunning = albumImageView.isRunning();
                    frameLayout.addView(visualizerView);
                } else{
                    frameLayout.addView(albumImageView);
                    if (mIsPlay != mIsLastRunning) {
                        if (mIsPlay) {
                            //albumImageView.start();
                        } else {
                            //albumImageView.stop();
                        }
                    }
                }
                mIsAlbum = !mIsAlbum;
            }
        });

        updateUI(getIntent().getExtras(), true);

        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent.getExtras(), false);
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));

        mStartTime = System.currentTimeMillis();
        LocalBroadcastManager.getInstance(this).registerReceiver(mVisualizerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ArrayList<Integer> list = intent.getIntegerArrayListExtra(AudioPlayService.VISUALIZER_INT_LIST);
                long end = System.currentTimeMillis();
                if (end - mStartTime >= 10) {
                    visualizerView.updateData(list, intent.getIntExtra(AudioPlayService.VISUALIZER_SAMPLE_RATE_INT, 0));
                    mStartTime = System.currentTimeMillis();
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_VISUALIZER_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayingReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVisualizerReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUI(intent.getExtras(), false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton:
                pauseMusic();
                break;
            case R.id.nextButton:
                nextMusic();
                break;
            case R.id.previousButton:
                previousMusic();
                break;
            case R.id.shuffleButton:
                changeListOrder();
                break;
            case R.id.repeatButton:
                changeLoopWay();
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
