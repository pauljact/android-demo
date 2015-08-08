package com.jact.jactfirstdemo;

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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

public class ProductsImageLoader {
    private MemoryCache memory_cache_;
    private FileCache file_cache_;
    private final int stub_id_ = R.drawable.no_image;
    private final int play_video_icon_id_ = R.drawable.play_button;
    private ExecutorService executor_service_;
    private AdapterWithImages parent_adapter_;
    private Map<ImageView, String> image_views_;
    private Map<String, Integer> failed_fetches_;
    private Map<String, HashSet<Integer>> urls_being_fetched_;

    public ProductsImageLoader(Context context, AdapterWithImages a, String activity_name) {
    	parent_adapter_ = a;
    	memory_cache_ = new MemoryCache();
        file_cache_ = new FileCache(context.getCacheDir(), activity_name, 4 * 1024 * 1024 /* 4Mb */);
        executor_service_ = Executors.newFixedThreadPool(10);
        image_views_ =
            Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
        urls_being_fetched_  =
                Collections.synchronizedMap(new HashMap<String, HashSet<Integer>>());
        failed_fetches_ = Collections.synchronizedMap(new HashMap<String, Integer>());
    }

    public boolean DisplayImage(String url, ImageView image_view, int position) {
      return DisplayImage(url, image_view, position, false);
    }
    
    public boolean DisplayImage(
            String url, ImageView image_view, int position, boolean display_play_icon) {
        if (url == null) {
          if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::DisplayImage", "Null url");
          return false;
        }
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
            if (!display_play_icon) {
        	  image_view.setImageBitmap(bitmap);
            } else {
              Drawable[] layers = new Drawable[2];
              layers[0] = parent_adapter_.GetDrawable(bitmap);
              layers[1] = parent_adapter_.GetDrawable(play_video_icon_id_);
              LayerDrawable layerDrawable = new LayerDrawable(layers);
              image_view.setImageDrawable(layerDrawable);
            }
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
        PhotosLoader photo_loader = new PhotosLoader(photo);
        photo_loader.execute();
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

    public class PhotosLoader extends AsyncTask<Void, Void, Void> {

        PhotoToLoad photo_to_load_;

        PhotosLoader(PhotoToLoad photo_to_load) {
            this.photo_to_load_ = photo_to_load;
        }

        @Override
        protected Void doInBackground(Void... strings) {
            // This process was run on a background thread. It is possible that
            // in the meantime, some other thread already cached the image. Check
            // to see if image is already present in cache, and if so, return.
            Bitmap bmp = memory_cache_.get(photo_to_load_.url_);
            if (failed_fetches_.get(photo_to_load_.url_) == null) {
                failed_fetches_.put(photo_to_load_.url_, 1);
            } else {
                failed_fetches_.put(photo_to_load_.url_,
                        failed_fetches_.get(photo_to_load_.url_).intValue() + 1);
            }
            if (failed_fetches_.get(photo_to_load_.url_).intValue() > 5) {
                if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::doInBackground","exceeded number failed gets");
                return null;
            }
            if (bmp == null) {
                bmp = GetBitmap(photo_to_load_.url_);
            }
            if (bmp == null) {
                if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::doInBackground",
                                                                "Null bmp (perhaps img is behind private url?); photo will not be displayed");
                return null;
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
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            AlertAdapter(photo_to_load_.url_);
        }
    }

    private Bitmap GetBitmap(String url) {
    	// Check if image already exists in file_cache_ (on SD). 
        File f = file_cache_.getEncodedFilename(url);
        if (f == null) if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::GetBitmap", "Null file for url: " + url);
        Bitmap b = DecodeFile(f);
        if (b != null) {
        	// File found. Return image.
        	return b;
        }
        
        // File not found. Do a synchronous (blocking; ok since we're on not on main thread)
        // call to fetch it from web.
        try {
            if (!JactActionBarActivity.IS_PRODUCTION) Log.d("ProductsImageLoader", "Fetching image at url: " + url);
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
            if (bitmap == null) if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::GetBitmap", "Null bitmap");
            return bitmap;
        } catch (Exception ex) {
           	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::GetBitmap",
           		  "Unable to fetch image at url: " + url + ". Exception:\n" + ex.toString());
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
            final int REQUIRED_SIZE = 800;
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
            //return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

            // PHB NEW.

            o2.inJustDecodeBounds = false;
            o2.inDither = false;
            o2.inPreferredConfig = Bitmap.Config.ARGB_8888;
            o2.inScaled = false;
            Bitmap temp = BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

            // Resize
            float desiredScale = (float) REQUIRED_SIZE / width_tmp;
            Matrix matrix = new Matrix();
            matrix.postScale(desiredScale, desiredScale);
            Bitmap scaledBitmap =
                Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
            temp = null;
            return scaledBitmap;

            // END PHB NEW.
        } catch (FileNotFoundException e) {
            if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ProductsImageLoader::DecodeFile", "Unable to decode file '" + f.getName() +
                                                     "': File Not Found.");
        }
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
        return (tag == null || !tag.equals(photo_to_load.url_));
    }

    private void AlertAdapter(String url) {
      parent_adapter_.AlertPositionsReady(urls_being_fetched_.get(url));
      urls_being_fetched_.remove(url);
    }
    
    public void clearCaches() {
        memory_cache_.Clear();
        file_cache_.Clear();
    }
}