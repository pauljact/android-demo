package com.example.jactfirstdemo;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
 
public class MemoryCache {
    private Map<String, SoftReference<Bitmap>> cache_ =
    	Collections.synchronizedMap(new HashMap<String, SoftReference<Bitmap>>());
 
    public Bitmap get(String id){
        if (!cache_.containsKey(id)) {
            return null;
        }
        SoftReference<Bitmap> ref = cache_.get(id);
        return ref.get();
    }
 
    public void put(String id, Bitmap bitmap){
        cache_.put(id, new SoftReference<Bitmap>(bitmap));
    }
 
    public void Clear() {
        cache_.clear();
    }
}