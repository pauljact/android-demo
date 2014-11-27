package com.example.jactfirstdemo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class JactLoggedInHomeActivity extends ActionBarActivity implements ProcessUrlResponseCallback {
  public enum OnTaskCompleted {
    PROFILE_PICTURE,
    USER_POINTS,
    POINTS_PICTURE,
    PRIZE_DRAWING,
    USER_VIEW
  }
  
  static java.net.CookieManager msCookieManager = new java.net.CookieManager();
  List<String> cookie_headers_;
  private static String jact_website_ = "http://us7.jact.com:3080/";
  private static final int drawer_pos_ = 0;
  private int drawer_click_pos_;
  private String jact_session_name_;
  private String jact_session_id_;
  private String jact_token_;
  private String jact_user_;
  private String jact_user_id_;
  private String jact_user_name_;
  private String jact_user_email_;
  private String jact_user_signature_;
  private String jact_user_pic_;
  private String jact_profile_pic_url_;
  private Bitmap profile_pic_bitmap_;
  private Bitmap userpoints_pic_bitmap_;

  private DrawerLayout drawer_layout_;
  private ListView drawer_list_view_;
  private String[] list_titles_;
  private ActionBarDrawerToggle drawer_toggle_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set layout.
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drawer_layout);
    //PHBsetContentView(R.layout.jact_logged_in_home_screen);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Set Navigation Drawer.
    drawer_click_pos_ = -1;
    drawer_layout_ = (DrawerLayout) findViewById(R.id.drawer_layout);
    // Toggles the icon that gets displayed in left side of Action Bar
    // (the "UP" button, also the menu button), based on whether the
    // drawer is open or closed.
    drawer_toggle_ = new ActionBarDrawerToggle(
        this, drawer_layout_,
        0,    // Needed for Accessibility
        0);  // Needed for Accessibility
    drawer_layout_.setDrawerListener(drawer_toggle_);
    drawer_list_view_ = (ListView) findViewById(R.id.left_drawer);
    //list_titles_ = getResources().getStringArray(R.array.planets_array);
    //drawer_list_view_.setAdapter(new ArrayAdapter<String>(
    //    this, R.layout.drawer_list_item, list_titles_));
    IconAndText menu_data[] = new IconAndText[] {
        new IconAndText(R.drawable.profile, "My Profile"),
        new IconAndText(R.drawable.games, "Games"),
        new IconAndText(R.drawable.trophy, "Rewards"),
        new IconAndText(R.drawable.community, "Community")
    };
    drawer_list_view_.setAdapter(new JactMenuAdapter(
        this, R.layout.drawer_list_icon, menu_data));
    drawer_list_view_.setOnItemClickListener(new DrawerItemClickListener(this));
    
    // Get session cookies.
    Intent intent = getIntent();
    String cookies_headers_string = intent.getStringExtra("session_cookies");
    //PHBcookie_headers_ = Arrays.asList(cookies_headers_string.split("_|_"));
    
    // Parse User Info from log in response.
    //PHBGetUserInfoFromResponse(intent.getStringExtra("server_login_response"));
    // Set User Name.
    //PHBSetUserName();
    // Set Profile Picture.
    //PHBnew GetUrlTask(OnTaskCompleted.PROFILE_PICTURE).execute(jact_profile_pic_url_);
    // Set User Points Icon.
    //PHBnew GetUrlTask(OnTaskCompleted.POINTS_PICTURE).execute(
    //PHB    jact_website_ + "sites/all/themes/jact_7/images/bux-symbol.gif");
    // Set User Points.
    //PHBnew GetUrlTask(OnTaskCompleted.USER_POINTS).execute(
    //PHB    jact_website_ + "rest/userpoints/" + jact_user_id_ + ".json");
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    drawer_toggle_.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawer_toggle_.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu items for use in the action bar.
    getMenuInflater().inflate(R.menu.action_bar, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // This ID represents the Home or Up button. In the case of this
        // activity, the Up button is shown. Use NavUtils to allow users
        // to navigate up one level in the application structure. For
        // more details, see the Navigation pattern on Android Design:
        //
        // http://developer.android.com/design/patterns/navigation.html#up-vs-back
        //
        //PHB NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    if (drawer_toggle_.onOptionsItemSelected(item)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void GetUserInfoFromResponse(String response) {
    try {
      JSONObject response_json = new JSONObject(response);
      jact_session_name_ = response_json.getString("session_name");
      jact_session_id_ = response_json.getString("sessid");
      jact_token_ = response_json.getString("token");
      jact_user_ = response_json.getString("user");
      JSONObject user_json = new JSONObject(jact_user_);
      jact_user_id_ = user_json.getString("uid");
      jact_user_name_ = user_json.getString("name");
      jact_user_email_ = user_json.getString("mail");
      jact_user_signature_ = user_json.getString("signature");
      jact_user_pic_ = user_json.getString("picture");
      JSONObject picture_info= new JSONObject(jact_user_pic_);
      jact_profile_pic_url_ = picture_info.getString("url");
    } catch (JSONException e) {
      Log.e("bar", "Error: Unable to parse Login JSON.");
      // TODO(PHB): Handle exception gracefully.
    }
  }

  private void SetUserName() {
    TextView tv =
        (TextView) this.findViewById(R.id.logged_in_home_welcome_textview);
    if (!jact_user_name_.isEmpty()) {
      tv.setText("Welcome, " + jact_user_name_);
    } else {
      tv.setText("Welcome, Unknown Jact User");
    }
  }
  
  public class IconAndText {
    public int icon_res_id_;
    public String text_;

    public IconAndText() {}
    public IconAndText(int icon_id, String text) {
      icon_res_id_ = icon_id;
      text_ = text;
    }
  }

  static class IconAndTextView {
    ImageView img_view_;
    TextView text_view_;
  }

  public class JactMenuAdapter extends ArrayAdapter<IconAndText> {
    Context context_;
    private int layout_res_id_;
    private IconAndText items_[] = null;

    public JactMenuAdapter(Context context, int layout_id, IconAndText[] data) {
      super(context, layout_id, data);
      this.context_ = context;
      this.layout_res_id_ = layout_id;
      this.items_ = data;
    }

    @Override
    public View getView(int position, View convert_view, ViewGroup parent) {
      View row = convert_view;
      IconAndTextView views = null;
      if (row == null) {
        row = ((Activity) context_).getLayoutInflater().inflate(layout_res_id_, parent, false);
        views = new IconAndTextView();
        views.img_view_ = (ImageView) row.findViewById(R.id.drawer_img_view);
        views.text_view_ = (TextView) row.findViewById(R.id.drawer_text_view);
        row.setTag(views);
      } else {
        views = (IconAndTextView) row.getTag();
      }
      IconAndText list_item = items_[position];
      views.img_view_.setImageResource(list_item.icon_res_id_);
      views.text_view_.setText(list_item.text_);
      return row;
    }
  }

  /**
   * Fragment that appears in the "content_frame", shows a planet
   */
