package com.goodjob.musicplayer.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Godot on 2017/6/3.
 */

public class ObjectPool<T> {
    private int mSize;
    private HashMap<String, T> mMap = new HashMap<>();
    private Queue<String> mQue = new LinkedList<>();

    public ObjectPool() {
        this(128);
    }

    public ObjectPool(int size) {
        mSize = size;
    }

    public void put(String key, T obj) {
        if (mQue.size() == mSize) {
            String first = mQue.remove();
            mMap.remove(first);
        }
        mQue.add(key);
        mMap.put(key, obj);
    }

    public T get(String key) {
        return mMap.get(key);
    }
}
