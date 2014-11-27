package com.example.jactfirstdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;
 
public class ProductsImageLoader {
    MemoryCache memory_cache_ = new MemoryCache();
    FileCache file_cache_;
    final int stub_id_ = R.drawable.no_image;
    //PHBprivate Map<ImageView, String> image_views_ =
    //PHB    Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    ExecutorService executor_service_; 
 
    public ProductsImageLoader(Context context){
        file_cache_ = new FileCache(context);
        executor_service_ = Executors.newFixedThreadPool(5);
    }
 
    public void DisplayImage(String url, ImageView image_view)
    {
        //PHBimage_views_.put(image_view, url);
        Bitmap bitmap = memory_cache_.get(url);
        if (bitmap != null) {
            image_view.setImageBitmap(bitmap);
        } else {
            QueuePhoto(url, image_view);
            image_view.setImageResource(stub_id_);
        }
    }
 
    private void QueuePhoto(String url, ImageView image_view) {
        PhotoToLoad photo = new PhotoToLoad(url, image_view);
        executor_service_.submit(new PhotosLoader(photo));
    }
 
    //Task for the queue
    private class PhotoToLoad
    {
        public String url_;
        public ImageView image_view_;
        
        public PhotoToLoad(String u, ImageView i){
            url_ = u;
            image_view_ = i;
        }
    }
 
    class PhotosLoader implements Runnable {
        PhotoToLoad photo_to_load_;
        
        PhotosLoader(PhotoToLoad photo_to_load) {
            this.photo_to_load_ = photo_to_load;
        }
 
        @Override
        public void run() {
            //PHBif (ImageViewReused(photo_to_load_)) {
            //PHB    return;
            //PHB}
            Bitmap bmp = GetBitmap(photo_to_load_.url_);
            memory_cache_.put(photo_to_load_.url_, bmp);
            //PHBif (ImageViewReused(photo_to_load_)) {
            //PHB    return;
            //PHB}
            BitmapDisplayer bd = new BitmapDisplayer(bmp, photo_to_load_);
            Activity a = (Activity) photo_to_load_.image_view_.getContext();
            a.runOnUiThread(bd);
        }
    }
    
    private Bitmap GetBitmap(String url) {
        File f = file_cache_.getFile(url);
 
        //from SD cache
        Bitmap b = DecodeFile(f);
        if (b != null) {
            return b;
        }
 
        //from web
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            IOUtils.copy(is, os);
            //PHBUtil.copyStream(is, os);
            os.close();
            bitmap = DecodeFile(f);
            return bitmap;
        } catch (Exception ex){
           ex.printStackTrace();
           return null;
        }
    }
 
    // Decodes image and scales it to reduce memory consumption
    private Bitmap DecodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
 
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while(true){
                if(width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
 
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
 /*
    boolean ImageViewReused(PhotoToLoad photo_to_load){
        String tag = image_views_.get(photo_to_load.imageView);
        if (tag == null || !tag.equals(photo_to_load.url)) {
            return true;
        }
        return false;
    }
 */
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap_;
        PhotoToLoad photo_to_load_;
        
        public BitmapDisplayer(Bitmap b, PhotoToLoad p) {
        	bitmap_ = b;
        	photo_to_load_ = p;
        }
        
        @Override
        public void run()
        {
            //PHBif (ImageViewReused(photo_to_load_)) {
            //PHB    return;
            //PHB}
            if (bitmap_ != null) {
                photo_to_load_.image_view_.setImageBitmap(bitmap_);
            } else {
                photo_to_load_.image_view_.setImageResource(stub_id_);
            }
        }
    }
 
    public void clearCache() {
        memory_cache_.Clear();
        file_cache_.Clear();
    }
 
}