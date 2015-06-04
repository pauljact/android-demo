package com.example.jactfirstdemo;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.support.v4.app.FragmentActivity;
//PHBimport android.support.v4.app.FragmentTransaction;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jactfirstdemo.JactLoggedInHomeActivity;
import com.example.jactfirstdemo.R;

public class JactLoginActivity extends FragmentActivity implements ProcessUrlResponseCallback {
  public static final String USERNAME_SEPERATOR = "PHB_NAME_SEP_PHB";
  private static String login_url_;
  private String username_;
  private String password_;
  private static boolean require_login_;
  private boolean logging_in_;
  private boolean requires_user_input_;
  private JactDialogFragment dialog_;
  private ArrayList<String> usernames_;
  AutoCompleteTextView autocomplete_tv_;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    login_url_ = GetUrlTask.JACT_DOMAIN + "/rest/user/login";
    require_login_ = false;
  }
  
  @Override
  protected void onResume() {
	// Re-enable parent activity before transitioning to the next activity.
	// This ensures e.g. that when user hits 'back' button, the screen
	// is 'active' (not faded) when the user returns.
	//PHB fadeAllViews(num_server_tasks_ != 0);
	super.onResume();
	
	// Check to see if 'logged_off' was passed in; if so, log-off (delete
	// login credentials from SharedPreferences).
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String is_logged_off = getIntent().getStringExtra(getString(R.string.was_logged_off_key));
    if (is_logged_off != null && is_logged_off.equalsIgnoreCase("true")) {
      SharedPreferences.Editor editor = user_info.edit();
      editor.remove(getString(R.string.ui_username));
      editor.remove(getString(R.string.ui_password));
      editor.remove(getString(R.string.ui_user_points));
      editor.remove(getString(R.string.ui_user_icon_url));
      editor.commit();
    }
    
    // Check to see if user is already logged in. If so, go to home screen.
    // Otherwise, go to login screen.
    username_ = user_info.getString(getString(R.string.ui_username), "");
    password_ = user_info.getString(getString(R.string.ui_password), "");
    logging_in_ = false;
    if (require_login_ || username_ == null || username_.isEmpty() || password_ == null || password_.isEmpty()) {
      requires_user_input_ = true;
      // Reset require login.
      require_login_ = false;
      setContentView(R.layout.jact_welcome_screen);
      
  	  // Size Jact Motto so text stretches to fill the Icon and Title.
      // Can't set it here, because views haven't been inflated yet.
      // Instead, assign a listener that will detect when sizes have been
      // set, and reassign widths there.
  	  JactLinearLayout title_ll = (JactLinearLayout) findViewById(R.id.login_icon_and_name);
  	  JactTextView motto_tv = (JactTextView) findViewById(R.id.jact_motto_tv);
  	  motto_tv.SetLinearLayoutToMatch(title_ll);
  	  title_ll.SetTextViewToMatch(motto_tv);
  	  
  	  // Set Autocomplete array to have a list of all previously used user names
  	  GetUsernames();
      autocomplete_tv_ = (AutoCompleteTextView) findViewById(R.id.username_autocomplete_tv);
      ArrayAdapter<String> adapter = 
              new ArrayAdapter<String>(this, R.layout.drop_down_layout, R.id.dropdown_textview, usernames_);
      autocomplete_tv_.setAdapter(adapter);
      // Hack to get drop down to show suggestions even on zero-length query.
      autocomplete_tv_.setOnClickListener(new View.OnClickListener() {
  		@Override
  		public void onClick(View v) {
    	      ShowDropDown();
  		}
  	  });
    } else {
      requires_user_input_ = false;
      setContentView(R.layout.jact_empty_welcome_screen);
      Login(username_, password_);
    }
  }
  
  private void ShowDropDown() {
	if (!usernames_.isEmpty()) {
	  autocomplete_tv_.showDropDown();
	}
  }
  
  private void GetUsernames() {
	SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
	String usernames = user_info.getString(getString(R.string.ui_usernames), "");
    if (usernames != null && !usernames.isEmpty()) {
      usernames_ = new ArrayList<String>(Arrays.asList(usernames.split(JactLoginActivity.USERNAME_SEPERATOR)));
    } else {
      usernames_ = new ArrayList<String>();
    }
  }

  public static void SetRequireLogin(boolean value) {
	require_login_ = value;
  }
  
  private void SetLoggingInState(boolean logging_in) {
    logging_in_ = logging_in;
    // Only time SetLoggingInState is called when requires_user_input_
    // is false is when there was an error logging in using the
    // stored user credentials, e.g. if there is a network issue.
    // In this case, set the login screen, populated with the stored
    // username and password.
    if (!requires_user_input_) {
      requires_user_input_ = true;
      setContentView(R.layout.jact_welcome_screen);
      if (autocomplete_tv_ == null) {
    	autocomplete_tv_ = (AutoCompleteTextView) findViewById(R.id.username_autocomplete_tv);
      }
  	  autocomplete_tv_.setText(username_);
      EditText password_box = (EditText) findViewById(R.id.jact_password);
      password_box.setText(password_);
    }
    Button login_button = (Button) findViewById(R.id.jact_login_button);
    login_button.setEnabled(!logging_in_);
    TextView register_tv = (TextView) findViewById(R.id.jact_register_tv);
    register_tv.setEnabled(!logging_in_);
    TextView forgot_tv = (TextView) findViewById(R.id.forgot_password_textview);
    forgot_tv.setEnabled(!logging_in_);
    if (autocomplete_tv_ == null) {
      autocomplete_tv_ = (AutoCompleteTextView) findViewById(R.id.username_autocomplete_tv);
    }
    autocomplete_tv_.setEnabled(!logging_in_);
    EditText password_box = (EditText) findViewById(R.id.jact_password);
    password_box.setEnabled(!logging_in_);
    ProgressBar spinner = (ProgressBar) findViewById(R.id.login_progress_bar);
    AlphaAnimation alpha;
    if (logging_in_) {
      spinner.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.login_layout);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  private void Login(String username, String password) {
	logging_in_ = true;
    if (requires_user_input_) SetLoggingInState(true);
    GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = login_url_;
	params.connection_type_ = "POST";
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    ArrayList<String> form_info = new ArrayList<String>();
	form_info.add(GetUrlTask.CreateFormInfo("username", username));
	form_info.add(GetUrlTask.CreateFormInfo("password", password));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, form_info);
	task.execute(params);
  }

  public void doForgotPassword (View view) {
	  startActivity(new Intent(this, ForgotPasswordActivity.class));
  }
  
  public void doRegisterButtonClick (View view) {
	  startActivity(new Intent(this, NewUserActivity.class));
  }
  
  public void doDialogOkClick(View view) {
	  // Close Dialog window.
	  dialog_.dismiss();
  }
  
  public void doLoginButtonClick(View view) {
	String username = autocomplete_tv_.getText().toString();
    EditText password = (EditText) findViewById(R.id.jact_password);
    Login(username, password.getText().toString());
  }
  
  public void doLoginScreenClick(View view) {
    SetLoggingInState(false);
  }
  
  private void StoreUserInfo(SharedPreferences.Editor editor, String user_info) {
    try {
      JSONObject response_json = new JSONObject(user_info);
      editor.putString(getString(R.string.was_logged_off_key), "true");
      editor.putBoolean(getString(R.string.ui_is_logged_in), true);
      editor.putString(getString(R.string.ui_session_name), response_json.getString("session_name"));
      editor.putString(getString(R.string.ui_session_id), response_json.getString("sessid"));
      editor.putString(getString(R.string.ui_token), response_json.getString("token"));
      JSONObject user_json = new JSONObject(response_json.getString("user"));
      editor.putString(getString(R.string.ui_user_id), user_json.getString("uid"));
      editor.putString(getString(R.string.ui_user_name), user_json.getString("name"));
      editor.putString(getString(R.string.ui_user_email), user_json.getString("mail"));
      editor.putString(getString(R.string.ui_signature), user_json.getString("signature"));
      // Some profiles don't have a picture (field is set to null).
      if (!user_json.has("picture") || user_json.isNull("picture")) {
    	editor.putString(getString(R.string.ui_user_icon_url), getString(R.string.null_str));
      } else {
        JSONObject picture_info = new JSONObject(user_json.getString("picture"));
        if (picture_info == null || !picture_info.has("url")) {
    	    editor.putString(getString(R.string.ui_user_icon_url), getString(R.string.null_str));
        } else {
          editor.putString(getString(R.string.ui_user_icon_url), picture_info.getString("url"));
        }
      }
    } catch (JSONException e) {
    	Log.e("PHB ERROR", "JactLoginActivity::StoreUserInfo. Unable to parse Login JSON:\n" + user_info);
      // TODO(PHB): Handle exception gracefully.
    }
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    // First make sure 'state' of activity is 'logging in'. Otherwise, user
	// interacted with the app before info from server could be fetched. In this case,
    // do nothing.
    if (!logging_in_) {
      return;
    }
    if (webpage.equals("")) {
      // Handle failed POST
      SetLoggingInState(false);
      Log.e("PHB ERROR", "JactLoginActivity.ProcessUrlResponse: Empty webpage.");
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.VISIBLE);
    } else {
      // Log username and password to Private file (only visible to Jact App).
      // Fetch username/password from the EditText fields, if they were not
      // already retrieved from Preferences file.
      if (username_ == null || username_.isEmpty()) {
    	String username = autocomplete_tv_.getText().toString();
        username_ = username;
      }
      if (password_ == null || password_.isEmpty()) {
        EditText password_from_edit = (EditText) findViewById(R.id.jact_password);
        password_ = password_from_edit.getText().toString().trim();
      }
      SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
      SharedPreferences.Editor editor = user_info.edit();
      editor.putString(getString(R.string.ui_username), username_);
      editor.putString(getString(R.string.ui_password), password_);
      editor.putString(getString(R.string.ui_session_cookies), cookies);
      // Add username_ to the set of all usernames ever used.
      String usernames = user_info.getString(getString(R.string.ui_usernames), "");
      if (usernames == null || usernames.isEmpty()) {
    	usernames = username_;
      } else {
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(usernames.split(USERNAME_SEPERATOR)));
        boolean has_username = false;
        for (String name : names) {
          if (name.equals(username_)) {
        	has_username = true;
        	break;
          }
        }
        if (!has_username) {
          names.add(username_);
        }
        usernames = TextUtils.join(USERNAME_SEPERATOR, names);
      }
      editor.putString(getString(R.string.ui_usernames), usernames);
      // Process response for user info, and store to file.
      StoreUserInfo(editor, webpage);
      editor.commit();
      
      // Go to main logged-in (home) activity.
      Intent logged_in_intent =
        new Intent(JactLoginActivity.this, JactLoggedInHomeActivity.class);
      startActivity(logged_in_intent);
    }
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
    // First make sure 'state' of activity is 'logging in'. Otherwise, user
	// interacted with the app before info from server could be fetched. In this case,
    // do nothing.
    if (!logging_in_) {
      return;
    }
	SetLoggingInState(false);
  }

  @Override
  public void ProcessFailedResponse(GetUrlTask.FetchStatus status, String extra_params) {
    // First make sure 'state' of activity is 'logging in'. Otherwise, user
	// interacted with the app before info from server could be fetched. In this case,
    // do nothing.
    if (!logging_in_) {
      return;
    }
	SetLoggingInState(false);
	Log.e("PHB ERROR", "JactLoginActivity.ProcessFailedResponse. Status: " + status);
    // Handle failed POST
    if (status == GetUrlTask.FetchStatus.ERROR_403 ||
    	status == GetUrlTask.FetchStatus.ERROR_BAD_USERNAME_OR_PASSWORD) {
    	dialog_ = new JactDialogFragment("Wrong Username or Password");
    	dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog_403");
        TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
        tv.setVisibility(View.INVISIBLE);
        tv.setVisibility(View.VISIBLE);
    } else if (status == GetUrlTask.FetchStatus.ERROR_BAD_POST_PARAMS) {
    	dialog_ = new JactDialogFragment("Must Enter Username and Password");
    	dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog_Post_Params");
        TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
        tv.setVisibility(View.INVISIBLE);
        tv.setVisibility(View.VISIBLE);
    } else {
      dialog_ = new JactDialogFragment(
          "Unable to Reach Jact",
          "Check internet connection, and try again.");
      dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog");
      /*FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.add(dialog_, null);
      ft.commitAllowingStateLoss();*/
    }
  }
  
  @Override
  public void IncrementNumRequestsCounter() {
  }

  @Override
  public void DecrementNumRequestsCounter() {
  }

  @Override
  public int GetNumRequestsCounter() {
	return 0;
  }

  @Override
  public void DisplayPopup(String message) {
  }
  
  @Override
  public void DisplayPopup(String title, String message) {
  }
}