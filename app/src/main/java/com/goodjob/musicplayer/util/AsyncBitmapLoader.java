package com.goodjob.musicplayer.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.goodjob.musicplayer.R;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Godot on 2017/6/5.
 */

public class AsyncBitmapLoader {
    private Context mContext;
    private LruCache<String, BitmapDrawable> mLruCache;
    private Map<Integer, String> mPathMap = Collections.synchronizedMap(new HashMap<Integer, String>());

    private class bitmapCache extends LruCache<String, BitmapDrawable> {
        public bitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, BitmapDrawable value) {
            return value.getBitmap().getByteCount();
        }
    }
    private class AsyncBitmapTask extends AsyncTask<Integer, Void, BitmapDrawable> {
        private ImageView mImageView;
        private int mDefaultRes;
        private int mAlbumId;

        public AsyncBitmapTask(ImageView imageView, int albumId, int defaultRes) {
            super();
            mImageView = imageView;
            mDefaultRes = defaultRes;
            mAlbumId = albumId;
        }

        @Override
        protected BitmapDrawable doInBackground(Integer... params) {
            int albumId = params[0];
            BitmapDrawable bitmapDrawable;
            String path;
            if (mPathMap.containsKey(albumId)) {
                path = mPathMap.get(albumId);
            } else {
                path = MediaUtils.getAlbumArt(mContext, albumId);
            }
            if (path == null) {
                bitmapDrawable = null;
            } else {
                bitmapDrawable = mLruCache.get(path);
                if (bitmapDrawable == null) {
                    mLruCache.put(path,
                            bitmapDrawable = new BitmapDrawable(mContext.getResources(), BitmapFactory.decodeFile(path)));
                }
            }
            return bitmapDrawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable bitmapDrawable) {
            Integer tag = (Integer) mImageView.getTag();
            if (tag != null && tag.equals(mAlbumId)) {
                if (bitmapDrawable != null) {
                    mImageView.setImageDrawable(bitmapDrawable);
                } else {
                    mImageView.setImageResource(mDefaultRes);
                }
            }
        }
    }

    public AsyncBitmapLoader(Context context) {
        mContext = context;
        int cacheSize = ((ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() << 20;
        mLruCache = new bitmapCache(cacheSize);
    }

    public void load(ImageView imageView, int albumId, int defaultRes) {
        new AsyncBitmapTask(imageView, albumId, defaultRes).execute(albumId);
    }
}
