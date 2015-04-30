package com.example.jactfirstdemo;

import java.util.ArrayList;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ForgotPasswordActivity extends ActionBarActivity implements ProcessUrlResponseCallback {
  private static final String forgot_password_url_ = "https://m.jact.com:3081/rest/user/request_new_password";
  private JactDialogFragment dialog_;
  private boolean is_password_reset_;
  private static final String FORGOT_PASSWORD_TASK = "forgot_password_task";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
	setContentView(R.layout.forgot_password_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.forgot_password_label);
    setSupportActionBar(toolbar);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	is_password_reset_ = false;
  }

  public void doForgotPasswordClick(View view) {
	EditText username_et = (EditText) findViewById(R.id.forgot_password_et);
    String username = username_et.getText().toString().trim();
    if (username.isEmpty()) {
      EmptyEditTextWarning();
      return;
    }
    SendRequest(username);
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
  	Log.e("PHB TEMP", "ForgotPasswordActivity::SendRequest. post string: " + params.post_string_);
	task.execute(params);
  }
  
  private void EmptyEditTextWarning() {
	dialog_ = new JactDialogFragment("Enter Username or Email Address");
	dialog_.show(getSupportFragmentManager(), "Empty_username");
  }
  
  private void UnrecognizedUsernameWarning() {
	EditText username_et = (EditText) findViewById(R.id.forgot_password_et);
    String username = username_et.getText().toString().trim();
    if (username.isEmpty()) {
      EmptyEditTextWarning();
      return;
    }
	dialog_ = new JactDialogFragment("Jact has no username/email '" + username + "'",
			                         "Re-enter valid username or email, Or sign up as new  user.");
	dialog_.show(getSupportFragmentManager(), "Bad_username");
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
    Log.e("PHB TEMP", "ForgotPassword::onBackPressed");
    JactLoginActivity.SetRequireLogin(true);
    super.onBackPressed();
  }
  
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.forgot_password_progress_bar);
    EditText textbox = (EditText) findViewById(R.id.forgot_password_et);
    AlphaAnimation alpha;
    if (should_fade) {
      spinner.setVisibility(View.VISIBLE);
      textbox.setEnabled(false);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      spinner.setVisibility(View.INVISIBLE);
      textbox.setEnabled(true);
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
      dialog_ = new JactDialogFragment("Further Instructions Have Been Sent to Your Email");
      dialog_.show(getSupportFragmentManager(), "Successful_password_reset");
      is_password_reset_ = true;
    } else {
      Log.e("ForgotPasswordActivity::ProcessUrlResponse", "Unrecognized task: " + extra_params);
    }
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	fadeAllViews(false);
	// TODO(PHB): Implement this for real, once Forgot Password is figured out.
	Log.e("PHB TEMP", "ForgotPassword::ProcessFailedResponse. Status: " + status);
	if (status == FetchStatus.ERROR_UNRECOGNIZED_USERNAME) {
	  UnrecognizedUsernameWarning();
	} else {
      dialog_ = new JactDialogFragment("Unable to Reach Jact",
                                       "Check internet connection, and try again.");
      dialog_.show(getSupportFragmentManager(), "Bad_Login_Dialog");
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
}