package com.example.jactfirstdemo;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class JactLoggedInHomeActivity extends JactActionBarActivity implements
        ProcessUrlResponseCallback,
        View.OnClickListener {

  private String jact_user_name_;
  private String jact_user_id_;
  private boolean init_once_;

  private HorizontalScrollView scroll_view_;
  private TextView left_caret_tv_;
  private TextView right_caret_tv_;
  private LayoutInflater inflater_;
  OnProductClickListener featured_rewards_listener_;
  
  // The follow fields used for GCM.
  public static final String PROPERTY_REG_ID = "gcm_registration_id";
  private static final String PROPERTY_APP_VERSION = "Jact_v1.0.0";
  private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private final static String SENDER_ID = "404003292102";  // From GCM Project Number
  GoogleCloudMessaging gcm_;
  AtomicInteger msg_id_ = new AtomicInteger();

  public static class FeatureRewardsLayoutItem {
    LinearLayout ll_;
    ImageView img_view_;
    TextView is_drawing_;
    TextView drawing_date_;
    TextView title_;
    TextView summary_;
    TextView quantity_;
    TextView pid_;
    TextView orig_price_;
    LinearLayout jact_price_ll_;
    TextView jact_price_;
    ImageView jact_icon_;
    TextView jact_points_;
  }
  public ProductsImageLoader featured_image_loader_;
  public ProductsImageLoader earn_image_loader_;
  private static ArrayList<ProductsPageParser.ProductItem> featured_rewards_;
  private static ArrayList<EarnPageParser.EarnItem> featured_earn_;
  private LinearLayout featured_rewards_ll_;
  private LinearLayout featured_earn_ll_;
  //private static ArrayList<FeatureRewardsLayoutItem> featured_rewards_layouts_;
  private static ArrayList<View> featured_rewards_layouts_;
  private static ArrayList<View> featured_earn_layouts_;
  private boolean[] unfinished_rewards_elements_;
  private boolean[] unfinished_earn_elements_;
  private static final String FETCH_FEATURED_REWARDS_PAGE_TASK = "fetch_featured_rewards_page_task";
  private static final String FETCH_FEATURED_EARN_PAGE_TASK = "fetch_featured_earn_page_task";
  private static final String FETCH_REWARDS_PAGE_TASK = "fetch_rewards_page_task";
  private static final String FETCH_AND_START_REWARDS_TASK = "fetch_and_start_rewards_task";
  private static final String FETCH_REWARDS_PRIZE_DRAWINGS_TASK = "fetch_rewards_prizes_task";
  private static final String DATE_PREFIX = "Drawing Date: ";
  private static String featured_rewards_url_;
  private static String featured_earn_url_;
  private static String rewards_url_;
  private static int next_drawing_bar_height_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState, R.string.app_name,
            R.layout.jact_logged_in_home_screen,
            JactNavigationDrawer.ActivityIndex.PROFILE);
    init_once_ = false;

    // Set Featured Rewards.
    inflater_ = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    featured_image_loader_ =
            new ProductsImageLoader(this, new FeaturedRewardsImageLoader(), "JactLoginActivity");
    earn_image_loader_ =
        new ProductsImageLoader(this, new FeaturedEarnImageLoader(), "JactLoginActivity");
    featured_rewards_ll_ = (LinearLayout) findViewById(R.id.featured_rewards_ll);
    featured_earn_ll_ = (LinearLayout) findViewById(R.id.featured_earn_ll);
    left_caret_tv_ = (TextView) findViewById(R.id.featured_rewards_left_tv);
    left_caret_tv_.setVisibility(View.INVISIBLE);
    right_caret_tv_ = (TextView) findViewById(R.id.featured_rewards_right_tv);
    scroll_view_ = (HorizontalScrollView) findViewById(R.id.featured_rewards_sv);
    //PHB_OLD scroll_view_.setOnClickListener(this);
    scroll_view_.getViewTreeObserver().addOnScrollChangedListener(
        new ViewTreeObserver.OnScrollChangedListener() {
      @Override
      public void onScrollChanged() {
        int scrollX = scroll_view_.getScrollX();
        if (scrollX == 0) {
          left_caret_tv_.setVisibility(View.INVISIBLE);
        } else {
          left_caret_tv_.setVisibility(View.VISIBLE);
        }
        if (scrollX >= scroll_view_.getMaxScrollAmount()) {
          right_caret_tv_.setVisibility(View.INVISIBLE);
        } else {
          right_caret_tv_.setVisibility(View.VISIBLE);
        }
      }
    });

    unfinished_rewards_elements_ = new boolean[100];
    unfinished_earn_elements_ = new boolean[100];
    featured_rewards_url_= GetUrlTask.GetJactDomain() + "/rest/featured";
    featured_earn_url_= GetUrlTask.GetJactDomain() + "/rest/earn-featured";
    rewards_url_ = GetUrlTask.GetJactDomain() + "/rest/rewards.json";

    // Get Original height of Next Drawing Bar.
    RelativeLayout next_drawing_bar = (RelativeLayout) findViewById(R.id.featured_prizes_ll);
    ViewGroup.LayoutParams bar_params = next_drawing_bar.getLayoutParams();
    next_drawing_bar_height_ = bar_params.height;

    // Start the Service that will run in background and handle GCM interactions.
    if (CheckPlayServices()) {
      gcm_ = GoogleCloudMessaging.getInstance(this);
      String reg_id = GetRegistrationId(getApplicationContext());

      if (reg_id.isEmpty()) {
        RegisterInBackground();
      }
    }
    startService(new Intent(this, GcmIntentService.class));
  }
  
  @Override
  protected void onResume() {
	super.onResume();	
	
	// Make sure Google Play is on user's device (so they can use GCM).
	CheckPlayServices();
    
	// Check if the Logged-In State is ready (all user info has already been fetched).
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String was_logged_off = user_info.getString(getString(R.string.was_logged_off_key), "");
    String was_logged_off_other_check = getIntent().getStringExtra(getString(R.string.was_logged_off_key));
    boolean was_logged_off_other_check_bool =
    	(was_logged_off_other_check != null) && was_logged_off_other_check.equalsIgnoreCase("true");
    // Logged-In State is not ready. Fetch requisite items.
    if (!init_once_ || num_server_tasks_ != 0 || !was_logged_off.equalsIgnoreCase("false") ||
    	was_logged_off_other_check_bool) {
      num_server_tasks_ = 0;
      init_once_ = true;
      
      // Retrieve information that was retrieved from Jact Server on inital login.
      jact_user_name_ = user_info.getString(getString(R.string.ui_user_name), "");
      jact_user_id_ = user_info.getString(getString(R.string.ui_user_id), "");
      if (jact_user_name_.isEmpty() || jact_user_id_.isEmpty()) {
        Log.e("JactLoggedInHomeActivity.onCreate",
              "User Info file is missing the requisite info.");
      }
    }
    
    // Set Cart Icon.
	SetCartIcon(this);

    // Get Featured Rewards.
    FetchFeaturedRewardsPage();

    // Get Featured Earn.
    FetchFeaturedEarnPage();

    // Get Rewards (to get Prize Drawings for Next Drawing Date).
    FetchPrizeDrawings();

    // Re-enable parent activity before transitioning to the next activity.
    // This ensures e.g. that when user hits 'back' button, the screen
    // is 'active' (not faded) when the user returns.
    fadeAllViews(num_server_tasks_ != 0);
  }
  
  @Override
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.my_profile_progress_bar);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.my_profile_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }

  @Override
  public void onClick(View view) {
    if (((View) view.getParent()).getId() == R.id.featured_rewards_ll) {
      // Initialize Product Popup Window.
      PopupWindow product_popup =
              new PopupWindow(inflater_.inflate(
                      R.layout.product_popup, null),
                      ActionBar.LayoutParams.MATCH_PARENT,
                      ActionBar.LayoutParams.WRAP_CONTENT);
      featured_rewards_listener_ = new OnProductClickListener(product_popup);
      featured_rewards_listener_.onFeaturedItemClick(view);
    }
    if (((View) view.getParent()).getId() == R.id.featured_earn_ll) {
      Log.e("PHB TEMP", "JLIHA::onClick. Featured Earn click");
    }
  }

  public void doPrizeDrawingsDetailsClick(View view) {
    FetchRewardsPage(FETCH_REWARDS_PRIZE_DRAWINGS_TASK);
    fadeAllViews(true);
  }

  public void doAddToCartClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    ShoppingUtils.LineItem item = new ShoppingUtils.LineItem();
    ProductsActivity.MessageHolder holder = new ProductsActivity.MessageHolder();
    if (!ProductsActivity.doAddToCartClick(featured_rewards_listener_, item, holder)) {
      Log.e("PHB TEMP", "JLIHA::doAddToCartClick. Not adding item here");
      if (holder.title_ != null && !holder.title_.isEmpty()) {
        if (holder.message_ != null && !holder.message_.isEmpty()) {
          DisplayPopupFragment(holder.title_, holder.message_, "Bad_Add_Product_Dialog");
        } else {
          DisplayPopupFragment(holder.title_, "Bad_Add_Product_Dialog");
        }
      } else if (holder.message_ != null && !holder.message_.isEmpty()) {
        DisplayPopupFragment(holder.message_, "Bad_Add_Product_Dialog");
      } else {
        Log.e("JLIHA::doAddToCartClick", "Unrecognized failed return.");
      }
    } else {
      AddLineItem(item);
    }
  }
  private void AddLineItem(ShoppingUtils.LineItem line_item) {
    if (line_item == null) return;
    Log.i("PHB TEMP", "JLIHA::AddLineItem. Adding line item:\n" +
            ShoppingUtils.PrintLineItemHumanReadable(line_item));

    // Need cookies and csrf_token to create server's cart.
    SharedPreferences user_info = getSharedPreferences(
            getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
    String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
    if (cookies.isEmpty()) {
      String username = user_info.getString(getString(R.string.ui_username), "");
      String password = user_info.getString(getString(R.string.ui_password), "");
      ShoppingUtils.RefreshCookies(
              this, username, password, ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK +
                      ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item));
      return;
    }

    // Shopping Cart should be initialized if we reach this point. Set order id from it.
    ShoppingCartActivity.CartAccessResponse response = new ShoppingCartActivity.CartAccessResponse();
    if (!ShoppingCartActivity.AccessCart(
            ShoppingCartActivity.CartAccessType.GET_ORDER_ID, response)) {
      Log.e("PHB ERROR", "JLIHA::AddLineItem. Failed to get order id.");
      return;
    }
    line_item.order_id_ = response.order_id_;
    Log.i("PHB TEMP", "JLIHA::AddLineItem. Just fetched order_id: " + line_item.order_id_);

    String csrf_token = user_info.getString(getString(R.string.ui_csrf_token), "");
    if (csrf_token.isEmpty()) {
      if (!ShoppingUtils.GetCsrfToken(
              this, cookies, ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
                      ShoppingUtils.TASK_CART_SEPARATOR + ShoppingUtils.PrintLineItem(line_item))) {
        // Nothing to do.
      }
      return;
    }

    if (line_item.order_id_ > 0) {
      // Update line-item in server's cart.
      if (!ShoppingUtils.UpdateLineItem(this, cookies, csrf_token, line_item)) {
        // Nothing to do.
      }
    } else {
      // Create new cart, and add line-item to it.
      if (!ShoppingUtils.CreateServerCart(this, cookies, csrf_token, line_item)) {
        // Nothing to do.
      }
    }
  }

  private void FetchFeaturedRewardsPage() {
    MakeFeaturedRewardsVisible(false);
    IncrementNumRequestsCounter();
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
    GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
    params.url_ = featured_rewards_url_;
    params.connection_type_ = "GET";
    params.extra_info_ = FETCH_FEATURED_REWARDS_PAGE_TASK;
    task.execute(params);
  }

  private void FetchFeaturedEarnPage() {
    MakeFeaturedEarnVisible(false);
    IncrementNumRequestsCounter();
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
    GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
    params.url_ = featured_earn_url_;
    params.connection_type_ = "GET";
    params.extra_info_ = FETCH_FEATURED_EARN_PAGE_TASK;
    task.execute(params);
  }

  private void FetchPrizeDrawings() {
    IncrementNumRequestsCounter();
    FetchRewardsPage(FETCH_REWARDS_PAGE_TASK);
  }

  private void MakeFeaturedRewardsVisible(boolean state) {
    TextView title = (TextView) findViewById(R.id.featured_rewards_title_tv);
    title.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    TextView title_two = (TextView) findViewById(R.id.featured_rewards_title_details_tv);
    title_two.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    featured_rewards_ll_.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    // Arrows will be handled interactively. Don't enable them here.
    if (state) return;
    TextView left_arrow = (TextView) findViewById(R.id.featured_rewards_left_tv);
    left_arrow.setVisibility(View.INVISIBLE);
    TextView right_arrow = (TextView) findViewById(R.id.featured_rewards_right_tv);
    right_arrow.setVisibility(View.INVISIBLE);
  }

  private void MakeFeaturedEarnVisible(boolean state) {
    TextView title = (TextView) findViewById(R.id.featured_earn_title_tv);
    title.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    TextView title_two = (TextView) findViewById(R.id.featured_earn_title_details_tv);
    title_two.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    featured_earn_ll_.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    // Arrows will be handled interactively. Don't enable them here.
    if (state) return;
    TextView left_arrow = (TextView) findViewById(R.id.featured_earn_left_tv);
    left_arrow.setVisibility(View.INVISIBLE);
    TextView right_arrow = (TextView) findViewById(R.id.featured_earn_right_tv);
    right_arrow.setVisibility(View.INVISIBLE);
  }

  private void SetFeaturedRewardsList(String webpage) {
    featured_rewards_ = new ArrayList<ProductsPageParser.ProductItem>();
    ProductsPageParser.ParseRewardsPage(webpage, featured_rewards_);
    if (featured_rewards_.isEmpty()) {
      Log.w("JLIHA::SetFeaturedRewardsList", "No Featured rewards found from webpage:\n" + webpage);
      MakeFeaturedRewardsVisible(false);
      return;
    }

    // Set products_list_.
    ProductsActivity.SetProductsList(webpage);

    // NOTE: Not taking year, as it made text too wide to appear on one line
    //DateFormat date_format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
    DateFormat date_format = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
    Calendar cal = Calendar.getInstance();
    TextView title = (TextView) findViewById(R.id.featured_rewards_title_tv);
    title.setText("FEATURED REWARDS for " + date_format.format(cal.getTime()) + " | ");

    featured_rewards_layouts_ = new ArrayList<View>(featured_rewards_.size());
    int position = 0;
    for (ProductsPageParser.ProductItem item : featured_rewards_) {
      View vi = (View) inflater_.inflate(R.layout.products_item, null);
      ProductsAdapter.ProductViewHolder view_holder = new ProductsAdapter.ProductViewHolder();
      view_holder.layout_ = (RelativeLayout) vi.findViewById(R.id.products_item_ll);
      view_holder.drawing_ = (TextView) vi.findViewById(R.id.product_drawing_title);
      view_holder.title_ = (TextView) vi.findViewById(R.id.product_title);
      view_holder.summary_ = (TextView) vi.findViewById(R.id.product_summary);
      view_holder.max_quantity_ = (TextView) vi.findViewById(R.id.product_max_quantity);
      view_holder.date_ = (TextView) vi.findViewById(R.id.product_date);
      view_holder.orig_price_usd_ = (TextView) vi.findViewById(R.id.product_orig_price_usd);
      view_holder.jact_price_usd_ = (TextView) vi.findViewById(R.id.product_jact_price_usd);
      view_holder.jact_price_second_line_ = (TextView) vi.findViewById(R.id.product_jact_price_second_line);
      view_holder.jact_price_point_icon_ = (ImageView) vi.findViewById(R.id.product_jact_price_points_icon);
      view_holder.jact_price_points_ = (TextView) vi.findViewById(R.id.product_jact_price_points);
      view_holder.pid_ = (TextView) vi.findViewById(R.id.product_pid);
      view_holder.img_ = (ImageView) vi.findViewById(R.id.product_image);
      view_holder.img_ll_ = (LinearLayout) vi.findViewById(R.id.product_thumbnail);
      view_holder.text_ll_ = (LinearLayout) vi.findViewById(R.id.product_text_info_ll);
      vi.setTag(view_holder);

      // Populate text fields with product info.
      // Set Minimum Text Width.
      ViewGroup.LayoutParams text_params = view_holder.text_ll_.getLayoutParams();
      text_params.width = 600;

      // Set Title.
      if (item.title_ != null) {
        view_holder.title_.setText(item.title_);
        view_holder.title_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      }

      // Set Summary.
      if (item.summary_ != null) {
        view_holder.summary_.setText(item.summary_);
        view_holder.summary_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      }

      // Set Max Quantity.
      if (item.max_quantity_ != null) {
        view_holder.max_quantity_.setText(item.max_quantity_);
      } else {
        view_holder.max_quantity_.setText("");
      }

      // Set Drawing Date.
      view_holder.drawing_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      if (item.date_ != null && !item.date_.isEmpty()) {
        view_holder.date_.setVisibility(View.VISIBLE);
        view_holder.date_.setText(item.date_);
        view_holder.date_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view_holder.date_.setGravity(Gravity.CENTER_HORIZONTAL);
      } else {
        view_holder.date_.setVisibility(View.GONE);
      }

      // Set PID.
      if (item.pid_ != null) {
        view_holder.pid_.setText(item.pid_);
      }

      // Set Original Price.
      ProductsAdapter.SetPrice(item, view_holder, this, true);
      view_holder.orig_price_usd_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

      // Set Price.
      ProductsAdapter.SetPrice(item, view_holder, this, false);
      view_holder.jact_price_second_line_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      view_holder.jact_price_usd_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      view_holder.jact_price_points_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

      // Set Image.
      if (item.img_url_ != null) {
        if (!featured_image_loader_.DisplayImage(item.img_url_, view_holder.img_, position)) {
          unfinished_rewards_elements_[position] = true;
        }
        ViewGroup.LayoutParams img_params = view_holder.img_.getLayoutParams();
        img_params.height = 350;
        img_params.width = 350;
      }

      featured_rewards_layouts_.add(vi);

      vi.setOnClickListener(this);
      featured_rewards_ll_.addView(vi, position);
      position++;
    }
    // Remove old content from featured_rewards, if present.
    for (int i = featured_rewards_ll_.getChildCount() - 1; i >= position; --i) {
      featured_rewards_ll_.removeViewAt(i);
    }
    MakeFeaturedRewardsVisible(true);
  }

  private void SetFeaturedEarnList(String webpage) {
    featured_earn_ = new ArrayList<EarnPageParser.EarnItem>();
    EarnPageParser.ParseEarnPage(webpage, featured_earn_);
    if (featured_earn_.isEmpty()) {
      Log.w("JLIHA::SetFeaturedEarnList", "No Earn items found from webpage:\n" + webpage);
      MakeFeaturedEarnVisible(false);
      return;
    }
    // NOTE: Not taking year, as it made text too wide to appear on one line
    //DateFormat date_format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
    DateFormat date_format = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
    Calendar cal = Calendar.getInstance();
    TextView title = (TextView) findViewById(R.id.featured_earn_title_tv);
    title.setText("FEATURED Earn Items for " + date_format.format(cal.getTime()) + " | ");

    featured_earn_layouts_ = new ArrayList<View>(featured_earn_.size());
    int position = 0;
    for (EarnPageParser.EarnItem item : featured_earn_) {
      View vi = (View) inflater_.inflate(R.layout.earn_item, null);
      EarnAdapter.EarnViewHolder view_holder = new EarnAdapter.EarnViewHolder();
      view_holder.layout_ = (LinearLayout) vi.findViewById(R.id.earn_item_ll);
      view_holder.title_ = (TextView) vi.findViewById(R.id.earn_title);
      view_holder.earn_now_ = (Button) vi.findViewById(R.id.earn_now_button);
      view_holder.points_ = (TextView) vi.findViewById(R.id.earn_points);
      view_holder.words_ = (TextView) vi.findViewById(R.id.earn_words);
      view_holder.nid_ = (TextView) vi.findViewById(R.id.earn_nid);
      view_holder.jact_icon_ = (ImageView) vi.findViewById(R.id.product_jact_price_points_icon);
      view_holder.img_ = (JactImageView) vi.findViewById(R.id.earn_image);
      vi.setTag(view_holder);

      // Hide Button to save vertical space
      view_holder.earn_now_.setVisibility(View.GONE);

      // Set Title.
      if (item.title_ != null) {
        view_holder.title_.setText(item.title_);
        view_holder.title_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        ViewGroup.LayoutParams text_params = view_holder.title_.getLayoutParams();
        text_params.width = 600;
      }

      // Set Points.
      view_holder.points_.setText(Integer.toString(item.earn_points_) + " Points");
      view_holder.points_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      view_holder.words_.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      //ViewGroup.LayoutParams icon_params = view_holder.jact_icon_.getLayoutParams();
      //icon_params.height = 12;
      //icon_params.width = 12;

      // Set Earn ID.
      view_holder.nid_.setText(Integer.toString(item.nid_));


      // Set Image.
      if (item.img_url_ != null) {
        if (!earn_image_loader_.DisplayImage(item.img_url_, view_holder.img_, position, true)) {
          unfinished_earn_elements_[position] = true;
        }
        view_holder.img_.SetUseCase(JactImageView.UseCase.EARN_IMAGE_THUMBNAIL);
        ViewGroup.LayoutParams img_params = view_holder.img_.getLayoutParams();
        img_params.height = 250;
        img_params.width = 250;
        //view_holder.img_.SetBounds(50, 200, 50, 200);
        view_holder.img_.SetUseCase(JactImageView.UseCase.FEATURED_EARN);
      }

      featured_earn_layouts_.add(vi);

      vi.setOnClickListener(this);
      featured_earn_ll_.addView(vi, position);
      position++;
    }

    // Remove old content from featured_rewards, if present.
    for (int i = featured_earn_ll_.getChildCount() - 1; i >= position; --i) {
      featured_earn_ll_.removeViewAt(i);
    }
    MakeFeaturedEarnVisible(true);
  }

  public void doEarnNowVideoClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent().getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
    int nid = -1;
    try {
      nid = Integer.parseInt(nid_str);
    } catch (NumberFormatException e) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
      return;
    }

    String youtube_url = GetYoutubeUrlViaNodeId(nid);
    if (youtube_url.isEmpty()) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else {
      StartYoutubeActivity(youtube_url, nid);
    }
  }

  public void doEarnNowClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    TextView nid_tv = (TextView) ((LinearLayout) view.getParent()).findViewById(R.id.earn_nid);
    String nid_str = nid_tv.getText().toString();
    int nid = -1;
    try {
      nid = Integer.parseInt(nid_str);
    } catch (NumberFormatException e) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to parse nid " + nid_str + ":" + e.getMessage());
      Popup("Unable to find Video", "Try again later.");
      return;
    }

    String youtube_url = GetYoutubeUrlViaNodeId(nid);
    if (youtube_url.isEmpty()) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + nid + " in earn_list_");
      Popup("Unable to find Video", "Try again later.");
    } else {
      StartYoutubeActivity(youtube_url, nid);
    }
  }

  private String GetYoutubeUrlViaNodeId(int nid) {
    if (featured_earn_ == null) {
      Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Null nid");
      return "";
    }

    // Look up nid.
    for (EarnPageParser.EarnItem item : featured_earn_) {
      if (item.nid_ == nid) {
        return item.youtube_url_;
      }
    }
    Log.e("EarnActivity::GetYoutubeUrlViaNodeId", "Unable to find nid " + Integer.toString(nid));
    return "";
  }

  private void StartYoutubeActivity(String youtube_id, int nid) {
    Intent youtube_intent = new Intent(this, YouTubePlayerActivity.class);
    Log.w("PHB TEMP", "Setting youtube id: " + youtube_id);
    youtube_intent.putExtra(getString(R.string.youtube_id), youtube_id);
    YouTubePlayerActivity.SetEarnId(nid);
    startActivity(youtube_intent);
  }

  private void Popup(String title, String message) {
    DisplayPopupFragment(title, message, title);
  }

  public void doSeeAllRewardsClick(View view) {
    FetchRewardsPage(FETCH_AND_START_REWARDS_TASK);
    fadeAllViews(true);
  }

  private void FetchRewardsPage(String extra_info) {
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
    GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
    params.url_ = rewards_url_;
    params.connection_type_ = "GET";
    params.extra_info_ = extra_info;
    task.execute(params);
  }

  private void ParseRewardsPage(String webpage) {
    ArrayList<ProductsPageParser.ProductItem> list = new ArrayList<ProductsPageParser.ProductItem>();
    ProductsPageParser.ParseRewardsPage(webpage, list);
    SetNextDrawingBar(GetNextDrawingDate(list));
  }

  private String GetNextDrawingDate(ArrayList<ProductsPageParser.ProductItem> list) {
    if (list == null) return "";

    // Get current date (don't show drawings that have already passed).
    Calendar cal = Calendar.getInstance();
    Date current_date = null;
    try {
      DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
      current_date = date_format.parse(date_format.format(cal.getTime()));
    } catch (ParseException e) {
      Log.e("JLIHA::GetNextDrawingDate",
            "ParseException for date_to_parse " + cal.getTime() + ": " + e.getMessage());
      return "";
    }

    // Loop through all Rewards, picking out just the drawing date items; among those,
    // find the one with the earliest date.
    Date earliest_date = null;
    for (ProductsPageParser.ProductItem item : list) {
      if (item.date_ == null || item.date_.isEmpty()) continue;
      int prefix = item.date_.indexOf(DATE_PREFIX);
      String date_to_parse = item.date_;
      if (prefix >= 0) {
        date_to_parse = item.date_.substring(prefix + DATE_PREFIX.length());
      }
      try {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        Date drawing_date = format.parse(date_to_parse);
        // Check drawing date against current date.
        if (!drawing_date.after(current_date)) continue;
        // No Drawing Date yet set. Set this as current 'earliest date', and continue.
        if (earliest_date == null) {
          earliest_date = drawing_date;
        } else if (!drawing_date.after(earliest_date)) {
          earliest_date = drawing_date;
        }
      } catch (ParseException e) {
        Log.e("JLIHA::GetNextDrawingDate",
              "ParseException for date_to_parse " + date_to_parse + ": " + e.getMessage());
        return "";
      }
    }
    if (earliest_date == null) return "";
    DateFormat print_format = new SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH);
    return print_format.format(earliest_date);
  }

  private void SetNextDrawingBar(String next_drawing_date) {
    if (next_drawing_date.isEmpty()) {
      RelativeLayout next_drawing_bar = (RelativeLayout) findViewById(R.id.featured_prizes_ll);
      ViewGroup.LayoutParams bar_params = next_drawing_bar.getLayoutParams();
      bar_params.height = 0;
    } else {
      RelativeLayout next_drawing_bar = (RelativeLayout) findViewById(R.id.featured_prizes_ll);
      ViewGroup.LayoutParams bar_params = next_drawing_bar.getLayoutParams();
      bar_params.height = next_drawing_bar_height_;
      TextView next_date = (TextView) findViewById(R.id.featured_prizes_tv);
      next_date.setText("Next Prize Drawing: " + next_drawing_date);
    }
  }

  private void StartRewardsActivity(String webpage) {
    Intent products_activity = new Intent(this, ProductsActivity.class);
    products_activity.putExtra("server_response", webpage);
    startActivity(products_activity);
  }

  private void StartRewardsPrizeDrawingsActivity(String webpage) {
    Intent products_activity = new Intent(this, ProductsActivity.class);
    products_activity.putExtra("server_response", webpage);
    products_activity.putExtra("to_display", "prize_drawings");
    startActivity(products_activity);
  }

  public void doSeeAllEarnClick(View view) {
    EarnActivity.SetShouldRefreshEarnItems(true);
    Intent earn_intent = new Intent(this, EarnActivity.class);
    earn_intent.putExtra(getString(R.string.go_to_earn_main_page), "true");
    startActivity(earn_intent);
  }

  public void doPopupDismissClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    featured_rewards_listener_.DismissClick();
  }

  private void SetLoginTrue() {
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.was_logged_off_key), "false");
    editor.commit();
  }

  protected void RewardsPositionsReady(HashSet<Integer> positions) {
    if (positions == null) {
      Log.e("JLIHA::AlertPositionsReady", "Null positions");
      return;
    }
    if (positions.isEmpty()) {
      Log.e("JLIHA::AlertPositionsReady", "Empty positions");
      return;
    }
    for (Integer i : positions) {
      int position = i.intValue();
      if (unfinished_rewards_elements_[position]) {
        unfinished_rewards_elements_[position] = false;
        // Update Image View with the now loaded image.
        if (featured_rewards_layouts_ != null && featured_rewards_layouts_.size() > position &&
                featured_rewards_ != null && featured_rewards_.size() > position) {
          ProductsAdapter.ProductViewHolder holder =
                  (ProductsAdapter.ProductViewHolder) featured_rewards_layouts_.get(position).getTag();
          featured_image_loader_.DisplayImage(
                  featured_rewards_.get(position).img_url_,
                  holder.img_, position);
          featured_rewards_layouts_.get(position).invalidate();
        } else if (featured_rewards_layouts_ == null) {
          Log.e("JLIHA::AlertPositionsReady", "Null Featured Rewards Layouts");
        } else if (featured_rewards_ == null) {
          Log.e("JLIHA::AlertPositionsReady", "Null Products List");
        } else {
          Log.e("JLIHA::AlertPositionsReady",
                  "Position: " + position + "featured_rewards_layouts_.size(): " +
                          featured_rewards_layouts_.size() + ", featured_rewards_.size(): " +
                          featured_rewards_.size());
        }
      }
    }
  }

  private class FeaturedRewardsImageLoader implements AdapterWithImages {
    @Override
    public void AlertPositionsReady(HashSet<Integer> positions) {
      RewardsPositionsReady(positions);
    }
    @Override
    public Drawable GetDrawable(int resource_id) {
      return GetResourceDrawable(resource_id);
    }
    @Override
    public Drawable GetDrawable(Bitmap bitmap) {
      return GetBitmapDrawable(bitmap);
    }
  }

  protected void EarnPositionsReady(HashSet<Integer> positions) {
    if (positions == null) {
      Log.e("JLIHA::AlertPositionsReady", "Null positions");
      return;
    }
    if (positions.isEmpty()) {
      Log.e("JLIHA::AlertPositionsReady", "Empty positions");
      return;
    }
    for (Integer i : positions) {
      int position = i.intValue();
      if (unfinished_earn_elements_[position]) {
        unfinished_earn_elements_[position] = false;
        // Update Image View with the now loaded image.
        if (featured_earn_layouts_ != null && featured_earn_layouts_.size() > position &&
            featured_earn_ != null && featured_earn_.size() > position) {
          EarnAdapter.EarnViewHolder holder =
                    (EarnAdapter.EarnViewHolder) featured_earn_layouts_.get(position).getTag();
          if (holder != null && holder.img_ != null) {
            earn_image_loader_.DisplayImage(
                    featured_earn_.get(position).img_url_,
                    holder.img_, position, true);
            featured_earn_layouts_.get(position).invalidate();
          } else {
            Log.e("PHB TEMP", "JLIHA::AlertPositionsReady. Earn image is null for pos: " + position);
          }
        } else if (featured_earn_layouts_ == null) {
          Log.e("JLIHA::AlertPositionsReady", "Null Featured Earn Layouts");
        } else if (featured_earn_ == null) {
          Log.e("JLIHA::AlertPositionsReady", "Null Earn List");
        } else {
          Log.e("JLIHA::AlertPositionsReady",
                "Position: " + position + "featured_earn_layouts_.size(): " +
                featured_earn_layouts_.size() + ", featured_earn_.size(): " +
                featured_earn_.size());
        }
      }
    }
  }

  protected Drawable GetResourceDrawable(int resource_id) {
    return getResources().getDrawable(resource_id);
  }

  protected Drawable GetBitmapDrawable(Bitmap bitmap) {
    return new BitmapDrawable(getResources(), bitmap);
  }

  private class FeaturedEarnImageLoader implements AdapterWithImages {
    @Override
    public void AlertPositionsReady(HashSet<Integer> positions) {
      EarnPositionsReady(positions);
    }
    @Override
    public Drawable GetDrawable(int resource_id) {
      return GetResourceDrawable(resource_id);
    }
    @Override
    public Drawable GetDrawable(Bitmap bitmap) {
      return GetBitmapDrawable(bitmap);
    }
  }