/*  public static class PlanetFragment extends Fragment {
    public static final String ARG_PLANET_NUMBER = "planet_number";

    public PlanetFragment() {
      // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_planet, container, false);
      int i = getArguments().getInt(ARG_PLANET_NUMBER);
      String planet = getResources().getStringArray(R.array.planets_array)[i];
      int imageId = getResources().getIdentifier(
          planet.toLowerCase(Locale.getDefault()),
          "drawable",
          getActivity().getPackageName());
      ((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
      getActivity().setTitle(planet);
      return rootView;
    }
  }
  // Swaps fragments in the main content view
  private void selectItem(int position) {
    // Create a new fragment and specify the planet to show based on position.
    Fragment fragment = new PlanetFragment();
    Bundle args = new Bundle();
    args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
    fragment.setArguments(args);

    // Insert the fragment by replacing any existing fragment
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction().
        replace(R.id.content_frame, fragment).commit();

    // Highlight the selected item, update the title, and close the drawer
    drawer_list_view_.setItemChecked(position, true);
    getSupportActionBar().setTitle(list_titles_[position]);
  }
*/
  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    private JactLoggedInHomeActivity parent_class_;
    
    public DrawerItemClickListener(JactLoggedInHomeActivity a) {
      parent_class_ = a;
    }
    
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
      drawer_click_pos_ = position;
      if (position == drawer_pos_) {
        // Nothing to do except close drawer.
      } else if (position == 0) {
        // Position '0' is for 'My Profile', which matches current activity.
    	// Note it is an error if code reaches here since drawer_num_ = 0,
    	// so should've entered above condtion.
        Log.e("PHB Error", "Error: Unexpected drawer position.");
      } else if (position == 1) {
        // Position '1' is for Games. Start that activity.
    	String url = "http://us7.jact.com:3080/rest/views/jact_prize_drawings.json";
    	new GetUrlTask(parent_class_, GetUrlTask.TargetType.JSON).execute(url, "GET");
      } else if (position == 2) {
        // Position '2' is for Rewards. Start that activity.
      	String url = "http://us7.jact.com:3080/rest/views/jact_prize_drawings.json";
      	new GetUrlTask(parent_class_, GetUrlTask.TargetType.JSON).execute(url, "GET");
      } else if (position == 3) {
        // Position '3' is for Community. Start that activity.
      	String url = "http://us7.jact.com:3080/rest/views/jact_prize_drawings.json";
      	new GetUrlTask(parent_class_, GetUrlTask.TargetType.JSON).execute(url, "GET");
      } else {
        // Only 4 drawers expected. Error.
        Log.e("PHB Error", "Error: unexpected position: " + position);
      }
      drawer_layout_.closeDrawer(drawer_list_view_);
    }
  }

