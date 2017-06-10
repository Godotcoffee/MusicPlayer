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
import android.util.Log;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static String[] permissionArray = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    private ListView listView;
    private ArrayAdapter adapter;

    private TextView mBarTitle;
    private TextView mBarArtist;
    private ImageView mBarAlbum;

    private BroadcastReceiver mEventReceiver;

    private List<AudioListItem> audioItemList;
    private List<Integer> mShuffleIndex;

    private int mLastPlay = -1;
    private int mLastIndex = -1;

    private boolean mIsPlaying = false;
    private boolean mIsShuffle = false;

    private int mLoopWay;

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

    private void playAudio(int position) {
        playAudio(position, true, false, false);
    }

    /**
     *
     * @param position 在原始音乐列表的位置
     * @param shuffle  是否再次打乱顺序
     */
    private void playAudio(int position, boolean start, boolean shuffle, boolean forced) {
        if (forced || position != mLastPlay) {
            AudioListItem item = audioItemList.get(position);
            Audio audio = item.getAudio();

            if (shuffle) {
                shuffleAudioIndex(mShuffleIndex, position);
                mLastIndex = 0;
            }

            Intent serviceIntent = getAudioIntent(audio);
            serviceIntent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PLAY_ACTION);
            serviceIntent.putExtra(AudioPlayService.AUDIO_PLAY_NOW_BOOL, start);
            serviceIntent.setClass(this, AudioPlayService.class);

            item.setPlayStatus(AudioListItem.PLAYING);

            if (mLastPlay != -1 && position != mLastPlay) {
                ((AudioListItem) listView.getItemAtPosition(mLastPlay)).setPlayStatus(AudioListItem.DEFAULT);
            }
            mLastPlay = position;

            mBarTitle.setText(audio.getTitle());

            mBarArtist.setText(audio.getArtist());

            mBarAlbum.setImageDrawable(MediaUtils.getAlbumBitmapDrawable(this, audio));

            startService(serviceIntent);
            mIsPlaying = start;
        }
    }

    /**
     * 把indexList乱序后将值=playIndex的项交换到开头
     * @param indexList
     * @param playIndex
     */
    private void shuffleAudioIndex(List<Integer> indexList, int playIndex) {
        Collections.shuffle(indexList);
        for (int i = 0; i < indexList.size(); ++i) {
            if (indexList.get(i) == playIndex) {
                Collections.swap(indexList, i, 0);
                break;
            }
        }
        for (int i = 0; i < mShuffleIndex.size(); ++i) {
            Log.d("list", mShuffleIndex.get(i) + "");
        }
    }

    /**
     * 歌曲切换
     * @param next      是否为下一首
     * @param fromUser  是否来自用户的动作
     */
    private void musicChange(boolean next, boolean fromUser) {
        if (mLoopWay == AudioPlayService.AUDIO_REPEAT && !fromUser) {
            playAudio(mLastPlay, true, false, true);
        } else {
            int index;
            if (mIsShuffle) {
                if (next) {
                    index = mShuffleIndex.get(mLastIndex = (mLastIndex + 1) % mShuffleIndex.size());
                } else {
                    index = mShuffleIndex.get(
                            mLastIndex = (mLastIndex - 1 + mShuffleIndex.size()) % mShuffleIndex.size());
                }
                mLastIndex = index;
            } else {
                if (next) {
                    index = (mLastPlay + 1) % audioItemList.size();
                } else {
                    index = (mLastPlay - 1 + audioItemList.size()) % audioItemList.size();
                }
            }
            if (index == 0 && next && !fromUser && mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
                index = -1;
            }
            if (index >= 0) {
                playAudio(index, mIsPlaying, mIsShuffle, true);
            } else {
                playAudio(0, false, mIsShuffle, true);
                mIsPlaying = false;
            }
        }
    }

    /**
     * 初始化列表
     */
    private void init() {
        List<Audio> audioList = AudioList.getAudioList(this);
        audioItemList = new ArrayList<>();
        mShuffleIndex = new ArrayList<>();
        int index = 0;
        for (Audio audio : audioList) {
            audioItemList.add(new AudioListItem(audio));
            mShuffleIndex.add(index++);
        }

        listView = (ListView) findViewById(R.id.list_view);
        adapter = new AudioListAdapter(this, R.layout.list_music, audioItemList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("click", "abc");
                playAudio(position, true, true, false);
                Audio audio = audioItemList.get(position).getAudio();
                Intent activityIntent = new Intent();
                //startActivity(activityIntent);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void pause() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLoopWay = AudioPlayService.LIST_NOT_LOOP;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        View barView = findViewById(R.id.bar);
        mBarTitle = (TextView) barView.findViewById(R.id.title);
        mBarArtist = (TextView) barView.findViewById(R.id.artist);
        mBarAlbum = (ImageView) barView.findViewById(R.id.album);

        mBarTitle.setHorizontallyScrolling(true);
        mBarTitle.setSelected(true);
        mBarArtist.setHorizontallyScrolling(true);
        mBarArtist.setSelected(true);

        barView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastPlay >= 0 && mLastPlay < audioItemList.size()) {
                    Intent intent = getAudioIntent(audioItemList.get(mLastPlay).getAudio());
                    intent.setClass(ListActivity.this, PlayerActivity.class);
                    intent.putExtra(AudioPlayService.AUDIO_IS_PLAYING_BOOL, mIsPlaying);
                    Log.d("bool", mIsPlaying + "");
                    intent.putExtra(AudioPlayService.LIST_ORDER_BOOL, mIsShuffle);
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
                        musicChange(true, false);
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.NEXT_EVENT:
                        musicChange(true, true);
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.PREVIOUS_EVENT:
                        musicChange(false, true);
                        adapter.notifyDataSetChanged();
                        break;
                    case AudioPlayService.PAUSE_EVENT:
                        mIsPlaying = false;
                        break;
                    case AudioPlayService.REPLAY_EVENT:
                        mIsPlaying = true;
                        break;
                    case AudioPlayService.LIST_ORDER_EVENT:
                        mIsShuffle = intent.getBooleanExtra(AudioPlayService.LIST_ORDER_BOOL, true);
                        if (mIsShuffle && mLastPlay != -1) {
                            shuffleAudioIndex(mShuffleIndex, mLastPlay);
                            mLastIndex = 0;
                        }
                        break;
                    case AudioPlayService.CHANEG_LOOP_EVENT:
                        mLoopWay = intent.getIntExtra(
                                AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
                        break;
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));

        List<String> requestList = new ArrayList<>();

        for (String permission : permissionArray) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PermissionChecker.PERMISSION_GRANTED) {
                requestList.add(permission);
            }
        }

        if (requestList.size() > 0) {
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[] {}),
                    PERMISSION_REQUEST_CODE);
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
            case PERMISSION_REQUEST_CODE:
                boolean good = true;
                for (int i = 0; i < permissions.length; ++i) {
                    if (grantResults[i] != PermissionChecker.PERMISSION_GRANTED) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ListActivity.this);
                        builder.setTitle("提示").setMessage("不允许读取SD卡权限则无法正常使用哦")
                                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ListActivity.super.finish();
                                    }
                                }).show();
                        good = false;
                        break;
                    }
                }
                if (good) {
                    init();
                }
                break;
        }
    }

    @Override
    public void finish() {
        moveTaskToBack(false);
    }
}
