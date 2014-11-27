package com.example.jactfirstdemo;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
 
public class FileCache {
    private File cache_dir_;
 
    public FileCache(Context context) {
        // Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(
        		android.os.Environment.MEDIA_MOUNTED)) {
            cache_dir_ = new File(android.os.Environment.getExternalStorageDirectory(),
            		              "LazyList");
        } else {
            cache_dir_ = context.getCacheDir();
        }
        if (!cache_dir_.exists()) {
          cache_dir_.mkdirs();
        }
    }
 
    public File getFile(String url) {
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        //String filename = String.valueOf(url.hashCode());
        //Another possible solution (thanks to grantland)
        String filename = "PHB_foo";
		try {
			filename = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        File f = new File(cache_dir_, filename);
        return f;
    }
 
    public void Clear() {
        File[] files = cache_dir_.listFiles();
        if (files == null) {
            return;
        }
        for (File f:files) {
            f.delete();
        }
    }
}