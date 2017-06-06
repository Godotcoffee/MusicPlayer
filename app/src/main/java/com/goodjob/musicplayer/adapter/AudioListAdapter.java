package com.goodjob.musicplayer.adapter;

import android.content.Context;
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
import com.goodjob.musicplayer.entity.AudioListItem;
import com.goodjob.musicplayer.util.AsyncBitmapLoader;

import java.util.List;

/**
 * Created by Godot on 2017/6/1.
 */

public class AudioListAdapter extends ArrayAdapter<AudioListItem> {
    private Context mContext;
    private int mResource;
    private LayoutInflater mInflater;
    private AsyncBitmapLoader mAsyncBitmapLoader;

    private static class ViewHolder {
        TextView title;
        TextView artist;
        TextView duration;
        //ImageView album;
        ImageView status;
    }

    public AudioListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<AudioListItem> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mInflater = LayoutInflater.from(mContext);
        mAsyncBitmapLoader = new AsyncBitmapLoader(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.artist = (TextView) convertView.findViewById(R.id.artist);
            viewHolder.duration = (TextView) convertView.findViewById(R.id.duration);
            //viewHolder.album = (ImageView) convertView.findViewById(R.id.album);
            viewHolder.status = (ImageView) convertView.findViewById(R.id.status);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        AudioListItem audioItem = getItem(position);
        Audio audio = audioItem.getAudio();

        viewHolder.title.setText(audio.getTitle());
        viewHolder.artist.setText(audio.getArtist());

        int totalSecond = audio.getDuration() / 1000;
        int minute = totalSecond / 60;
        int second = totalSecond % 60;

        viewHolder.duration.setText(String.format("%02d:%02d", minute, second));

        //viewHolder.album.setTag(audio.getAlbumId());
        //mAsyncBitmapLoader.load(viewHolder.album, audio.getAlbumId(), R.drawable.no_album);

        return convertView;
    }

}
