package com.goodjob.musicplayer.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.adapter.AudioListAdapter;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.entity.AudioListItem;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.AudioList;
import com.goodjob.musicplayer.util.MediaUtils;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 1;

    public static final String LIST_RANDOM_ORDER_BOOL = "list_random";

    private int mLastPlay = -1;
    private ListView listView;
    private ArrayAdapter adapter;

    private TextView mBarTitle;
    private TextView mBarArtist;
    private ImageView mBarAlbum;

    private BroadcastReceiver mEventReceiver;

    List<AudioListItem> audioItemList;

    private boolean mIsPlaying = false;

    private void init() {
        List<Audio> audioList = AudioList.getAudioList(this);
        audioItemList = new ArrayList<>();
        for (Audio audio : audioList) {
            audioItemList.add(new AudioListItem(audio));
        }

        listView = (ListView) findViewById(R.id.list_view);
        adapter = new AudioListAdapter(this, R.layout.list_music, audioItemList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                play(position);
                Audio audio = audioItemList.get(position).getAudio();
                Intent activityIntent = getAudioIntent(audio);
                activityIntent.setClass(ListActivity.this, PlayerActivity.class);
                //startActivity(activityIntent);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private Intent getAudioIntent(Audio audio) {
        Intent intent = new Intent();
        intent.putExtra(AudioPlayService.AUDIO_PATH_STR, audio.getPath());
        intent.putExtra(AudioPlayService.AUDIO_TITLE_STR, audio.getTitle());
        intent.putExtra(AudioPlayService.AUDIO_ARTIST_STR, audio.getArtist());
        intent.putExtra(AudioPlayService.AUDIO_ALBUM_ID_INT, audio.getAlbumId());
        intent.putExtra(AudioPlayService.AUDIO_DURATION_INT, audio.getDuration());
        intent.putExtra(AudioPlayService.AUDIO_CURRENT_INT, 0);
        return intent;
    }

    private void play(int position) {
        AudioListItem item = audioItemList.get(position);
        if (position != mLastPlay) {
            Audio audio = item.getAudio();
            Intent serviceIntent = getAudioIntent(audio);
            serviceIntent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PLAY_ACTION);
            serviceIntent.setClass(this, AudioPlayService.class);

            item.setPlayStatus(AudioListItem.PLAYING);

            if (mLastPlay != -1) {
                ((AudioListItem) listView.getItemAtPosition(mLastPlay)).setPlayStatus(AudioListItem.DEFAULT);
            }
            mLastPlay = position;

            mBarTitle.setText(audio.getTitle());
            mBarArtist.setText(audio.getArtist());
            mBarAlbum.setImageDrawable(MediaUtils.getAlbumBitmapDrawable(this, audio));

            startService(serviceIntent);
            mIsPlaying = true;
        }
    }

    private void pause() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        View barView = findViewById(R.id.bar);
        mBarTitle = (TextView) barView.findViewById(R.id.title);
        mBarArtist = (TextView) barView.findViewById(R.id.artist);
        mBarAlbum = (ImageView) barView.findViewById(R.id.album);

        barView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastPlay >= 0 && mLastPlay < audioItemList.size()) {
                    Intent intent = getAudioIntent(audioItemList.get(mLastPlay).getAudio());
                    intent.setClass(ListActivity.this, PlayerActivity.class);
                    intent.putExtra(AudioPlayService.AUDIO_IS_PLAYING_BOOL, mIsPlaying);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mEventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(AudioPlayService.EVENT_KEY);
                switch (event) {
                    case AudioPlayService.FINISHED_EVENT:
                        play((mLastPlay + 1) % audioItemList.size());
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.NEXT_EVENT:
                        play((mLastPlay + 1) % audioItemList.size());
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.PREVIOUS_EVENT:
                        play((mLastPlay + audioItemList.size() - 1) % audioItemList.size());
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.PAUSE_ACTION:
                        mIsPlaying = false;
                        break;
                    case AudioPlayService.REPLAY_ACTION:
                        mIsPlaying = true;
                        break;
                }
                Log.d("event", "get");
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        } else {
            init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, AudioPlayService.class));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mEventReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                    init();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ListActivity.this);
                    builder.setTitle("提示").setMessage("不允许读取SD卡权限则无法正常使用哦")
                            .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ListActivity.super.finish();
                                }
                            }).show();
                }
                break;
        }
    }

    @Override
    public void finish() {
        moveTaskToBack(false);
    }
}