// =============================================================================
// GCM Methods.
//=============================================================================
  
  /**
   * Check the device to make sure it has the Google Play Services APK. If
   * it doesn't, display a dialog that allows users to download the APK from
   * the Google Play Store or enable it in the device's system settings.
   */
  private boolean CheckPlayServices() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(
        	resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      } else {
        Log.i("JactLoginActivity::CheckPlayServices", "This device is not supported.");
        finish();
      }
      return false;
    }
    return true;
  }
  
  /**
   * Gets the current registration ID for application on GCM service.
   * <p>
   * If result is empty, the app needs to register.
   *
   * @return registration ID, or empty string if there is no existing
   *         registration ID.
   */
  private String GetRegistrationId(Context context) {
      final SharedPreferences prefs = GetGCMPreferences(context);
      String registrationId = prefs.getString(PROPERTY_REG_ID, "");
      if (registrationId.isEmpty()) {
          Log.i("JactLoginActivity::CheckPlayServices", "Registration not found.");
          return "";
      }
      // Check if app was updated; if so, it must clear the registration ID
      // since the existing registration ID is not guaranteed to work with
      // the new app version.
      int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
      int currentVersion = GetAppVersion(context);
      if (registeredVersion != currentVersion) {
          Log.i("JactLoginActivity::CheckPlayServices", "App version changed.");
          return "";
      }
      return registrationId;
  }

  /**
   * @return Application's {@code SharedPreferences}.
   */
  private SharedPreferences GetGCMPreferences(Context context) {
      // This sample app persists the registration ID in shared preferences, but
      // how you store the registration ID in your app is up to you.
      return getSharedPreferences(JactLoginActivity.class.getSimpleName(),
              Context.MODE_PRIVATE);
  }
  
  /**
   * @return Application's version code from the {@code PackageManager}.
   */
  private static int GetAppVersion(Context context) {
      try {
        PackageInfo packageInfo =
        	context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionCode;
      } catch (NameNotFoundException e) {
        // should never happen
        throw new RuntimeException("Could not get package name: " + e.getMessage());
      }
  }
  
  /**
   * Registers the application with GCM servers asynchronously.
   * <p>
   * Stores the registration ID and app versionCode in the application's
   * shared preferences.
   */
  private void RegisterInBackground() {
    new AsyncTask<Void, String, String>() {
        @Override
        protected String doInBackground(Void... params) {
          Log.e("PHB TEMP", "JLHA::RegisterInBackground::doInBackground.");
          String msg = "";
          try {
            if (gcm_ == null) {
              gcm_ = GoogleCloudMessaging.getInstance(getApplicationContext());
            }
            String regid = gcm_.register(SENDER_ID);
            msg = "Device registered, registration ID=" + regid;

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // The request to your server should be authenticated if your app
            // is using accounts.
            SendRegistrationIdToBackend();

            // For this demo: we don't need to send it because the device
            // will send upstream messages to a server that echo back the
            // message using the 'from' address in the message.

            // Persist the registration ID - no need to register again.
            StoreRegistrationId(getApplicationContext(), regid);
          } catch (IOException ex) {
            msg = "Error :" + ex.getMessage();
            // If there is an error, don't just keep trying to register.
            // Require the user to click a button again, or perform
            // exponential back-off.
          }
          return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
          Log.e("PHB TEMP", "JLHA::RegisterInBackground::onPostExecute. msg: " + msg);
          //mDisplay.append(msg + "\n");
        }
    }.execute(null, null, null);
  }
  
  /**
   * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
   * or CCS to send messages to your app. Not needed for this demo since the
   * device sends upstream messages to a server that echoes back the message
   * using the 'from' address in the message.
   */
  private void SendRegistrationIdToBackend() {
	Log.e("PHB TEMP", "JLHA::SendRegistrationIdToBackend");
	Intent registration_intent = new Intent("com.google.android.c2dm.intent.REGISTER");
	registration_intent.putExtra(
		"app", PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(), 0));
	registration_intent.putExtra("sender", jact_user_name_);
	getApplicationContext().startService(registration_intent);
  }
  
  /**
   * Stores the registration ID and app versionCode in the application's
   * {@code SharedPreferences}.
   *
   * @param context application's context.
   * @param regId registration ID
   */
  private void StoreRegistrationId(Context context, String regId) {
	  Log.e("PHB TEMP", "JLHA::StoreRegistrationId");
      final SharedPreferences prefs = GetGCMPreferences(context);
      int appVersion = GetAppVersion(context);
      Log.i("JactLoginActivity::CheckPlayServices", "Saving regId on app version " + appVersion);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString(PROPERTY_REG_ID, regId);
      editor.putInt(PROPERTY_APP_VERSION, appVersion);
      editor.commit();
  }
  
  // Temporarily testing sending upstream message from App to Jact GCM.
  public void doSendUpstreamMessageClick(final View view) {
	new AsyncTask<Void, String, String>() {
	    @Override
	    protected String doInBackground(Void... params) {
	      Log.e("PHB TEMP", "JLHA::doSendUpstreamMessageClick");
	      String msg = "";
	      try {
	        Bundle data = new Bundle();
	        data.putString("my_message", "Hello World");
	        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
	        String id = Integer.toString(msg_id_.incrementAndGet());
	        gcm_.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	        msg = "Sent message";
	      } catch (IOException ex) {
	        msg = "Error :" + ex.getMessage();
	      }
	      return msg;
	    }

	    @Override
	    protected void onPostExecute(String msg) {
	      Log.e("PHB TEMP", "JLHA::onSendUpstreamMessageClick::onPostExecute. msg: " + msg);
	      //mDisplay.append(msg + "\n");
	    }
	}.execute(null, null, null);
  }

