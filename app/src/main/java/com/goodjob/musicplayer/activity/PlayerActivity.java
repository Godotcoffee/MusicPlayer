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
import android.widget.ImageView;
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
    private ImageButton nextButton;
    private ImageButton previousButton;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;
    private ImageButton returnButton;
    private LyricView lyricView;
    private View pauseButtonBackground;

    private Object mLock = new Object();

    private String mTitle;
    private String mArtist;
    private int mDuration;
    private int mAlbumId = -1;
    private String mPath;

    private boolean onDrag = false;
    private boolean mIsShuffle;
    private boolean mIsPlay;

    private int mLoopWay;

    private boolean mIsAlbum;

    private boolean mIsLastRunning;

    private BroadcastReceiver mPlayingReceiver;
    private BroadcastReceiver mVisualizerReceiver;
    private BroadcastReceiver mPlayEventReceiver;

    private long mStartTime;

    /**
     * UI更新
     * @param bundle 包含音乐信息的bundle
     */
    private void updateUI(Bundle bundle) {
        synchronized (mLock) {
            String title = bundle.getString(AudioPlayService.AUDIO_TITLE_STR);
            String artist = bundle.getString(AudioPlayService.AUDIO_ARTIST_STR);
            String path = bundle.getString(AudioPlayService.AUDIO_PATH_STR);
            int albumId = bundle.getInt(AudioPlayService.AUDIO_ALBUM_ID_INT, -1);

            if (mTitle == null || !mTitle.equals(title)) {
                titleTextView.setText(mTitle = title);
            }
            if (mArtist == null || !mArtist.equals(artist)) {
                artistTextView.setText(mArtist = artist);
            }
            if (mAlbumId != albumId) {
                mAlbumId = albumId;
                BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(this, mAlbumId);
                Log.d("pic", drawable + "");
                if (drawable != null) {
                    albumImageView.setImageDrawable(drawable);
                } else {
                    albumImageView.setImageResource(R.drawable.no_album);
                }
            }

            if (mPath == null || !mPath.equals(path)) {
                loadLyrics(getLyricsPath(mPath = path));
            }

            // 特殊处理，停止旋转需要时间
            boolean isPlay = bundle.getBoolean(AudioPlayService.AUDIO_IS_PLAYING_BOOL, false);
            if (isPlay != albumImageView.isRunning()) {
               if (isPlay) {
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
            //mIsPlay = false;
            //pauseButton.setImageResource(R.drawable.pause_light);
        } else {
            intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.REPLAY_ACTION);
            //mIsPlay = true;
            //pauseButton.setImageResource(R.drawable.play_light);
        }

        enableButton(false, false);
        startService(intent);
    }

    /**
     * 转换音乐路径为歌词路径
     * @param musicPath
     * @return
     */
    private String getLyricsPath(String musicPath) {
        if (musicPath == null) {
            return null;
        }
        return musicPath.replaceAll(".[^.]*$", ".lrc");
    }

    /**
     * 根据路径载入歌词
     * @param lyricsPath
     */
    private void loadLyrics(String lyricsPath) {
        File saveFile = new File(lyricsPath);
        lyricView.setLyricFile(saveFile);
    }

    /**
     * 切换播放顺序
     */
    private void changeListOrder() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LIST_SHUFFLE_ACTION);
        intent.putExtra(AudioPlayService.LIST_ORDER_BOOL, mIsShuffle = !mIsShuffle);
        startService(intent);
        //Toast.makeText(this, "切换到" + (mIsShuffle ? "随机播放" : "顺序播放"), Toast.LENGTH_SHORT).show();
        if (mIsShuffle) {
            shuffleButton.setImageResource(R.drawable.btn_playback_shuffle_all);
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }
    }

    public void changeLoopWay() {
        Intent intent = new Intent(this, AudioPlayService.class);
        intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.CHANGE_LOOP_ACTION);
        if (mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
            mLoopWay = AudioPlayService.LIST_LOOP;
            repeatButton.setImageResource(R.drawable.btn_playback_repeat_all);
        } else if (mLoopWay == AudioPlayService.LIST_LOOP) {
            mLoopWay = AudioPlayService.AUDIO_REPEAT;
            repeatButton.setImageResource(R.drawable.btn_playback_repeat_one);
        } else {
            mLoopWay = AudioPlayService.LIST_NOT_LOOP;
            repeatButton.setImageResource(R.drawable.repeat);
        }
        intent.putExtra(AudioPlayService.LOOP_WAY_INT, mLoopWay);
        startService(intent);
    }

    /** 切换按钮的可用状态 */
    public void enableButton(boolean enable) {
        enableButton(enable, false);
    }
    public void enableButton(boolean enable, boolean grey) {
        pauseButton.setEnabled(enable);
        pauseButtonBackground.setEnabled(enable);
        nextButton.setEnabled(enable);
        previousButton.setEnabled(enable);

        if (grey && !enable) {
            pauseButtonBackground.setBackgroundResource(R.drawable.shadowed_circle_grey);
        } else {
            pauseButtonBackground.setBackgroundResource(R.drawable.shadowed_circle_red);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mIsShuffle = getIntent().getBooleanExtra(AudioPlayService.LIST_ORDER_BOOL, false);
        mLoopWay = getIntent().getIntExtra(AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
        mPath = getIntent().getStringExtra(AudioPlayService.AUDIO_PATH_STR);
        mIsPlay = getIntent().getBooleanExtra(AudioPlayService.AUDIO_IS_PLAYING_BOOL, false);

        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTextView = (TextView) findViewById(R.id.current);
        durationTextView = (TextView) findViewById(R.id.duration);
        titleTextView = (TextView) findViewById(R.id.title);
        artistTextView = (TextView) findViewById(R.id.artist);
        frameLayout = (FrameLayout) findViewById(R.id.album);
        pauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        nextButton = (ImageButton) findViewById(R.id.nextButton);
        previousButton = (ImageButton) findViewById(R.id.previousButton);
        repeatButton = (ImageButton) findViewById(R.id.repeatButton);
        shuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
        returnButton = (ImageButton) findViewById(R.id.returnButton);
        lyricView = (LyricView) findViewById(R.id.lyric_view);
        pauseButtonBackground = findViewById(R.id.playPauseButtonBackground);

        titleTextView.setHorizontallyScrolling(true);
        titleTextView.setSelected(true);
        artistTextView.setHorizontallyScrolling(true);
        artistTextView.setSelected(true);

        pauseButton.setOnClickListener(this);
        pauseButtonBackground.setOnClickListener(this);
        repeatButton.setOnClickListener(this);
        shuffleButton.setOnClickListener(this);
        returnButton.setOnClickListener(this);
        visualizerView = new VisualizerView(this);

        mIsAlbum = true;
        albumImageView = new MusicCoverView(this);
        albumImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumImageView.setCallbacks(new MusicCoverView.Callbacks() {
            @Override
            public void onMorphEnd(MusicCoverView coverView) {
            }

            @Override
            public void onRotateEnd(MusicCoverView coverView) {
                enableButton(true, true);
            }
        });
        albumImageView.setShape(MusicCoverView.SHAPE_CIRCLE);
        frameLayout.addView(albumImageView);

        // 载入歌词
        loadLyrics(getLyricsPath(mPath));
        //专辑封面旋转
        if (mIsPlay) {
            albumImageView.start();
        } else {
            pauseButton.setImageResource(R.drawable.play_light);
        }


        // 设置图标
        if (mIsShuffle) {
            shuffleButton.setImageResource(R.drawable.btn_playback_shuffle_all);
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }

        if (mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
            repeatButton.setImageResource(R.drawable.repeat);
        } else if (mLoopWay == AudioPlayService.LIST_LOOP) {
            repeatButton.setImageResource(R.drawable.btn_playback_repeat_all);
        } else {
            repeatButton.setImageResource(R.drawable.btn_playback_repeat_one);
        }

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
                    if (mIsPlay != albumImageView.isRunning()) {
                        if (mIsPlay) {
                            albumImageView.start();
                        } else {
                            albumImageView.stop();
                        }
                    }
                }
                mIsAlbum = !mIsAlbum;
            }
        });

        updateUI(getIntent().getExtras());

        // 更新UI广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent.getExtras());
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_PLAYING_FILTER));

        // 频谱广播
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

        // 事件广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mPlayEventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(AudioPlayService.EVENT_KEY);
                if (event == null) {
                    return;
                }
                switch (event) {
                    case AudioPlayService.PAUSE_EVENT:
                        synchronized (mLock) {
                            pauseButton.setImageResource(R.drawable.play_light);
                            albumImageView.stop();
                            enableButton(true);

                        }
                        mIsPlay = false;
                        break;
                    case AudioPlayService.REPLAY_EVENT:
                        synchronized (mLock) {
                            pauseButton.setImageResource(R.drawable.pause_light);
                            albumImageView.start();
                            enableButton(true);
                        }
                        mIsPlay = true;
                        break;
                    case AudioPlayService.PLAY_EVENT:
                        synchronized (mLock) {
                            boolean isPlay = intent.getBooleanExtra(AudioPlayService.AUDIO_PLAY_NOW_BOOL, false);
                            Log.d("plat", isPlay + "");
                            if (isPlay) {
                                pauseButton.setImageResource(R.drawable.pause_light);
                                albumImageView.start();
                                enableButton(true);
                                mIsPlay = true;
                            } else {
                                pauseButton.setImageResource(R.drawable.play_light);
                                if (albumImageView.isRunning()) {
                                    if (mIsAlbum) {
                                        enableButton(false, true);
                                        albumImageView.stop();
                                    } else {
                                        enableButton(true);
                                    }
                                } else {
                                    enableButton(true);
                                }
                                mIsPlay = false;
                            }
                        }
                        break;
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayingReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVisualizerReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayEventReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUI(intent.getExtras());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton: case R.id.playPauseButtonBackground:
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
            case R.id.returnButton:
                finish();
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
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
