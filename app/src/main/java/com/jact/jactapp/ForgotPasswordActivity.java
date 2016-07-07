package com.jact.jactapp;

import java.util.ArrayList;
import java.util.Arrays;

import com.jact.jactapp.GetUrlTask.FetchStatus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ForgotPasswordActivity extends ActionBarActivity 
                                    implements OnItemSelectedListener, ProcessUrlResponseCallback {
  private static String forgot_password_url_;
  private JactDialogFragment dialog_;
  private boolean can_show_dialog_;
  private ArrayList<String> usernames_;
  AutoCompleteTextView autocomplete_tv_;
  private boolean is_password_reset_;
  private static final String FORGOT_PASSWORD_TASK = "forgot_password_task";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    can_show_dialog_ = false;
    forgot_password_url_ = GetUrlTask.GetJactDomain() + "/rest/user/request_new_password";
	setContentView(R.layout.forgot_password_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.forgot_password_label);
    TextView points = (TextView) findViewById(R.id.toolbar_title_points);
    points.setVisibility(View.GONE);
    ImageView points_icon = (ImageView) findViewById(R.id.toolbar_userpoints_pic);
    points_icon.setVisibility(View.GONE);
    setSupportActionBar(toolbar);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    forgot_password_url_ = GetUrlTask.GetJactDomain() + "/rest/user/request_new_password";
	can_show_dialog_ = true;
	is_password_reset_ = false;
	GetUsernames();
    
    // Get a reference to the AutoCompleteTextView in the layout.
    autocomplete_tv_ = (AutoCompleteTextView) findViewById(R.id.username_autocomplete_tv);
    ArrayAdapter<String> adapter = 
            new ArrayAdapter<String>(this, R.layout.drop_down_layout, R.id.dropdown_textview, usernames_);
    autocomplete_tv_.setAdapter(adapter);
    autocomplete_tv_.setHint("Start typing to see old usernames");
    // Hack to get drop down to show suggestions even on zero-length query.
    autocomplete_tv_.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
  	      ShowDropDown();
		}
	});
    autocomplete_tv_.setText("");
	fadeAllViews(false);
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    can_show_dialog_ = true;
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    can_show_dialog_ = false;
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

  public void doForgotPasswordClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
	String username = autocomplete_tv_.getText().toString();
    if (username.isEmpty() || username.trim().length() == 0) {
      EmptyEditTextWarning();
      return;
    }
    SendRequest(username);
  }
  
  public void doGoToJactClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
    Uri uri_url = Uri.parse(GetUrlTask.GetJactDomain() + "/user/password");
    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
    startActivity(launchBrowser);
  }
  
  private void SendRequest(String username) {
	GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = forgot_password_url_;
	params.connection_type_ = "POST";
	params.extra_info_ = FORGOT_PASSWORD_TASK;
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    ArrayList<String> form_info = new ArrayList<String>();
    form_info.add(GetUrlTask.CreateFormInfo("name", username));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, form_info);
  	fadeAllViews(true);
	task.execute(params);
  }
  
  private void EmptyEditTextWarning() {
	if (can_show_dialog_) {
	  dialog_ = new JactDialogFragment();
      dialog_.SetTitle("Enter Username or Email Address");
	  dialog_.show(getSupportFragmentManager(), "Empty_username");
	}
  }
  
  private void UnrecognizedUsernameWarning() {
    String username = autocomplete_tv_.getText().toString();
    if (username.isEmpty() || username.trim().length() == 0) {
      EmptyEditTextWarning();
      return;
    }
    if (can_show_dialog_) {
	  dialog_ = new JactDialogFragment();
      dialog_.SetTitle("Username '" + username + "' Does Not Exist");
      dialog_.SetMessage("Enter valid username, or sign up as a new user");
	  dialog_.show(getSupportFragmentManager(), "Bad_username");
    }
  }
  
  public void doDialogOkClick(View view) {
    // Close Dialog window.
    dialog_.dismiss();
	if (is_password_reset_) {
	  is_password_reset_ = false;
	  startActivity(new Intent(this, JactLoginActivity.class));
	}
  }

  @Override
  public void onBackPressed() {
    JactLoginActivity.SetRequireLogin(true);
    super.onBackPressed();
  }
  
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.forgot_password_progress_bar);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      autocomplete_tv_.setEnabled(false);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      autocomplete_tv_.setEnabled(true);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    // The AlphaAnimation will make the whole content frame transparent
    // (so that none of the views show).
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.forgot_password_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
    if (extra_params.indexOf(FORGOT_PASSWORD_TASK) == 0) {
      if (can_show_dialog_) {
        dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Further Instructions Have Been Sent to Your Email");
        dialog_.show(getSupportFragmentManager(), "Successful_password_reset");
      }
      is_password_reset_ = true;
    } else {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("ForgotPasswordActivity::ProcessUrlResponse", "Unrecognized task: " + extra_params);
    }
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	fadeAllViews(false);
	// TODO(PHB): Implement this for real, once Forgot Password is figured out.
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("ForgotPassword::ProcessFailedResponse", "Status: " + status);
    }
	if (status == FetchStatus.ERROR_UNRECOGNIZED_USERNAME) {
	  UnrecognizedUsernameWarning();
	} else {
	  if (can_show_dialog_) {
        dialog_ = new JactDialogFragment();
        dialog_.SetTitle("Unable to Reach Jact");
        dialog_.SetMessage("Check internet connection, and try again.");
        dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog");
	  }
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
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }
}