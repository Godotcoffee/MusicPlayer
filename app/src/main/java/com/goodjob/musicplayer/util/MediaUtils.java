package com.goodjob.musicplayer.util;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.goodjob.musicplayer.entity.Audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Godot on 2017/6/1.
 */

public class MediaUtils {
    public static List<Audio> getAudioList(Context context) {
        List<Audio> list = new ArrayList<>();

        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();

        // 获得内部存储的音频
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Bundle bundle = new Bundle();
            for (int i = 0; i < cursor.getColumnCount(); ++i) {
                int type = cursor.getType(i);
                String colName = cursor.getColumnName(i);
                switch (type) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        bundle.putInt(colName, cursor.getInt(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        bundle.putString(colName, cursor.getString(i));
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
            for (int i = 0; i < cursor.getColumnCount(); ++i) {
                int type = cursor.getType(i);
                String colName = cursor.getColumnName(i);
                switch (type) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        bundle.putInt(colName, cursor.getInt(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        bundle.putString(colName, cursor.getString(i));
                        break;
                }
            }
            list.add(new Audio(bundle));
        }
        cursor.close();

        return list;
    }

    public static String getAlbumArt(Context context, int albumId) {
        String albumArt = null;
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[] {albumId + ""},
                null);
        if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
            cursor.moveToFirst();
            albumArt = cursor.getString(0);
        }
        cursor.close();

        if (albumArt != null) {
            return albumArt;
        }

        cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[] {albumId + ""},
                null);
        if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
            cursor.moveToFirst();
            albumArt = cursor.getString(0);
        }
        cursor.close();

        return albumArt;
    }

    public static Bitmap getAlbumBitmapDrawable(Audio audio) {
        if (audio == null) {
            return null;
        }
        return getAlbumBitmapDrawable(audio.getPath());
    }

    public static Bitmap getAlbumBitmapDrawable(String path) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(path);

        byte[] art = mediaMetadataRetriever.getEmbeddedPicture();

        return art != null ? BitmapFactory.decodeByteArray(art, 0, art.length) : null;
    }
}
