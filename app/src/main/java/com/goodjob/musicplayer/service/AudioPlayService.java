package com.goodjob.musicplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

public class AudioPlayService extends Service {
    /** Service发送广播的ACTION FILTER */
    public static final String BROADCAST_PLAYING_FILTER = "AUDIO_PLAYER_PLAYING";
    public static final String BROADCAST_FINISHED_FILTER = "AUDIO_PLAYER_FINISHED";

    private Object mLock = new Object();

    private MediaPlayer mediaPlayer;

    private boolean isPlay;
    private boolean isPause;

    // 用于广播当前播放状态
    private Runnable playingRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer == null) {
                return;
            }
            try {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                while (true) {
                    Intent intent = new Intent(BROADCAST_PLAYING_FILTER);
                    intent.putExtra("current", mediaPlayer.getCurrentPosition());
                    intent.putExtra("total", mediaPlayer.getDuration());
                    lbm.sendBroadcast(intent);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Log.d("player-service-thread", "interrupted");
            }
            Log.d("player-service-thread", "end");
        }
    };
    private Thread thread;

    public AudioPlayService() {
    }

    @Override
    public void onCreate() {
        Log.d("player-service", "create");
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        isPlay = false;
        isPause = false;
        if (thread == null) {
            thread = new Thread(playingRunnable);
            thread.start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mLock) {
            String action = intent.getStringExtra("action");
            switch (action) {
                // 开始播放
                case "play":
                    // 播放路径
                    String path = intent.getStringExtra("path");
                    // 停止上一次的播放
                    //if (mediaPlayer.isPlaying()) {
                    //    mediaPlayer.stop();
                    //}
                    mediaPlayer.reset();
                    try {
                        mediaPlayer.setDataSource(path);
                        mediaPlayer.prepare();
                        if (!isPause)
                            mediaPlayer.start();
                        isPlay = true;
                        Log.d("player-service", "start");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    break;
                // 切换暂停状态
                case "pause":
                    if (isPlay) {
                        if (isPause) {
                            mediaPlayer.start();
                            isPause = false;
                        } else {
                            mediaPlayer.pause();
                            isPause = true;
                        }
                    }
                    break;
                // 停止播放
                case "stop":
                    isPlay = false;
                    isPause = false;

                    mediaPlayer.stop();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        isPlay = false;
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
            thread = null;
        }
        mediaPlayer.stop();
        mediaPlayer.release();
        Log.d("player-service", "destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
