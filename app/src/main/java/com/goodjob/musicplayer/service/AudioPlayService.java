package com.goodjob.musicplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.activity.ListActivity;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.ArrayList;

public class AudioPlayService extends Service {
    /** Service发送当前播放状态广播的ACTION FILTER */
    public static final String BROADCAST_PLAYING_FILTER = "AUDIO_PLAYER_PLAYING";

    /** Service发送音乐播放事件广播的ACTION FILTER */
    public static final String BROADCAST_EVENT_FILTER = "AUDIO_PLAYER_EVENT";

    public static final String BROADCAST_VISUALIZER_FILTER = "AUDIO_PLAYER_VISUALIZER";

    /** ACTION KEY */
    public static final String ACTION_KEY = "action";

    /** EVENT KEY */
    public static final String EVENT_KEY = "event";

    /** 开始播放动作 */
    public static final String PLAY_ACTION = "play";

    /** 暂停动作 */
    public static final String PAUSE_ACTION = "pause";

    /** 继续播放动作 */
    public static final String REPLAY_ACTION = "replay";

    /** 停止播放动作 */
    public static final String STOP_ACTION = "stop";

    /** 唤出播放器动作 */
    public static final String ACTIVITY_ACTION = "activity";

    /** 切换下一首 */
    public static final String NEXT_ACTION = "next";

    /** 切换上一首 */
    public static final String PREVIOUS_ACTION = "previous";

    /** 调整进度 */
    public static final String SEEK_ACTION = "seek";

    /** 改变播放顺序 */
    public static final String CHANGE_LIST_SHUFFLE_ACTION = "list_shuffle";

    /** 改变循环方式 */
    public static final String CHANGE_LOOP_ACTION = "loop_way";

    /** 开始播放事件 */
    public static final String PLAY_EVENT = "play_event";

    /** 播放完成事件 */
    public static final String FINISHED_EVENT = "finished";

    /** 下一首事件 */
    public static final String NEXT_EVENT = "next_event";

    /** 上一首事件 */
    public static final String PREVIOUS_EVENT = "previous_event";

    /** 暂停事件 */
    public static final String PAUSE_EVENT = "pause_event";

    /** 继续事件 */
    public static final String REPLAY_EVENT = "replay_event";

    /** 列表播放顺序改变 */
    public static final String LIST_ORDER_EVENT = "list_order_event";

    /** 开启列表顺序播放 */
    public static final String CHANGE_LOOP_EVENT = "change_loop_event";

    /** 音频标题属性 */
    public static final String AUDIO_TITLE_STR = "title";

    /** 音频演唱者属性 */
    public static final String AUDIO_ARTIST_STR = "artist";

    /** 音频总时长属性 */
    public static final String AUDIO_DURATION_INT = "duration";

    /** 音频当前时长属性 */
    public static final String AUDIO_CURRENT_INT = "current";

    /** 音频专辑ID属性 */
    public static final String AUDIO_ALBUM_ID_INT = "albumId";

    /** 音频是否正在播放属性 */
    public static final String AUDIO_IS_PLAYING_BOOL = "isPlaying";

    /** 音频路径属性 */
    public static final String AUDIO_PATH_STR = "path";

    /** 音频是否立即播放属性 */
    public static final String AUDIO_PLAY_NOW_BOOL = "playNow";

    /** 音频调节位置 */
    public static final String AUDIO_SEEK_POS_INT = "seekPos";

    /** 列表顺序 */
    public static final String LIST_SHUFFLE_BOOL = "list_is_order";

    /** 循环方式 */
    public static final String LOOP_WAY_INT = "loop_way";

    public static final int LIST_NOT_LOOP = 0;
    public static final int LIST_LOOP = 1;
    public static final int AUDIO_REPEAT = 2;

    /** 频谱列表 */
    public static final String VISUALIZER_INT_LIST = "visualizer_list";

    /***/
    public static final String  VISUALIZER_SAMPLE_RATE_INT = "visualizer_sample";

    /** Notification的ID */
    private static final int NOTIFICATION_ID = 1;

    /** MediaPlayer的同步锁 */
    private Object mLock = new Object();

    /** 音乐播放对象 */
    private MediaPlayer mMediaPlayer;

