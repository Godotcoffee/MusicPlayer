package com.goodjob.musicplayer.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.adapter.AudioListAdapter;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        List<Audio> audioList = new ArrayList<>();
        for (Audio audio : MediaUtils.getAudioList(this)) {
            if (!audio.isMusic())
            Log.d("musiclist", "title: " + audio.getTitle() + ", isMusic: " + audio.isMusic()
                    + ", isAlarm: " + audio.isAlarm() + ", isNotification: " + audio.isNotification() + ", isRingtone: " + audio.isRingtone());
            if (audio.isMusic() && audio.getDuration() > 40 * 1000) {
                audioList.add(audio);
            }
        }

        final ListView listView = (ListView) findViewById(R.id.list_view);
        ArrayAdapter adapter = new AudioListAdapter(this, R.layout.list_music, audioList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Audio audio = (Audio) listView.getAdapter().getItem(position);
                Intent intent = new Intent(ListActivity.this, PlayerActivity.class);
                intent.putExtra("audio", audio);
                startActivity(intent);
            }
        });
    }
}
