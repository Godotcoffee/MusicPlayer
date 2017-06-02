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
import com.goodjob.musicplayer.util.AudioList;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        List<Audio> audioList = AudioList.getAudioList(this);

        final ListView listView = (ListView) findViewById(R.id.list_view);
        ArrayAdapter adapter = new AudioListAdapter(this, R.layout.list_music, audioList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ListActivity.this, PlayerActivity.class);
                intent.putExtra("audioPosition", position);
                startActivity(intent);
            }
        });
    }
}
