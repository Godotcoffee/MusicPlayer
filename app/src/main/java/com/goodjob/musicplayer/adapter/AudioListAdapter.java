package com.goodjob.musicplayer.adapter;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.List;

/**
 * Created by Godot on 2017/6/1.
 */

public class AudioListAdapter extends ArrayAdapter<Audio> {
    private Context mContext;
    private int mResource;
    private LayoutInflater mInflater;
    public AudioListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Audio> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mInflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView artist = (TextView) convertView.findViewById(R.id.artist);
        TextView duration = (TextView) convertView.findViewById(R.id.duration);
        ImageView album = (ImageView) convertView.findViewById(R.id.album);

        Audio audio = getItem(position);

        title.setText(audio.getTitle());
        artist.setText(audio.getArtist());
        BitmapDrawable drawable = MediaUtils.getAlbumBitmapDrawable(mContext, audio);
        if (drawable != null) {
            album.setImageDrawable(drawable);
        } else {
            album.setImageResource(R.drawable.no_album);
        }

        int totalSecond = audio.getDuration() / 1000;
        int minute = totalSecond / 60;
        int second = totalSecond % 60;

        duration.setText(String.format("%02d:%02d", minute, second));

        return convertView;
    }

}
