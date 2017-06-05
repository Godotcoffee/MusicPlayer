package com.goodjob.musicplayer.entity;

/**
 * Created by Godot on 2017/6/5.
 */

public class AudioListItem {
    public static final int DEFAULT = 0;
    public static final int PLAYING = 1;
    public static final int PAUSING = 2;
    private Audio mAudio;
    private int mPlayStatus;

    public AudioListItem(Audio audio) {
        mAudio = audio;
        mPlayStatus = DEFAULT;
    }

    public void setPlayStatus(int playStatus) {
        mPlayStatus = playStatus;
    }

    public int getPlayStatus() {
        return mPlayStatus;
    }

    public Audio getAudio() {
        return mAudio;
    }
}
