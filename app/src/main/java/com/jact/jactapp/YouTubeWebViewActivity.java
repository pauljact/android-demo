package com.jact.jactapp;

/**
 * Created by Paul on 12/19/2015.
 */

import com.jact.jactapp.GetUrlTask.FetchStatus;
import com.jact.jactapp.JactNavigationDrawer.ActivityIndex;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

public class YouTubeWebViewActivity extends JactActionBarActivity
        implements //MediaPlayer.OnCompletionListener,
                   ProcessUrlResponseCallback {
    private static String youtube_webview_url_;
    private static String youtube_url_within_earn_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set layout.
        super.onCreate(savedInstanceState, R.string.youtube_webview_label,
                R.layout.youtube_webview_layout,
                JactNavigationDrawer.ActivityIndex.EARN);
        youtube_url_within_earn_ = "";
        youtube_webview_url_ = GetUrlTask.GetJactDomain() + "/earn/";
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set cookies for WebView.
        SharedPreferences user_info = getSharedPreferences(
                getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
        String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
        List<String> cookie_headers = Arrays.asList(cookies.split(GetUrlTask.COOKIES_SEPERATOR));
        HttpCookie cookie = null;
        for (String cookie_str : cookie_headers) {
            cookie = HttpCookie.parse(cookie_str).get(0);
        }
        if (cookie != null) {
            CookieManager cookie_manager = CookieManager.getInstance();
            cookie_manager.setCookie(
                    youtube_webview_url_,
                    cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
        }

        youtube_url_within_earn_ = getIntent().getStringExtra(getString(R.string.earn_page_url));
        //PHBBHP Use below line when mobile is ready
        //PHBBHP youtube_webview_url_ = GetUrlTask.GetJactDomain() + "/earn/" + youtube_url_within_earn_;
        youtube_webview_url_ = "https://www.jact.com/earn/" + youtube_url_within_earn_;
        navigation_drawer_.setActivityIndex(JactNavigationDrawer.ActivityIndex.EARN);

        // Set webview from youtube_webview_url_.
        WebView web_view = (WebView) findViewById(R.id.youtube_webview_webview);
        if (web_view != null) {
            web_view.onResume();
            web_view.resumeTimers();
        }

        // Enable Javascript (and other copied code).
        WebSettings webSettings = web_view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);

        // New 6/1/2016 to allow display/interaction of HTML5 urls (with games).
        // Didn't work, so commenting out.
        /*
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        // DEPRECATED webSettings.setPluginState(true);
        web_view.setWebChromeClient(new WebChromeClient() {
          @Override
          public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            if (!JactActionBarActivity.IS_PRODUCTION) {
                Log.e("YouTubeWevViewActivity::onShowCustomView", "Do Something");
            }
            OnShowCustomView(view);

          }
          @Override
          public void onHideCustomView() {
                super.onHideCustomView();

                if (!JactActionBarActivity.IS_PRODUCTION) {
                    Log.e("YouTubeWevViewActivity::onHideCustomView", "Do Something");
                }
          }
        });
        web_view.setInitialScale(1);
        */

        web_view.loadUrl(youtube_webview_url_);
        web_view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                fadeAllViews(false);
            }
        });
        // Set spinner (and hide WebView) until page has finished loading.
        GetCart(this);
        fadeAllViews(num_server_tasks_ > 0);
    }

    @Override
    protected void onPause(){
        super.onPause();
        WebView web_view = (WebView) findViewById(R.id.youtube_webview_webview);
        if (web_view != null){
            web_view.onPause();
            web_view.pauseTimers();
        }
    }
    // New 6/1/2016 to allow display/interaction of HTML5 urls (with games)
    /*
    @Override
    public void onCompletion(MediaPlayer mp) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("YouTubeWevViewActivity::onCompletion", "onCompletion with MediaPlayer mp");
      }
        //a.setContentView(R.layout.main);
        //WebView wb = (WebView) a.findViewById(R.id.webview);
        //a.initWebView();
    }
    // New 6/1/2016 to allow display/interaction of HTML5 urls (with games)
    public void OnShowCustomView(View view) {
        if (view instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) view;
            if (frame.getFocusedChild() instanceof VideoView) {
                VideoView video = (VideoView) frame.getFocusedChild();
                frame.removeView(video);
                setContentView(video);
                video.setOnCompletionListener(this);
                //PHBvideo.setOnErrorListener(this);
                video.start();
            }
        }
    }
    */

    @Override
    public void fadeAllViews(boolean should_fade) {
        ProgressBar spinner = (ProgressBar) findViewById(R.id.youtube_webview_progress_bar);
        AlphaAnimation alpha;
        if (should_fade) {
            spinner.setVisibility(View.VISIBLE);
            alpha = new AlphaAnimation(0.5F, 0.5F);
        } else {
            spinner.setVisibility(View.INVISIBLE);
            alpha = new AlphaAnimation(1.0F, 1.0F);
        }
        // The AlphaAnimation will make the whole content frame transparent
        // (so that none of the views show).
        alpha.setDuration(0); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.youtube_webview_content_frame);
        layout.startAnimation(alpha); // Add animation to the layout.
    }

    @Override
    public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
        ProcessCartResponse(this, webpage, cookies, extra_params);
    }

    @Override
    public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
        // TODO Auto-generated method stub
    }

    @Override
    public void ProcessFailedResponse(GetUrlTask.FetchStatus status, String extra_params) {
        ProcessFailedCartResponse(this, status, extra_params);
    }
}