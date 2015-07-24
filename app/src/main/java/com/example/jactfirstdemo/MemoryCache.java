package com.example.jactfirstdemo;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class MemoryCache {
	private static final int CACHE_SIZE = 1 * 1024 * 1024;  // 1Mb
	private LruCache<String, Bitmap> cache_;
    //private Map<String, SoftReference<Bitmap>> cache_ =
    //	Collections.synchronizedMap(new HashMap<String, SoftReference<Bitmap>>());
 
	public MemoryCache() {
		cache_ = new LruCache<String, Bitmap>(CACHE_SIZE);
	}
	
    public Bitmap get(String id) {
    	return (Bitmap) cache_.get(id);
    }
 
    public void put(String id, Bitmap bitmap) {
		if (bitmap == null) {
			Log.e("MemoryCache::put", "Null bitmap");
		}
    	if (cache_.get(id) == null) {
    		cache_.put(id, bitmap);
    	}
    }
 
    public void Clear() {
        cache_.evictAll();
    }
}