package com.goodjob.musicplayer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import com.goodjob.musicplayer.entity.Audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Godot on 2017/6/1.
 */

public class MediaUtils {
    public static List<Audio> getAudioList(Context context) {
        List<Audio> list = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();

        // 获得内部存储的音频
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Bundle bundle = new Bundle();
            for (String column : cursor.getColumnNames()) {
                int colIdx = cursor.getColumnIndex(column);
                int type = cursor.getType(colIdx);
                switch (type) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        bundle.putInt(column, cursor.getInt(colIdx));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        bundle.putString(column, cursor.getString(colIdx));
                        break;
                }
            }
            list.add(new Audio(bundle));
        }
        cursor.close();

        // 获得外部存储的音频
        cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Bundle bundle = new Bundle();
            for (String column : cursor.getColumnNames()) {
                int colIdx = cursor.getColumnIndex(column);
                int type = cursor.getType(colIdx);
                switch (type) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        bundle.putInt(column, cursor.getInt(colIdx));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        bundle.putString(column, cursor.getString(colIdx));
                        break;
                }
            }
            list.add(new Audio(bundle));
        }
        cursor.close();

        return list;
    }
}
