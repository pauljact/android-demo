package com.jact.jactapp;

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
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("MemoryCache::put", "Null bitmap");
			return;
		}
		if (cache_ == null) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("MemoryCache::put", "Null cache_");
			cache_ = new LruCache<String, Bitmap>(CACHE_SIZE);
		}
		if (id == null) {
			if (!JactActionBarActivity.IS_PRODUCTION) Log.e("MemoryCache::put", "Null id");
			return;
		}
    	if (cache_.get(id) == null) {
    		cache_.put(id, bitmap);
    	}
    }
 
    public void Clear() {
        cache_.evictAll();
    }
}