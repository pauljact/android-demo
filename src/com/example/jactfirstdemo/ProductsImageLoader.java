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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

public class ProductsImageLoader {
    private MemoryCache memory_cache_;
    private FileCache file_cache_;
    private final int stub_id_ = R.drawable.no_image;
    private ExecutorService executor_service_;
    private ProductsAdapter parent_product_adapter_;
    private CheckoutAdapter parent_checkout_adapter_;
    private EarnAdapter parent_earn_adapter_;
    private Map<ImageView, String> image_views_;
    private Map<String, HashSet<Integer>> urls_being_fetched_;

    // Constructor from ProductsAdapter.
    public ProductsImageLoader(Context context, ProductsAdapter a, String activity_name) {
    	parent_product_adapter_ = a;
    	parent_checkout_adapter_ = null;
    	parent_earn_adapter_ = null;
    	memory_cache_ = new MemoryCache();
        file_cache_ = new FileCache(context.getCacheDir(), activity_name, 4 * 1024 * 1024 /* 4Mb */);
        executor_service_ = Executors.newFixedThreadPool(10);
        image_views_ =
            Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
        urls_being_fetched_  =
                Collections.synchronizedMap(new HashMap<String, HashSet<Integer>>());
    }
    
    // Constructor from CheckoutAdapter.
    public ProductsImageLoader(Context context, CheckoutAdapter a, String activity_name) {
    	parent_checkout_adapter_ = a;
    	parent_product_adapter_ = null;
    	parent_earn_adapter_ = null;
    	memory_cache_ = new MemoryCache();
        file_cache_ = new FileCache(context.getCacheDir(), activity_name, 4 * 1024 * 1024 /* 4Mb */);
        executor_service_ = Executors.newFixedThreadPool(10);
        image_views_ =
            Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
        urls_being_fetched_  =
                Collections.synchronizedMap(new HashMap<String, HashSet<Integer>>());
    }
    
    // Constructor from EarnAdapter
    public ProductsImageLoader(Context context, EarnAdapter a, String activity_name) {
    	parent_checkout_adapter_ = null;
    	parent_product_adapter_ = null;
    	parent_earn_adapter_ = a;
    	memory_cache_ = new MemoryCache();
        file_cache_ = new FileCache(context.getCacheDir(), activity_name, 4 * 1024 * 1024 /* 4Mb */);
        executor_service_ = Executors.newFixedThreadPool(10);
        image_views_ =
            Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
        urls_being_fetched_  =
                Collections.synchronizedMap(new HashMap<String, HashSet<Integer>>());
    }
 
    public boolean DisplayImage(String url, ImageView image_view, int position)
    {
    	// Every time DisplayImage is called, the image_view that
    	// is passed in IS THE SAME, (even when DisplayImage is
    	// called for different List Elements; i.e. all the time).
    	// This means that, at all times, image_views_ will be a
    	// map with a single element. Later, when we check image_views_,
    	// we'll make sure that the url being processed matches the
    	// url in image_views_; i.e. that the current image view
    	// on the main thread is the one corresponding to the url
    	// we're working on.
        image_views_.put(image_view, url);
        
    	// Check if image at target url has already been fetched
    	// and is being stored in memory_cache_.
    	Bitmap bitmap = memory_cache_.get(url);
        if (bitmap != null) {
        	// Image already exists, display it from cache.
            image_view.setImageBitmap(bitmap);
            return true;
        } else {
        	// Image does not exist in cache, fetch it; in the
        	// meantime, display 'no_image' icon.
        	HashSet<Integer> positions_for_url = urls_being_fetched_.get(url);
        	if (positions_for_url == null) {
        		// This is first position referencing this url.
        		positions_for_url = new HashSet<Integer>();
            	QueuePhoto(url, image_view, position);
        	}
        	positions_for_url.add(position);
        	urls_being_fetched_.put(url, (HashSet<Integer>) positions_for_url);
            image_view.setImageResource(stub_id_);
            return false;
        }
    }
 
    private void QueuePhoto(String url, ImageView image_view, int position) {
        PhotoToLoad photo = new PhotoToLoad(url, image_view);
        executor_service_.submit(new PhotosLoader(photo));
    }
 
    //Task for the queue
    private class PhotoToLoad
    {
        public String url_;
        public ImageView image_view_;
        
