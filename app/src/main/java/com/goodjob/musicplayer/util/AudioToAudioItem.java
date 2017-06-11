package com.goodjob.musicplayer.util;

import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.entity.AudioItem;

/**
 * Created by Godot on 2017/6/11.
 */

public interface AudioToAudioItem {
    AudioItem apply(Audio audio);
}