    /** 频谱分析对象 */
    private Visualizer mVisualizer;
    private Equalizer mEqualizer;

    /** 是否有音乐在播放中 */
    private boolean mIsPlay;

    /** 播放中的音乐是否暂停 */
    private boolean mIsPause;

    /** 路径 */
    private String mPath;

    /** 信息通知管理 */
    //private NotificationManager mNotificationManager;
    /** 当前播放的歌曲的标题 */
    private String mAudioTitle = "";
    /** 当前播放的歌曲的歌手 */
    private String mAudioArtist = "";
    /** 当前播放的专辑id */
    private int mAudioAlbumId;

    /** 获得包含Audio信息的Intent */
    private Intent getAudioIntent() {
        Intent intent = new Intent(BROADCAST_PLAYING_FILTER);
        int current = 0, duration = 1;
        boolean isPlaying = false;
        if (mMediaPlayer != null) {
            synchronized (mLock) {
                current = mMediaPlayer.getCurrentPosition();
                duration = mMediaPlayer.getDuration();
                isPlaying = mMediaPlayer.isPlaying();
            }
        }
        intent.putExtra(AUDIO_PATH_STR, mPath);
        intent.putExtra(AUDIO_CURRENT_INT, current);
        intent.putExtra(AUDIO_DURATION_INT, duration);
        intent.putExtra(AUDIO_IS_PLAYING_BOOL, isPlaying);
        intent.putExtra(AUDIO_TITLE_STR, mAudioTitle);
        intent.putExtra(AUDIO_ARTIST_STR, mAudioArtist);
        intent.putExtra(AUDIO_ALBUM_ID_INT, mAudioAlbumId);
        return intent;
    }

