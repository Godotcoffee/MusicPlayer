package com.goodjob.musicplayer.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.adapter.AudioListAdapter;
import com.goodjob.musicplayer.adapter.ViewPagerAdapter;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.entity.AudioItem;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.AudioList;
import com.goodjob.musicplayer.util.AudioToAudioItem;
import com.goodjob.musicplayer.util.MediaUtils;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String SHARED_PREFERENCES_NAME = "music_player";

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static String[] permissionArray = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    private List<BaseAdapter> mAdapterList;

    private TextView mBarTitle;
    private TextView mBarArtist;
    private ImageView mBarAlbum;
    private ImageButton mBarPauseButton;
    private ImageButton mBarNextButton;
    private View mBarPauseBackground;
    private PagerTitleStrip mPaperTitleStrip;

    private BroadcastReceiver mEventReceiver;

    private List<List<AudioItem>> mListOfAudioItemList;
    private int mPlayingIndex = -1;

    private List<Integer> mShuffleIndex;

    private int mLastPlay = -1;
    private int mLastIndex = -1;

    private boolean mIsPlaying = false;
    private boolean mIsShuffle = false;

    private int mLoopWay;

    private static String getPinyinString(String str) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch);
            if (pinyin != null) {
                builder.append(pinyin[0]);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private void saveStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(AudioPlayService.LOOP_WAY_INT, mLoopWay);
        editor.putBoolean(AudioPlayService.LIST_SHUFFLE_BOOL, mIsShuffle);
        editor.apply();
    }

    private void readStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mLoopWay = sharedPreferences.getInt(AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
        mIsShuffle = sharedPreferences.getBoolean(AudioPlayService.LIST_SHUFFLE_BOOL, false);
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
            List<AudioItem> list = mListOfAudioItemList.get(mPlayingIndex);
            AudioItem audioItem = list.get(position);
            Audio audio = audioItem.getAudio();

            if (shuffle) {
                shuffleAudioIndex(list, position);
                mLastIndex = 0;
            }

            Intent serviceIntent = getAudioIntent(audio);
            serviceIntent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PLAY_ACTION);
            serviceIntent.putExtra(AudioPlayService.AUDIO_PLAY_NOW_BOOL, start);
            serviceIntent.setClass(this, AudioPlayService.class);

            mLastPlay = position;

            mBarTitle.setText(audio.getTitle());

            mBarArtist.setText(audio.getArtist());

            Bitmap bitmap = MediaUtils.getAlbumBitmapDrawable(audio);
            if (bitmap != null) {
                mBarAlbum.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            } else {
                mBarAlbum.setImageResource(R.drawable.no_album);
            }
            enableButton(false);
            startService(serviceIntent);
        }
    }

    /**
     * 把indexList乱序后将值=playIndex的项交换到开头
     * @param playIndex
     */
    private void shuffleAudioIndex(List<? extends Object> audioList, int playIndex) {
        if (mShuffleIndex == null) {
            mShuffleIndex = new ArrayList<>();
        }
        if (mShuffleIndex.size() != audioList.size()) {
            mShuffleIndex.clear();
            for (int i = 0; i < audioList.size(); ++i) {
                mShuffleIndex.add(i);
            }
        }
        Collections.shuffle(mShuffleIndex);
        for (int i = 0; i < mShuffleIndex.size(); ++i) {
            if (mShuffleIndex.get(i) == playIndex) {
                Collections.swap(mShuffleIndex, i, 0);
                break;
            }
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
                int listSize = mShuffleIndex.size();
                if (next) {
                    index = mShuffleIndex.get(mLastIndex = (mLastIndex + 1) % listSize);
                } else {
                    index = mShuffleIndex.get(
                            mLastIndex = (mLastIndex - 1 + listSize) % listSize);
                }
                mLastIndex = index;
            } else {
                int listSize = mListOfAudioItemList.get(mPlayingIndex).size();
                if (next) {
                    index = (mLastPlay + 1) % listSize;
                } else {
                    index = (mLastPlay - 1 + listSize) % listSize;
                }
            }
            if (index == 0 && next && !fromUser && mLoopWay == AudioPlayService.LIST_NOT_LOOP) {
                playAudio(index, false, mIsShuffle, true);
            } else {
                playAudio(index, mIsPlaying, true, true);
            }
        }
    }

    /**
     * 初始化列表
     */
    private void init() {
        // 标题
        final String[] titles = new String[] {
            "Audio", "Artist", "Album"
        };
        // 排序比较器
        Comparator[] cmps = new Comparator[] {
                new Comparator<Audio>() {
                    @Override
                    public int compare(Audio o1, Audio o2) {
                        return getPinyinString(o1.getTitle())
                                .compareToIgnoreCase(getPinyinString(o2.getTitle()));
                    }
                },
                new Comparator<Audio>() {
                    @Override
                    public int compare(Audio o1, Audio o2) {
                        int res = getPinyinString(o1.getArtist())
                                .compareToIgnoreCase(getPinyinString(o2.getArtist()));
                        if (res != 0) {
                            return res;
                        }
                        return o1.getArtistId() - o2.getArtistId();
                    }
                },
                new Comparator<Audio>() {
                    @Override
                    public int compare(Audio o1, Audio o2) {
                        int res = getPinyinString(o1.getAlbum())
                                .compareToIgnoreCase(getPinyinString(o2.getAlbum()));
                        if (res != 0) {
                            return res;
                        }
                        return o1.getAlbumId() - o2.getAlbumId();
                    }
                }
        };
        // 转换
        AudioToAudioItem[] trans = new AudioToAudioItem[] {
                new AudioToAudioItem() {
                    @Override
                    public AudioItem apply(Audio audio) {
                        AudioItem audioItem = new AudioItem(audio);
                        String title = getPinyinString(audio.getTitle()).toUpperCase();
                        audioItem.setClassificationId(title.length() > 0 ? title.charAt(0) : -1);
                        audioItem.setClassificationName(title.length() > 0 ? title.charAt(0) + "" : "");
                        return audioItem;
                    }
                },
                new AudioToAudioItem() {
                    @Override
                    public AudioItem apply(Audio audio) {
                        AudioItem audioItem = new AudioItem(audio);
                        audioItem.setClassificationId(audio.getArtistId());
                        audioItem.setClassificationName(audio.getArtist());
                        return audioItem;
                    }
                },
                new AudioToAudioItem() {
                    @Override
                    public AudioItem apply(Audio audio) {
                        AudioItem audioItem = new AudioItem(audio);
                        audioItem.setClassificationId(audio.getAlbumId());
                        audioItem.setClassificationName(audio.getAlbum());
                        return audioItem;
                    }
                }
        };

        List<View> viewList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        mAdapterList = new ArrayList<>();
        mListOfAudioItemList = new ArrayList<>();

        for (int i = 0; i < titles.length; ++i) {
            List<Audio> list = AudioList.getAudioList(this, cmps[i]);
            List<AudioItem> itemList = new ArrayList<>();
            for (Audio audio : list) {
                itemList.add(trans[i].apply(audio));
            }
            mListOfAudioItemList.add(itemList);
            final AudioListAdapter adapter = new AudioListAdapter(this, R.layout.list_music, itemList);
            mAdapterList.add(adapter);
            ListView listView = new ListView(this);
            listView.setAdapter(adapter);
            final int index = i;
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mPlayingIndex = index;
                    playAudio(position, true, true, false);
                    adapter.notifyDataSetChanged();
                }
            });
            viewList.add(listView);
            titleList.add(titles[i]);
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(viewList.size() - 1);
        viewPager.setAdapter(new ViewPagerAdapter(titleList, viewList));
    }

    private void pause() {
        if (mLastPlay >= 0) {
            Intent intent = new Intent(ListActivity.this, AudioPlayService.class);
            if (mIsPlaying) {
                intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PAUSE_ACTION);
            } else {
                intent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.REPLAY_ACTION);
            }
            enableButton(false);
            startService(intent);
        }
    }

    private void enableButton(boolean enable) {
        enableButton(enable, false);
    }

    private void enableButton(boolean enable, boolean grey) {
        mBarPauseButton.setEnabled(enable);
        mBarPauseBackground.setEnabled(enable);
        mBarNextButton.setEnabled(enable);

        if (grey && !enable) {
            mBarPauseBackground.setBackgroundResource(R.drawable.shadowed_circle_grey);
        } else {
            mBarPauseBackground.setBackgroundResource(R.drawable.shadowed_circle_red);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        readStatus();

        View barView = findViewById(R.id.bar);
        mBarTitle = (TextView) barView.findViewById(R.id.title);
        mBarArtist = (TextView) barView.findViewById(R.id.artist);
        mBarAlbum = (ImageView) barView.findViewById(R.id.album);
        mBarPauseButton = (ImageButton) barView.findViewById(R.id.home_pauseButton);
        mBarNextButton = (ImageButton) barView.findViewById(R.id.home_nextButton);
        mBarPauseBackground = barView.findViewById(R.id.homebar_background);
        mPaperTitleStrip = (PagerTitleStrip) findViewById(R.id.title_strip);

        //设置tab栏字体
        mPaperTitleStrip.setTextColor(Color.rgb(255, 255, 255));

        mBarTitle.setHorizontallyScrolling(true);
        mBarTitle.setSelected(true);
        mBarArtist.setHorizontallyScrolling(true);
        mBarArtist.setSelected(true);

        enableButton(false, true);

        // 弹出播放器界面
        barView.setOnClickListener(this);

        // 播放条暂停按钮事件监听器
        mBarPauseButton.setOnClickListener(this);

        // 播放条暂停按钮背景事件监听器
        mBarPauseBackground.setOnClickListener(this);

        // 播放条下一首事件监听器
        mBarNextButton.setOnClickListener(this);

        // 事件广播接受器
        LocalBroadcastManager.getInstance(this).registerReceiver(mEventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(AudioPlayService.EVENT_KEY);
                if (event == null) {
                    return;
                }
                switch (event) {
                    case AudioPlayService.FINISHED_EVENT:
                        musicChange(true, false);
                        if (mPlayingIndex >= 0 && mPlayingIndex < mAdapterList.size()) {
                            mAdapterList.get(mPlayingIndex).notifyDataSetChanged();
                        }
                        break;
                    case AudioPlayService.NEXT_EVENT:
                        musicChange(true, true);
                        if (mPlayingIndex >= 0 && mPlayingIndex < mAdapterList.size()) {
                            mAdapterList.get(mPlayingIndex).notifyDataSetChanged();
                        }
                        break;
                    case AudioPlayService.PREVIOUS_EVENT:
                        musicChange(false, true);
                        if (mPlayingIndex >= 0 && mPlayingIndex < mAdapterList.size()) {
                            mAdapterList.get(mPlayingIndex).notifyDataSetChanged();
                        }
                        break;
                    case AudioPlayService.PLAY_EVENT:
                        boolean isPlay = intent.getBooleanExtra(AudioPlayService.AUDIO_PLAY_NOW_BOOL, false);
                        if (isPlay) {
                            mBarPauseButton.setImageResource(R.drawable.pause_light);
                            mIsPlaying = true;
                        } else {
                            mBarPauseButton.setImageResource(R.drawable.play_light);
                            mIsPlaying = false;
                        }
                        enableButton(true);
                        break;
                    case AudioPlayService.PAUSE_EVENT:
                        mBarPauseButton.setImageResource(R.drawable.play_light);
                        mIsPlaying = false;
                        enableButton(true);
                        break;
                    case AudioPlayService.REPLAY_EVENT:
                        mBarPauseButton.setImageResource(R.drawable.pause_light);
                        mIsPlaying = true;
                        enableButton(true);
                        break;
                    case AudioPlayService.LIST_ORDER_EVENT:
                        mIsShuffle = intent.getBooleanExtra(AudioPlayService.LIST_SHUFFLE_BOOL, true);
                        if (mIsShuffle) {
                            shuffleAudioIndex(mListOfAudioItemList.get(mPlayingIndex), mLastPlay);
                            mLastIndex = 0;
                        }
                        saveStatus();
                        break;
                    case AudioPlayService.CHANGE_LOOP_EVENT:
                        mLoopWay = intent.getIntExtra(
                                AudioPlayService.LOOP_WAY_INT, AudioPlayService.LIST_NOT_LOOP);
                        saveStatus();
                        break;
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));

        // 权限分配
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 弹出播放器界面
            case R.id.bar:
                if (mPlayingIndex >= 0 && mPlayingIndex < mListOfAudioItemList.size()) {
                    List<AudioItem> audioItemList = mListOfAudioItemList.get(mPlayingIndex);
                    if (mLastPlay >= 0 && mLastPlay < audioItemList.size()) {
                        Intent intent = getAudioIntent(audioItemList.get(mLastPlay).getAudio());
                        intent.setClass(ListActivity.this, PlayerActivity.class);
                        intent.putExtra(AudioPlayService.AUDIO_IS_PLAYING_BOOL, mIsPlaying);
                        intent.putExtra(AudioPlayService.LIST_SHUFFLE_BOOL, mIsShuffle);
                        intent.putExtra(AudioPlayService.LOOP_WAY_INT, mLoopWay);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
                    }
                }
                break;
            // 暂停按钮
            case R.id.home_pauseButton: case R.id.homebar_background:
                pause();
                break;
            // 下一首
            case R.id.home_nextButton:
                musicChange(true, true);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mIsPlaying) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    AlertDialog dialog = builder.setTitle("音乐正在播放")
                            .setPositiveButton("后台播放", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ListActivity.this.finish();
                                }
                            })
                            .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ListActivity.super.finish();
                                }
                            }).create();

                    dialog.show();
                } else {
                    super.finish();
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