        public PhotoToLoad(String u, ImageView i) {
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
        	// This process was run on a background thread. It is possible that
        	// in the meantime, some other thread already cached the image. Check
        	// to see if image is already present in cache, and if so, return.
        	Bitmap bmp = memory_cache_.get(photo_to_load_.url_);
        	if (bmp == null) {
            	bmp = GetBitmap(photo_to_load_.url_);
        	}
        	// Store image in cache.
        	// GetBitmap may have taken some time (if we had to fetch image from
        	// web). Check again to make sure that some other thread in the meantime
        	// didn't already put this url into memory_cache_ (probably not crucial,
        	// since even if this is the case, it probably is not a big deal just
        	// to overwrite the bmp in cache; but does avoid this small overhead).
        	if (memory_cache_.get(photo_to_load_.url_) == null) {
            	memory_cache_.put(photo_to_load_.url_, bmp);
        	}
            /* PHB REMOVE
            // Before loading the image, make sure that we're still on the
        	// appropriate Element in the ListView.
            if (ImageViewReused(photo_to_load_)) {
                return;
            }*/
        	
            // Load image.
            BitmapDisplayer bd = new BitmapDisplayer(bmp, photo_to_load_);
            Activity a = (Activity) photo_to_load_.image_view_.getContext();
            a.runOnUiThread(bd);
        }
    }
    
    private Bitmap GetBitmap(String url) {
    	// Check if image already exists in file_cache_ (on SD). 
        File f = file_cache_.getEncodedFilename(url);
        Bitmap b = DecodeFile(f);
        if (b != null) {
        	// File found. Return image.
        	return b;
        }
        
        // File not found. Fetch it from web.
        try {
        	// Fetch file.
        	Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream is = conn.getInputStream();
            
            // Write image to SD.
            OutputStream os = new FileOutputStream(f);
            IOUtils.copy(is, os);
            os.close();
            
            // Return image.
            bitmap = DecodeFile(f);
            return bitmap;
        } catch (Exception ex) {
           	Log.e("PHB ERROR", "ProductsImageLoader::GetBitmap.\n" +
           			           "Unable to fetch image at url: " + url +
           			           ". Exception:\n" + ex.toString());
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
            final int REQUIRED_SIZE = 70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while(true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
 
            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
 
    // Checks to see if the main activity thread has moved on from
    // and loaded a new List Element's ImageView (which is recycled,
    // and hence the same ImageView as is referenced by photo_to_load.image_view_,
    // but now represents a DIFFERENT list element). If so, stops processing,
    // as it will be all screwed up (the image that it has just fetched will be
    // put on the wrong element). Note that using this properly requires that
    // any time the current processing is aborted due to this function returning
    // true, that the app has ALREADY successfully loaded the image, and hence
    // aborting here is ok.
    boolean ImageViewReused(PhotoToLoad photo_to_load) {
        String tag = image_views_.get(photo_to_load.image_view_);
        // We check whether the main thread is indeed on the relevant element
        // position by seeing if the url of the main thread's current ImageView
        // matches the url of the photo being loaded here. Note that it is
        // possible that two different list elements have that same url, and
        // so this test is not perfect; but in this case, it doesn't matter,
        // since they have the same url, loading that image on the different
        // (current) ImageView won't matter, as it will still be loading the
        // proper image.
        if (tag == null || !tag.equals(photo_to_load.url_)) {
            return true;
        }
        return false;
    }
 
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
        	// Before loading the image, make sure that we're still on the
        	// appropriate Element in the ListView.
            if (ImageViewReused(photo_to_load_)) {
            	// Don't load anything to this imageview.
            } else if (bitmap_ != null) {
                photo_to_load_.image_view_.setImageBitmap(bitmap_);
            } else {
                photo_to_load_.image_view_.setImageResource(stub_id_);
            }
            AlertAdapter(photo_to_load_.url_);
        }
    }
 
    private void AlertAdapter(String url) {
    	if (parent_product_adapter_ != null) {
    	  parent_product_adapter_.alertPositionsReady(urls_being_fetched_.get(url));
    	}
    	if (parent_checkout_adapter_ != null) {
      	  parent_checkout_adapter_.alertPositionsReady(urls_being_fetched_.get(url));
      	}
    	if (parent_earn_adapter_ != null) {
    	  parent_earn_adapter_.alertPositionsReady(urls_being_fetched_.get(url));
    	}
    	urls_being_fetched_.remove(url);
    }
    
    public void clearCaches() {
        memory_cache_.Clear();
        file_cache_.Clear();
    }
}