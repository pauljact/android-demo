package com.example.jactfirstdemo;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.util.Log;
 
public class FileCache {
    private File cache_dir_;
    private long max_dir_size_;
 
    public FileCache(File cache_parent_dir, String dir, long max_size) {
    	cache_dir_ = new File(cache_parent_dir, dir);
        if (!cache_dir_.exists()) {
          cache_dir_.mkdirs();
        }
        max_dir_size_ = max_size;
    }
    
    public File getEncodedFilename(String url) {
    	if (JactFileUtils.GetDirSize(cache_dir_) > max_dir_size_) {
    		Log.i("PHB", "FileCache::getFile. Not enough space in CacheDir to create new file.");
    		JactFileUtils.TrimDirToMaxSize(cache_dir_, max_dir_size_);
    	}
        String filename = "PHB_foo";
		try {
			filename = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO(PHB): Implement this.
			e.printStackTrace();
			Log.e("PHB ERROR", "FileCache::getFile: UnsupportedEncodingException:\n" +
			                   e.toString());
		}
		if (filename.equals("PHB_foo")) {
		  Log.e("PHB ERROR", "FileCache::getFile: Never reset file name.");
		}
        File f = new File(cache_dir_, filename);
        return f;
    }
 
    public void Clear() {
        File[] files = cache_dir_.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            f.delete();
        }
    }
}