package com.jact.jactapp;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.support.v4.app.FragmentActivity;
//PHBimport android.support.v4.app.FragmentTransaction;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

public class JactLoginActivity extends FragmentActivity implements
        ProcessUrlResponseCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
  public static final String USERNAME_SEPERATOR = "PHB_NAME_SEP_PHB";
  private static String login_url_;
  private static String debug_login_url_;
  private String username_;
  private String password_;
  private static boolean require_login_;
  private boolean logging_in_;
  private boolean requires_user_input_;
  private ArrayList<String> usernames_;
  AutoCompleteTextView autocomplete_tv_;
  private boolean can_show_dialog_;
  private JactDialogFragment dialog_;

  /* Request code used to invoke sign in user interactions. */
  private static final int RC_SIGN_IN = 0;

  /* Client used to interact with Google APIs. */
  private GoogleApiClient mGoogleApiClient;

  // Checkbox used for debugging purposed (toggles between UAT and jact.com)
  private CheckBox toggle_jact_url_checkbox_;
  private static boolean use_uat_server_;

  /**
   * True if the sign-in button was clicked.  When true, we know to resolve all
   * issues preventing sign-in without waiting.
   */
  private boolean mSignInClicked;

  /**
   * True if we are in the process of resolving a ConnectionResult
   */
  private boolean mIntentInProgress;

  private CallbackManager callbackManager_;

  private boolean is_google_signed_in_;
  private boolean is_facebook_signed_in_;
  private boolean is_facebook_signing_in_ = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    is_facebook_signing_in_ = false;
    can_show_dialog_ = false;
    use_uat_server_ = false;
    login_url_ = GetUrlTask.GetJactDomain() + "/rest/user/login";
    require_login_ = false;

    // For Google Sign-in.
    mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(Plus.API)
            .addScope(new Scope("profile"))
            .build();

    // For Facebook Sign-in.
    FacebookSdk.sdkInitialize(getApplicationContext());

    // Determine if User is currently logged-in via Google or Facebook.
    SharedPreferences user_info =
        getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    is_google_signed_in_ = user_info.getBoolean(getString(R.string.ui_is_google_logged_in), false);
    is_facebook_signed_in_ =
        user_info.getBoolean(getString(R.string.ui_is_facebook_logged_in), false);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (is_google_signed_in_) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onStart. is_google_signed_in");
      }
      mGoogleApiClient.connect();
    } else if (is_facebook_signed_in_) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onStart. is_facebook_signed_in");
      }
      //TODO(PHB): Implement this.
    }
  }

  @Override
  protected void onResume() {
	// Re-enable parent activity before transitioning to the next activity.
	// This ensures e.g. that when user hits 'back' button, the screen
	// is 'active' (not faded) when the user returns.
	//PHB fadeAllViews(num_server_tasks_ != 0);
	super.onResume();
    login_url_ = GetUrlTask.GetJactDomain() + "/rest/user/login";
    can_show_dialog_ = true;

	// Check to see if 'logged_off' was passed in; if so, log-off (delete
	// login credentials from SharedPreferences, and log-off Google/Facebook if necessary).
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    String is_logged_off = getIntent().getStringExtra(getString(R.string.was_logged_off_key));
    if (is_logged_off != null && is_logged_off.equalsIgnoreCase("true")) {
      SharedPreferences.Editor editor = user_info.edit();
      editor.remove(getString(R.string.ui_username));
      editor.remove(getString(R.string.ui_password));
      editor.remove(getString(R.string.ui_user_points));
      editor.remove(getString(R.string.ui_user_icon_url));

      // Log off of Google.
      editor.putBoolean(getString(R.string.ui_is_google_logged_in), false);
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onResume. Logging out of google and facebook");
      }
      is_google_signed_in_ = false;
      if (mGoogleApiClient.isConnected()) {
        mGoogleApiClient.clearDefaultAccountAndReconnect();
      }

      // Log off of Facebook.
      editor.putBoolean(getString(R.string.ui_is_facebook_logged_in), false);
      is_facebook_signed_in_ = false;

      editor.commit();
    }
    
    // Check to see if user is already logged in. If so, go to home screen.
    // Otherwise, go to login screen.
    username_ = user_info.getString(getString(R.string.ui_username), "");
    password_ = user_info.getString(getString(R.string.ui_password), "");
    logging_in_ = false;
    if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB TEMP", "JactLoginActivity::onResume. is_google_signed_in_: " +
            is_google_signed_in_ + ", is_facebook_signed_in_: " +
            is_facebook_signed_in_);
    }
    if (require_login_ ||
        is_google_signed_in_ || // TODO(PHB): Remove this line, once google_sign_in works!
        (!is_google_signed_in_ && !is_facebook_signed_in_ &&
         (username_ == null || username_.isEmpty() || password_ == null || password_.isEmpty()))) {
      SetupWelcomeLayout();
    } else if (is_google_signed_in_) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onResume. is_google_signed_in_ = true");
      }
      requires_user_input_ = false;
      setContentView(R.layout.jact_empty_welcome_screen);
    } else if (is_facebook_signed_in_) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onResume. is_facebook_signed_in_ = true");
      }
      AppEventsLogger.activateApp(this);
      requires_user_input_ = false;
      setContentView(R.layout.jact_empty_welcome_screen);
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP",
              "JactLoginActivity::onResume. is_google_signed_in_, is_facebook_signed_in_ = false");
      }
      requires_user_input_ = false;
      setContentView(R.layout.jact_empty_welcome_screen);
      Login(username_, password_);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
    if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB TEMP", "JactLoginActivity::onActivityResult. requestCode: " + requestCode +
            ", responseCode: " + responseCode + ", + intent: " + intent.toString());
    }
    // TODO(PHB): Decide if I should do both Google and Facebook clauses below for
    // when I'm in the case that both are true (i.e. how to decide which is the proper one to do?
    // Use requestCode?
    if (mSignInClicked || mIntentInProgress || is_google_signed_in_) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onActivityResult. Inside is_google_signed_in.");
      }
      if (requestCode == RC_SIGN_IN) {
        if (responseCode != RESULT_OK) {
          mSignInClicked = false;
        }

        mIntentInProgress = false;

        if (!mGoogleApiClient.isConnected()) {
          mGoogleApiClient.reconnect();
        }
      }
    }

    if (is_facebook_signed_in_ || is_facebook_signing_in_) {
      is_facebook_signing_in_ = false;
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::onActivityResult. Inside is_facebook_signed_in.");
      }
      callbackManager_.onActivityResult(requestCode, responseCode, intent);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    can_show_dialog_ = false;

    if(is_facebook_signed_in_) {
      AppEventsLogger.deactivateApp(this);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    mGoogleApiClient.disconnect();
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

  private void SetupWelcomeLayout() {requires_user_input_ = true;
    // Reset require login.
    require_login_ = false;
    setContentView(R.layout.jact_welcome_screen);

    toggle_jact_url_checkbox_ = (CheckBox) findViewById(R.id.jact_web_domain_url_checkbox);
    if (JactActionBarActivity.IS_PRODUCTION) {
      toggle_jact_url_checkbox_.setVisibility(View.GONE);
    }

    // Listener for Google Sign-In button click.
    //BHPfindViewById(R.id.jact_google_signin_button).setOnClickListener(this);

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
  }

  private void SetLoggingInState(boolean logging_in) {
    logging_in_ = logging_in;
    // Only time SetLoggingInState is called when requires_user_input_
    // is false is when there was an error logging in using the
    // stored user credentials, e.g. if there is a network issue.
    // In this case, set the login screen, populated with the stored
    // username and password.
    if (!requires_user_input_) {
      SetupWelcomeLayout();
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
    params.url_ = GetUrlTask.GetJactDomain() + "/rest/user/login";
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
  
  public void doLoginButtonClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
	String username = autocomplete_tv_.getText().toString();
    EditText password = (EditText) findViewById(R.id.jact_password);
    Login(username, password.getText().toString());
  }

  public void doGoogleButtonClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB TEMP", "JactLoginActivity::doGoogleButtonClick. is_google_signed_in_ : " +
            is_google_signed_in_ + ", mGoogleApiClient.isConnecting(): " +
            mGoogleApiClient.isConnecting() + ", mSignInClicked: " +
            mSignInClicked + ", mIntentInProgress: " + mIntentInProgress);
    }
    if (!is_google_signed_in_ && !mGoogleApiClient.isConnecting()) {
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB TEMP", "JactLoginActivity::doGoogleButtonClick. Attempting to connect.");
      }
      mSignInClicked = true;
      mGoogleApiClient.connect();
    }
    //String username = autocomplete_tv_.getText().toString();
    //EditText password = (EditText) findViewById(R.id.jact_password);
    //Login(username, password.getText().toString());
  }
  /*BHP Start
  public void doFacebookButtonClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB TEMP", "JactLoginActivity::doFacebookButtonClick.");
    }
    if (is_facebook_signed_in_) {
      // Already signed in, so they must have clicked Sign Out
      is_facebook_signed_in_ = false;
      SharedPreferences user_info =
              getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
      SharedPreferences.Editor editor = user_info.edit();
      editor.putBoolean(getString(R.string.ui_is_facebook_logged_in), false);
      editor.commit();
    } else {
      is_facebook_signing_in_ = true;
      callbackManager_ = CallbackManager.Factory.create();
      LoginButton loginButton = (LoginButton) view.findViewById(R.id.jact_facebook_signin_button);
      loginButton.registerCallback(callbackManager_, new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
          if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", "JactLoginActivity::FacebookCallback::onSuccess. loginResult:" +
                  loginResult.toString());
          }
          is_facebook_signed_in_ = true;
          SharedPreferences user_info =
                  getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
          SharedPreferences.Editor editor = user_info.edit();
          editor.putBoolean(getString(R.string.ui_is_facebook_logged_in), true);
          editor.commit();
        }

        @Override
        public void onCancel() {
          if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", "JactLoginActivity::FacebookCallback::onCancel.");
          }
        }

        @Override
        public void onError(FacebookException exception) {
          if (!JactActionBarActivity.IS_PRODUCTION) {
            Log.e("PHB TEMP", "JactLoginActivity::FacebookCallback::onError. Exception: " +
                  exception.toString());
          }
        }
      });
    }
    //String username = autocomplete_tv_.getText().toString();
    //EditText password = (EditText) findViewById(R.id.jact_password);
    //Login(username, password.getText().toString());
  }BHP END*/
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
    	if (!JactActionBarActivity.IS_PRODUCTION) {
          Log.e("PHB ERROR", "JactLoginActivity::StoreUserInfo. Unable to parse Login JSON:\n" + user_info);
        }
      // TODO(PHB): Handle exception gracefully.
    }
  }
  
  public void doDialogOkClick(View view) {
	  // Close Dialog window.
	  dialog_.dismiss();
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
      if (!JactActionBarActivity.IS_PRODUCTION) {
        Log.e("PHB ERROR", "JactLoginActivity.ProcessUrlResponse: Empty webpage.");
      }
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
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB ERROR", "JactLoginActivity.ProcessFailedResponse. Status: " + status);
    }
    // Handle failed POST
    if (status == GetUrlTask.FetchStatus.ERROR_403 ||
    	status == GetUrlTask.FetchStatus.ERROR_BAD_USERNAME_OR_PASSWORD) {
      if (can_show_dialog_) {
    	dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Wrong Username or Password");
    	dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog_403");
      }
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.INVISIBLE);
      tv.setVisibility(View.VISIBLE);
    } else if (status == GetUrlTask.FetchStatus.ERROR_BAD_POST_PARAMS) {
      if (can_show_dialog_) {
    	dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Must Enter Username and Password");
    	dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog_Post_Params");
      }
      TextView tv = (TextView) JactLoginActivity.this.findViewById(R.id.login_error_textview);
      tv.setVisibility(View.INVISIBLE);
      tv.setVisibility(View.VISIBLE);
    } else {
      if (can_show_dialog_) {
        dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Unable to Reach Jact");
        dialog_.SetMessage("Check internet connection, and try again.");
        dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog");
      }
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

  @Override
  public void onConnected(Bundle bundle) {
    // We've resolved any connection errors.  mGoogleApiClient can be used to
    // access Google APIs on behalf of the user.

    Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
    is_google_signed_in_ = true;
    SharedPreferences user_info =
        getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putBoolean(getString(R.string.ui_is_google_logged_in), true);
    editor.commit();

    // TODO(PHB): Add call to Jact Server here, to authenticate Google sign in, and get cookies/
    // credentials for this user. Then proceed to JLHA on successful response from Jact server.
  }

  @Override
  public void onConnectionSuspended(int i) {
    if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("PHB TEMP", "JactLoginActivity::onConnectionSuspended. Reconnecting to google.");
    }
    mGoogleApiClient.connect();
  }

  @Override
  public void onConnectionFailed(ConnectionResult result) {
    if (!mIntentInProgress && result.hasResolution()) {
      try {
        mIntentInProgress = true;
        result.startResolutionForResult(this, RC_SIGN_IN);
      } catch (IntentSender.SendIntentException e) {
        // The intent was canceled before it was sent.  Return to the default
        // state and attempt to connect to get an updated ConnectionResult.
        mIntentInProgress = false;
        if (!JactActionBarActivity.IS_PRODUCTION) {
          Log.e("PHB TEMP", "JactLoginActivity::onConnectionFailed. Reconnecting to google.");
        }
        mGoogleApiClient.connect();
      }
    }
  }

  @Override
  public void onClick(View view) {
    doGoogleButtonClick(view);
  }

  public void onUseUatCheckboxClicked(View view) {
    use_uat_server_ = ((CheckBox) view).isChecked();
    GetUrlTask.SetServer(use_uat_server_);
  }
}