/*
  private class GetUrlTask extends AsyncTask<String, Void, Void> {
    InputStream is_;
    OnTaskCompleted task_done_;
    Bitmap picture_;
    String webpage_;

    // Constructor; sets task_done_
    public GetUrlTask(OnTaskCompleted task) {
      is_ = null;
      picture_ = null;
      webpage_ = "";
      task_done_ = task;
    }

    protected Void doInBackground(String... params) {
      Log.e("foo", "doInBackground for: " + task_done_.toString());
      if (params[0].isEmpty()) { return null; }
      try {
       	URL url = new URL(params[0]);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
       	connection.setReadTimeout(20000);
       	connection.setConnectTimeout(25000);
       	connection.setRequestMethod("GET");
       	connection.setDoInput(true);
        // Set cookies, if this task requires a logged-in user view.
        if (task_done_ == OnTaskCompleted.USER_VIEW &&
            !cookie_headers_.isEmpty()) {
          for (String cookie : cookie_headers_) {
        	Log.e("foo", "cookie: " + cookie);
            msCookieManager.getCookieStore().add(
                null, HttpCookie.parse(cookie).get(0));
          }               
          if (msCookieManager.getCookieStore().getCookies().size() > 0) {
            connection.setRequestProperty(
                "Cookie",
                TextUtils.join(",", msCookieManager.getCookieStore().getCookies()));   
          }
        }
        connection.connect();

        int response = connection.getResponseCode();
       	if (response != 200) {
          if (response == 403) {
            // TODO(PHB): Handle error here.
            Log.e("bar", "Error: Server responded with 403 for task: " +
                         task_done_.toString() + ", likely because logged-" +
                         "in credentials (cookies) were not properly " +
                         "transferred to the server.");
            return null;
          } else {
            // TODO(PHB): Handle error here.
            Log.e("bar", "Bad response (" + response + ") for task: " + task_done_.toString());
            return null;
          }
       	}
       	is_ = connection.getInputStream();
        if (task_done_ == OnTaskCompleted.PROFILE_PICTURE ||
            task_done_ == OnTaskCompleted.POINTS_PICTURE) {
          picture_ = BitmapFactory.decodeStream(is_);
        } else if (task_done_ == OnTaskCompleted.USER_POINTS ||
                   task_done_ == OnTaskCompleted.PRIZE_DRAWING ||
                   task_done_ == OnTaskCompleted.USER_VIEW) {
          webpage_ = ParseResultAsString();
        }
      } catch (IOException e) {
        Log.e("bar", "Error connecting to remote server:\n" + e.getMessage());
        // TODO(PHB): Handle Exception gracefully.
        //return ("Unable to retrieve web page. URL may be invalid.\n" + e.getMessage());
      } finally {
        // Makes sure that the InputStream is closed after the app is
        // finished using it.
        try {
          is_.close();
        } catch (IOException ex) {
          // TODO(PHB): Handle exception.
          Log.e("bar", "Error in closing is_ stream:\n" + ex.getMessage());
        }
      }
      return null;
    }

    protected void onPostExecute(Void result) {
      Log.e("foo", "onPostExecute for: " + task_done_.toString());
      if (is_ == null) {
        Log.e("bar", "Error: is_ stream is null.");
        // TODO(PHB): Handle this error case.
        return;
      }
      if (task_done_ == OnTaskCompleted.PROFILE_PICTURE) {
        JactLoggedInHomeActivity.this.SetProfilePic(picture_);
      } else if (task_done_ == OnTaskCompleted.POINTS_PICTURE) {
        JactLoggedInHomeActivity.this.SetUserPointsPic(picture_);
      } else if (task_done_ == OnTaskCompleted.USER_POINTS) {
        JactLoggedInHomeActivity.this.SetUserPoints(webpage_);
      } else if (task_done_ == OnTaskCompleted.PRIZE_DRAWING) {
        JactLoggedInHomeActivity.this.SetPrizeDrawing(webpage_);
      } else if (task_done_ == OnTaskCompleted.USER_VIEW) {
        JactLoggedInHomeActivity.this.SetUserView(webpage_);
      } else {
        Log.e("bar", "Error: unknown task: " + task_done_.toString());
        // TODO(PHB): Error. Abort gracefully.
      }
    }

    private String ParseResultAsString() {
      try {
        // Convert the InputStream into a string
        BufferedInputStream bis = new BufferedInputStream(is_);
        ByteArrayBuffer baf = new ByteArrayBuffer(50);
        int len;
        byte[] buffer = new byte[4096];
        while (-1 != (len = bis.read(buffer))) {
          baf.append(buffer, 0, len);
        }
        String webPage = new String(baf.toByteArray());
        return webPage;
      } catch (IOException e) {
        Log.e("bar", "Error reading returned webpage:\n" + e.getMessage());
    	  // TODO(PHB): Handle exception
    	  return "";
      }
    }
  } 
*/
  private void SetProfilePic(Bitmap profile_pic_bitmap) {
    Log.e("foo", "SetProfilePic called");
    ImageView image = (ImageView) this.findViewById(R.id.user_profile_pic);
    image.setImageBitmap(profile_pic_bitmap);
  }

  private void SetUserPointsPic(Bitmap userpoints_pic_bitmap) {
    Log.e("foo", "SetUserPointsPic called");
    ImageView image = (ImageView) this.findViewById(R.id.userpoints_pic);
    image.setImageBitmap(userpoints_pic_bitmap);
  }
  
  private void SetUserPoints(String points) {
    Log.e("foo", "SetUserPoints called");
    points = points.replace("[", "");
    points = points.replace("]", "");
    points = points.replaceAll("\n", "");
    try {
      int user_points = Integer.parseInt(points);
      String to_display =
          " User Points:  " +
          NumberFormat.getNumberInstance(Locale.US).format(user_points);
      TextView tv = (TextView) this.findViewById(R.id.current_points);
      tv.setText(to_display);
    } catch (NumberFormatException e) {
      Log.e("bar", "Error: could not parse points into an int: " +
                   points + "\n" + e.getMessage());
      // TODO(PHB): Handle exception.
    }
  }

  private void SetPrizeDrawing(String prize_drawing_info) {
    Log.e("foo", "SetPrizeDrawing called. Response:\n" + prize_drawing_info.toString());
    try {
      JSONArray items = new JSONArray(prize_drawing_info);
      Log.e("foo", "response_json valid, number of items: " + items.length());
      for (int i = 0; i < items.length(); i++) {
        Log.e("foo", "item " + i + ": " + items.getJSONObject(i).toString());
        JSONObject item = items.getJSONObject(i);
        String title = item.getString("node_title");
        String date = item.getString("drawing_date");
        String product_id =
            item.getString("commerce_product_field_data_field_product_product_id");
        //JSONObject nid = item.getJSONObject("nid");
        Log.e("foo", "Title: " + title + "; Date: " + date + "; PID: " + product_id);
      }
    } catch (JSONException e) {
      Log.e("bar", "Failed to parse response");
      // TODO(PHB): Handle exception.
    }
    SetTextView(prize_drawing_info);
  }

  private void SetUserView(String user_view) {
    Log.e("foo", "SetUserView called. Response:\n" + user_view.toString());
    try {
      JSONArray items = new JSONArray(user_view);
      Log.e("foo", "response_json valid, number of items: " + items.length());
      for (int i = 0; i < items.length(); i++) {
        Log.e("foo", "item " + i + ": " + items.getJSONObject(i).toString());
        //JSONObject item = items.getJSONObject(i);
      }
    } catch (JSONException e) {
      Log.e("bar", "Failed to parse response");
      // TODO(PHB): Handle exception.
    }
    SetTextView(user_view);
  }

  protected void SetTextView(String result) {
    TextView tv =
        (TextView) JactLoggedInHomeActivity.this.findViewById(R.id.test_textview);
    tv.setText(result);
    tv.setVisibility(View.VISIBLE);
  }

  public void doFetchPrizeDrawings(View view) {
//PHB      new GetUrlTask(OnTaskCompleted.PRIZE_DRAWING).execute(
//PHB          jact_website_ + "rest/views/jact_prize_drawings.json");
  }

	public void doFetchUserView(View view) {
//PHB      new GetUrlTask(OnTaskCompleted.USER_VIEW).execute(
//PHB          jact_website_ + "rest/views/user_profile_block.json");
  	}

  public void doFetchPremiumProducts(View view) {
    // TODO(PHB): Implement this.
    SetTextView("");
  }

  public void doFetchCollectAndWin(View view) {
    // TODO(PHB): Implement this.
    SetTextView("");
  }

  public void doFetchSupportedGames(View view) {
    // TODO(PHB): Implement this.
    SetTextView("");
  }

  public void doFetchUserGameList(View view) {
    // TODO(PHB): Implement this.
    SetTextView("");
  }

  public void doFetchNodeButtonClick(View view) {
    // TODO(PHB): Implement this, or remove the (invisible) button and
    // (invisible) text.
  }

  @Override
  public void ProcessUrlResponse(String webpage, String cookies) {
    if (webpage == "") {
      // TODO(PHB): Handle failed GET.
    } else if (drawer_click_pos_ == 0) {
      Log.e("PHB ERROR", "Error: Unexpectedly returning from GetTaskUrl.");
    } else if (drawer_click_pos_ == 1) {
      // Start 'ProductsActivity' (for Games).
      Intent products_activity =
          new Intent(JactLoggedInHomeActivity.this, ProductsActivity.class);
      products_activity.putExtra("server_response", webpage);
      startActivity(products_activity);
    } else if (drawer_click_pos_ == 2) {
        // Start 'Activity' (for Rewards).
        Intent products_activity =
            new Intent(JactLoggedInHomeActivity.this, ProductsActivity.class);
        products_activity.putExtra("server_response", webpage);
        startActivity(products_activity);
    } else if (drawer_click_pos_ == 3) {
      // Start 'Activity' (for Community).
      Intent products_activity =
          new Intent(JactLoggedInHomeActivity.this, ProductsActivity.class);
      products_activity.putExtra("server_response", webpage);
      startActivity(products_activity);
    } else {
      Log.e("PHB ERROR", "Unexpected drawer pos: " + drawer_click_pos_);
    }
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies) {
	// TODO(PHB): Implement this.
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status) {
	// TODO(PHB): Implement this.
	Log.e("PHB ERROR", "Status: " + status);
  }
}
