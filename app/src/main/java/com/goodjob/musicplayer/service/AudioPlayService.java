package com.goodjob.musicplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.activity.ListActivity;
import com.goodjob.musicplayer.activity.PlayerActivity;

import java.io.IOException;

public class AudioPlayService extends Service {
    /** Service发送当前播放状态广播的ACTION FILTER */
    public static final String BROADCAST_PLAYING_FILTER = "AUDIO_PLAYER_PLAYING";

    /** Service发送音乐播放事件广播的ACTION FILTER */
    public static final String BROADCAST_EVENT_FILTER = "AUDIO_PLAYER_EVENT";

    /** ACTION KEY */
    public static final String ACTION_KEY = "action";

    /** EVENT KEY */
    public static final String EVENT_KEY = "event";

    /** 开始播放动作 */
    public static final String PLAY_ACTION = "play";

    /** 切换暂停动作 */
    public static final String PAUSE_ACTION = "pause";

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

    /** 播放完成事件 */
    public static final String FINISHED_EVENT = "finished";

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

    /** Notification的ID */
    private static final int NOTIFICATION_ID = 1;

    /** MediaPlayer的同步锁 */
    private Object mLock = new Object();

    /** 音乐播放 */
    private MediaPlayer mMediaPlayer;

    /** 是否有音乐在播放中 */
    private boolean mIsPlay;

    /** 播放中的音乐是否暂停 */
    private boolean mIsPause;

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
                    Thread.sleep(500);
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

    private void changeAudio(String action) {
        Intent intent = new Intent(BROADCAST_EVENT_FILTER);
        intent.putExtra(EVENT_KEY, action);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public AudioPlayService() {
    }

    @Override
    public void onCreate() {
        Log.d("player-service", "create");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                synchronized (mLock) {
                    Intent intent = new Intent(BROADCAST_EVENT_FILTER);
                    intent.putExtra(EVENT_KEY, FINISHED_EVENT);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
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
        /**
         * Intent所接收的格式
         * key - type - expected - description - extra
         * action String play 播放一首新的本地歌曲 path 歌曲的路径
         *                                       title 歌曲的名称（用于状态栏显示）
         *                                       artist 歌曲的演唱者（用于状态栏显示）
         *                                       playNow 是否立刻播放
         *               pause 如果有播放的歌曲，切换暂停和播放
         *               stop 停止播放
         */
        String action = intent.getStringExtra(ACTION_KEY);

        switch (action) {
            // 播放
            case PLAY_ACTION:
                // 播放路径
                String path = intent.getStringExtra(AUDIO_PATH_STR);
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
                        mMediaPlayer.setDataSource(path);
                        mMediaPlayer.prepare();
                        if (playNow)
                            mMediaPlayer.start();
                    }
                    mIsPlay = true;
                    Log.d("player-service", "start");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                Intent notificationIntent = new Intent(this, AudioPlayService.class);
                notificationIntent.putExtra(ACTION_KEY, ACTIVITY_ACTION);
                PendingIntent pendingIntent = PendingIntent.getService(
                        getApplicationContext(), 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                Notification notification = builder
                        .setSmallIcon(R.drawable.ic_player_notification)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_player_big))
                        .setContentTitle(mAudioTitle)
                        .setContentText(mAudioArtist)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent).build();
                //notification.flags = Notification.FLAG_ONGOING_EVENT;
                startForeground(NOTIFICATION_ID, notification);
                //mNotificationManager.notify(NOTIFICATION_ID, notification);
                break;
            // 切换暂停状态
            case PAUSE_ACTION:
                if (mIsPlay) {
                    if (mIsPause) {
                        synchronized (mLock) {
                            mMediaPlayer.start();
                        }
                        mIsPause = false;
                    } else {
                        synchronized (mLock) {
                            mMediaPlayer.pause();
                        }
                        mIsPause = true;
                    }
                }
                Log.d("player-service", "pause");
                break;
            // 停止播放
            case STOP_ACTION:
                stopForeground(true);
                mIsPlay = false;
                mIsPause = false;
                synchronized (mLock) {
                    mMediaPlayer.stop();
                }
                break;
            // 唤出播放器页面
            case ACTIVITY_ACTION:
                openPlayerActivity();
                break;
            // 下一首
            case NEXT_ACTION:
                changeAudio(NEXT_ACTION);
                break;
            // 上一首
            case PREVIOUS_ACTION:
                changeAudio(PREVIOUS_ACTION);
                break;
            // 进度调整
            case SEEK_ACTION:
                int pos = intent.getIntExtra(AUDIO_SEEK_POS_INT, 0);
                mMediaPlayer.seekTo(pos);
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
        Log.d("player-service", "destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
