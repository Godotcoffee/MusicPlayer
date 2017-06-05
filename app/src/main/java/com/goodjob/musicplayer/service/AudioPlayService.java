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
import com.goodjob.musicplayer.activity.PlayerActivity;

import java.io.IOException;

public class AudioPlayService extends Service {
    /** Service发送当前播放状态广播的ACTION FILTER */
    public static final String BROADCAST_PLAYING_FILTER = "AUDIO_PLAYER_PLAYING";

    /** Service发送音乐播放事件广播的ACTION FILTER */
    public static final String BROADCAST_EVENT_FILTER = "AUDIO_PLAYER_EVENT";

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
    private NotificationManager mNotificationManager;
    /** 当前播放的歌曲的标题 */
    private String mAudioTitle = "";
    /** 当前播放的歌曲的歌手 */
    private String mAudioArtist = "";

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
                int current, duration;
                boolean isPlaying;
                while (mThreadContinue) {
                    /**
                     * Intent携带的数据格式：
                     * key - type - description
                     * current int 当前歌曲所在的进度
                     * duration int 当前歌曲总长度
                     * isPlaying boolean 当前是否有歌曲在播放（为了避免错误更新UI）
                     */
                    Intent intent = new Intent(BROADCAST_PLAYING_FILTER);
                    synchronized (mLock) {
                        current = mMediaPlayer.getCurrentPosition();
                        duration = mMediaPlayer.getDuration();
                        isPlaying = mMediaPlayer.isPlaying();
                    }
                    intent.putExtra("current", current);
                    intent.putExtra("total", duration);
                    intent.putExtra("isPlaying", isPlaying);
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

    public AudioPlayService() {
    }

    @Override
    public void onCreate() {
        Log.d("player-service", "create");
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                synchronized (mLock) {
                    Intent intent = new Intent(BROADCAST_EVENT_FILTER);
                    intent.putExtra("event", "finished");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
            }
        });
        mThreadContinue = true;
        mIsPlay = false;
        mIsPause = false;
        if (mThread == null) {
            mThread = new Thread(playingRunnable);
            mThread.start();
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
        String action = intent.getStringExtra("action");
        switch (action) {
            // 播放
            case "play":
                // 播放路径
                String path = intent.getStringExtra("path");
                // 标题
                mAudioTitle = intent.getStringExtra("title");
                // 歌手
                mAudioArtist = intent.getStringExtra("artist");
                // 是否播放
                boolean playNow = intent.getBooleanExtra("playNow", true);
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
                Intent notificationIntent = new Intent(this, PlayerActivity.class);
                notificationIntent.putExtra("title", mAudioTitle);
                notificationIntent.putExtra("artist", mAudioArtist);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        getApplicationContext(), 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                Notification notification = builder
                        .setSmallIcon(R.drawable.ic_player_notification)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_player_big))
                        .setContentTitle(mAudioTitle)
                        .setContentText(mAudioArtist)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent).build();
                //notification.flags = Notification.FLAG_ONGOING_EVENT;
                startForeground(NOTIFICATION_ID, notification);
                //mNotificationManager.notify(NOTIFICATION_ID, notification);
                break;
            // 切换暂停状态
            case "pause":
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
                break;
            // 停止播放
            case "stop":
                stopForeground(true);
                mIsPlay = false;
                mIsPause = false;
                synchronized (mLock) {
                    mMediaPlayer.stop();
                }
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mIsPlay = false;
        mThreadContinue = false;
        synchronized (mLock) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        Log.d("player-service", "destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