    // 用于广播当前播放状态
    private Runnable playingRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer == null) {
                Log.e("player-service-thread", "null");
                return;
            }
            try {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                while (mThreadContinue) {
                    Intent intent = getAudioIntent();
                    intent.setAction(BROADCAST_PLAYING_FILTER);
                    lbm.sendBroadcast(intent);
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                Log.d("player-service-thread", "interrupted");
            }
            Log.d("player-service-thread", "end");
        }
    };

    /** 播放中的歌曲状态广播线程 */
    private Thread mThread;
    private boolean mThreadContinue;

    private void openPlayerActivity() {
        Intent intent = getAudioIntent();
        intent.setClass(this, ListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void sendAudioEvent(String event, Bundle bundle) {
        Intent intent = new Intent(BROADCAST_EVENT_FILTER);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.putExtra(EVENT_KEY, event);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public AudioPlayService() {
    }

    @Override
    public void onCreate() {
        Log.d("player-service", "create");
        mMediaPlayer = new MediaPlayer();
        Log.d("id", mMediaPlayer.getAudioSessionId() + "");
        int audioId = mMediaPlayer.getAudioSessionId();
        mEqualizer = new Equalizer(0, audioId);
        mEqualizer.setEnabled(true);

        mVisualizer = new Visualizer(audioId);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (!mIsPlay || mIsPause) {
                    return;
                }
                Intent intent = new Intent(BROADCAST_VISUALIZER_FILTER);
                ArrayList<Integer> list = new ArrayList<>(fft.length);
                for (int i = 0; i < fft.length; ++i) {
                    list.add((int) fft[i]);
                }
                intent.putIntegerArrayListExtra(VISUALIZER_INT_LIST, list);
                intent.putExtra(VISUALIZER_SAMPLE_RATE_INT, samplingRate);
                LocalBroadcastManager.getInstance(AudioPlayService.this).sendBroadcast(intent);
            }
        }, mVisualizer.getMaxCaptureRate(), true, true);
        mVisualizer.setEnabled(true);

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //mVisualizer.setEnabled(false);
                sendAudioEvent(FINISHED_EVENT, null);
            }
        });
        mThreadContinue = true;
        mIsPlay = false;
        mIsPause = false;
        mThread = new Thread(playingRunnable);
        mThread.start();

        //mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra(ACTION_KEY);
        if (action == null) {
            return START_NOT_STICKY;
        }
        switch (action) {
            // 播放
            case PLAY_ACTION:
                // 播放路径
                mPath = intent.getStringExtra(AUDIO_PATH_STR);
                // 标题
                mAudioTitle = intent.getStringExtra(AUDIO_TITLE_STR);
                // 歌手
                mAudioArtist = intent.getStringExtra(AUDIO_ARTIST_STR);
                // 专辑id
                mAudioAlbumId = intent.getIntExtra(AUDIO_ALBUM_ID_INT, 0);
                // 是否播放
                boolean playNow = intent.getBooleanExtra(AUDIO_PLAY_NOW_BOOL, true);
                try {
                    synchronized (mLock) {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(mPath);
                        mMediaPlayer.prepare();
                        if (playNow) {
                            mMediaPlayer.start();
                        }
                        mIsPause = !playNow;
                    }

                    //if (!mVisualizer.getEnabled()) {
                    //mVisualizer.setEnabled(true);
                    //}
                    mIsPlay = true;
                    Log.d("player-service", "start");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent notificationIntent = new Intent(this, AudioPlayService.class);
                notificationIntent.putExtra(ACTION_KEY, ACTIVITY_ACTION);
                PendingIntent pendingIntent = PendingIntent.getService(
                        getApplicationContext(), 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                Bitmap bitmap = MediaUtils.getAlbumBitmapDrawable(mPath);

                Notification notification = builder
                        .setSmallIcon(R.drawable.ic_player_notification)
                        .setLargeIcon(bitmap != null ? bitmap : BitmapFactory.decodeResource(getResources(), R.drawable.ic_player_big))
                        .setContentTitle(mAudioTitle)
                        .setContentText(mAudioArtist)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent).build();
                //notification.flags = Notification.FLAG_ONGOING_EVENT;
                startForeground(NOTIFICATION_ID, notification);
                //mNotificationManager.notify(NOTIFICATION_ID, notification);
                Bundle bundle = new Bundle();
                bundle.putBoolean(AudioPlayService.AUDIO_PLAY_NOW_BOOL, playNow);
                sendAudioEvent(PLAY_EVENT, bundle);

                break;
            // 暂停
            case PAUSE_ACTION:
                if (mIsPlay) {
                    if (!mIsPause) {
                        synchronized (mLock) {
                            mMediaPlayer.pause();
                        }
                        //mVisualizer.setEnabled(false);
                        mIsPause = true;
                    }
                    sendAudioEvent(PAUSE_EVENT, null);
                }
                Log.d("player-service", "pause");
                break;
            // 继续播放
            case REPLAY_ACTION:
                if (mIsPlay) {
                    if (mIsPause) {
                        synchronized (mLock) {
                            mMediaPlayer.start();
                        }
                        //mVisualizer.setEnabled(true);
                        mIsPause = false;
                    }
                    sendAudioEvent(REPLAY_EVENT, null);
                }
                break;
            // 停止播放
            case STOP_ACTION:
                stopForeground(true);
                mIsPlay = false;
                mIsPause = false;
                synchronized (mLock) {
                    mMediaPlayer.stop();
                }
                //mVisualizer.setEnabled(false);
                break;
            // 唤出播放器页面
            case ACTIVITY_ACTION:
                openPlayerActivity();
                break;
            // 下一首
            case NEXT_ACTION:
                sendAudioEvent(NEXT_EVENT, null);
                break;
            // 上一首
            case PREVIOUS_ACTION:
                sendAudioEvent(PREVIOUS_EVENT, null);
                break;
            // 进度调整
            case SEEK_ACTION:
                int pos = intent.getIntExtra(AUDIO_SEEK_POS_INT, 0);
                mMediaPlayer.seekTo(pos);
                break;
            // 切换播放顺序
            case CHANGE_LIST_SHUFFLE_ACTION:
                sendAudioEvent(LIST_ORDER_EVENT, intent.getExtras());
                break;
            // 切换循环方式
            case CHANGE_LOOP_ACTION:
                sendAudioEvent(CHANGE_LOOP_EVENT, intent.getExtras());
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mIsPlay = false;
        mThreadContinue = false;
        if (mMediaPlayer != null) {
            synchronized (mLock) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
        }
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
        }
        if (mEqualizer != null) {
            mEqualizer.release();
        }
        Log.d("player-service", "destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
