package com.goodjob.musicplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.goodjob.musicplayer.entity.Audio;

import java.io.IOException;

public class AudioPlayService extends Service {
    /** Service发送广播的ACTION FILTER */
    public static final String BROADCAST_SENDER_FILTER = "AUDIO_PLAYER_SENDER";
    /** Service接收广播的ACTION FILTER */
    public static final String BROADCAST_RECEIVER_FILTER = "AUDIO_PLAYER_RECEIVER";

    private MediaPlayer mediaPlayer;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer == null) {
                return;
            }
            try {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                while (mediaPlayer.isPlaying()) {
                    Intent intent = new Intent(BROADCAST_SENDER_FILTER);
                    intent.putExtra("current", mediaPlayer.getCurrentPosition());
                    intent.putExtra("total", mediaPlayer.getDuration());
                    lbm.sendBroadcast(intent);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {

            }
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = intent.getStringExtra("path");
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("player-service", "start");
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }
        thread = new Thread(runnable);
        thread.start();
        Log.d("sender", "ok");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
            thread = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        Log.d("player-service", "destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