// =============================================================================
// END GCM Methods.
// =============================================================================
 
  
// =============================================================================
// ProcessUrlResponse Override Methods.
// =============================================================================

  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    if (extra_params.isEmpty()) {
      Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: JactLoggedInHomeActivity has multiple calls " +
              "to GetUrlTask; in order to properly handle the response, " +
              "must specify desired action via extra_params");
    } else if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_GET_CART_TASK) == 0) {
  	  SaveCookies(cookies);
  	  GetCart(this);
  	} else if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
      if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
        // TODO(PHB): Handle this gracefully (popup a dialog).
        Log.e("PHB ERROR", "JactActionBarActivity::ProcessCartResponse. Unable to parse cart response:\n" + webpage);
      }
    } else if (extra_params.indexOf(FETCH_REWARDS_PAGE_TASK) == 0) {
      ParseRewardsPage(webpage);
    } else if (extra_params.indexOf(FETCH_AND_START_REWARDS_TASK) == 0) {
      StartRewardsActivity(webpage);
    } else if (extra_params.indexOf(FETCH_REWARDS_PRIZE_DRAWINGS_TASK) == 0) {
      StartRewardsPrizeDrawingsActivity(webpage);
    } else if (extra_params.indexOf(FETCH_FEATURED_REWARDS_PAGE_TASK) == 0) {
      SetFeaturedRewardsList(webpage);
    } else if (extra_params.indexOf(FETCH_FEATURED_EARN_PAGE_TASK) == 0) {
      SetFeaturedEarnList(webpage);
    } else if (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
               extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0) {
      try {
        JSONObject line_item = new JSONObject(webpage);
        ArrayList<ShoppingUtils.LineItem> new_line_items = new ArrayList<ShoppingUtils.LineItem>();
        if (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 &&
                !ShoppingUtils.ParseLineItemFromAddLineItemPage(line_item, new_line_items)) {
          // TODO(PHB): Handle this error (e.g. popup warning to user).
          Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
          return;
        } else if (extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 &&
                !ShoppingUtils.ParseLineItemFromUpdateLineItemPage(line_item, new_line_items)) {
          // TODO(PHB): Handle this error (e.g. popup warning to user).
          Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Unable to parse add line-item response:\n" + webpage);
          return;
        }
        if (new_line_items.size() != 1 ||
                !ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.UPDATE_LINE_ITEM,
                        new_line_items.get(0))) {
          // TODO(PHB): Handle this error (e.g. popup warning to user).
          Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Unable to parse cart response. " +
                  "Num new line items: " + new_line_items.size() + "; Webpage response:\n" + webpage);
          return;
        }
        DisplayPopupFragment("Added Item to Cart", "Finally_added_item");
        SetCartIcon(this);
        fadeAllViews(false);
      } catch (JSONException e) {
        // TODO(PHB): Handle this error (e.g. popup warning to user).
        Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Unable to parse add line-item response " +
                "from server. Exception: " + e.getMessage() + "; webpage response:\n" + webpage);
        return;
      }
    } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
            (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0 ||
             extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
             extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0)) {
      String parsed_line_item = extra_params.substring(
              extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
                      ShoppingUtils.TASK_CART_SEPARATOR.length());
      if (extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
        SaveCookies(cookies);
      } else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) == 0) {
        SaveCsrfToken(webpage);
      } else if (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0) {
        if (!ShoppingCartActivity.AccessCart(ShoppingCartActivity.CartAccessType.SET_CART_FROM_WEBPAGE, webpage)) {
          // TODO(PHB): Handle this gracefully (pop-up a dialog).
          Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Create_cart: Unable to parse cart response:\n" + webpage);
          return;
        }
      }
      AddLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
    } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
      ProcessCartResponse(this, webpage, cookies, extra_params);
      return;
  	} else {
      Log.e("JLIHA::ProcessUrlResponse",
            "Unrecognized extra params: " + GetUrlTask.PrintExtraParams(extra_params));
    }
    num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  SetLoginTrue();
	  SetCartIcon(this);
	  if (num_server_tasks_ == 0) {
	    fadeAllViews(false);
	  }
	}
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
    if (extra_params.isEmpty()) {
      Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: JactLoggedInHomeActivity has multiple calls " +
                         "to GetUrlTask; in order to properly handle the response, " +
      		             "must specify desired action via extra_params");
    } else {
        Log.e("PHB ERROR", "JLIHA::ProcessUrlResponse. Error: Unrecognized extra params: " +
                           GetUrlTask.PrintExtraParams(extra_params));
    }
    num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  SetLoginTrue();
	  SetCartIcon(this);
	  if (num_server_tasks_ == 0) {
	    fadeAllViews(false);
	  }
	}
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	// TODO(PHB): Implement this.
	Log.e("PHB ERROR", "JLIHA::ProcessFailedResponse. Status: " + status);
    if (extra_params.indexOf(ShoppingUtils.GET_CART_TASK) == 0) {
      GetCookiesThenGetCart(this);
      return;
    } else if (status == GetUrlTask.FetchStatus.ERROR_CSRF_FAILED) {
      if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
              (extra_params.indexOf(ShoppingUtils.CREATE_CART_TASK) == 0 ||
               extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
               extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0)) {
        String parsed_line_item = extra_params.substring(
                extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
                ShoppingUtils.TASK_CART_SEPARATOR.length());
        String task = ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK +
                ShoppingUtils.TASK_CART_SEPARATOR + parsed_line_item;
        SharedPreferences user_info = getSharedPreferences(
                getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
        String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
        if (!ShoppingUtils.GetCsrfToken(this, cookies, task)) {
          // Nothing to do.
        }
      } else {
        Log.e("PHB TEMP", "JLIHA::ProcessFailedResponse. Status: " + status +
              "; extra_params: " + GetUrlTask.PrintExtraParams(extra_params));
      }
    } else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) >= 0) {
      SharedPreferences user_info = getSharedPreferences(
              getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      ShoppingUtils.GetCsrfToken(this, cookies, extra_params);
    } else if (extra_params.indexOf(FETCH_FEATURED_REWARDS_PAGE_TASK) == 0) {
      Log.e("JLIHA::ProcessFailedResponse", "Failed to fetch Featured Rewards Page.");
    } else if (extra_params.indexOf(FETCH_REWARDS_PAGE_TASK) == 0) {
      Log.e("JLIHA::ProcessFailedResponse", "Failed to fetch Rewards Page.");
    } else if (extra_params.indexOf(FETCH_AND_START_REWARDS_TASK) == 0) {
      Log.e("JLIHA::ProcessFailedResponse", "Failed to fetch (and start) Rewards Page.");
    } else if (extra_params.indexOf(FETCH_FEATURED_EARN_PAGE_TASK) == 0) {
      Log.e("JLIHA::ProcessFailedResponse", "Failed to fetch Featured Earn Page.");
    } else if (extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) > 0 &&
               (extra_params.indexOf(ShoppingUtils.ADD_LINE_ITEM_TASK) == 0 ||
                extra_params.indexOf(ShoppingUtils.UPDATE_LINE_ITEM_TASK) == 0 ||
                extra_params.indexOf(ShoppingUtils.GET_COOKIES_THEN_UPDATE_LINE_ITEM_TASK) >= 0)) {
      String parsed_line_item = extra_params.substring(
              extra_params.indexOf(ShoppingUtils.TASK_CART_SEPARATOR) +
                      ShoppingUtils.TASK_CART_SEPARATOR.length());
      AddLineItem(ShoppingUtils.ParseLineItem(parsed_line_item));
    } else if (extra_params.indexOf(ShoppingUtils.GET_CSRF_THEN_UPDATE_LINE_ITEM_TASK) >= 0) {
      SharedPreferences user_info = getSharedPreferences(
              getString(R.string.ui_master_file), Activity.MODE_PRIVATE);
      String cookies = user_info.getString(getString(R.string.ui_session_cookies), "");
      ShoppingUtils.GetCsrfToken(this, cookies, extra_params);
    } else if (extra_params.equalsIgnoreCase(USER_POINTS)) {
      ProcessFailedCartResponse(this, status, extra_params);
      return;
    } else {
      Log.e("JLIHA::ProcessFailedResponse", "Failed for Unhandled task: " +
                                            GetUrlTask.PrintExtraParams(extra_params));
    }
    num_server_tasks_--;
	if (num_server_tasks_ == 0) {
	  fadeAllViews(false);
	  SetLoginTrue();
	}
  }
}
// =============================================================================
// END ProcessUrlResponse Override Methods.
// =============================================================================
