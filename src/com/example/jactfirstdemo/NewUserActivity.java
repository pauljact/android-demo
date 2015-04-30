package com.example.jactfirstdemo;

import java.util.ArrayList;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class NewUserActivity extends ActionBarActivity
                             implements OnItemClickListener, ProcessUrlResponseCallback {
  private static final String new_user_url_ = "https://m.jact.com:3081/rest/user/register";
  private JactDialogFragment dialog_;
  private static ArrayList<AvatarItem> avatars_list_;
  private ListView list_;
  private AvatarAdapter adapter_;
  private String avatar_id_;
  private static final String REGISTER_NEW_USER_TASK = "register_new_user_task";
  
  public static class AvatarItem {
    public String id_;
    public String img_url_;
    public Bitmap icon_;
  }
  
  // TODO(PHB): Update this once I can fetch avatar images from phone; and/or
  // when we have a rest view for avatar images.
  private void SetAvatars() {
	avatars_list_ = new ArrayList<AvatarItem>();
	// TODO(PHB): Current populating of avatars_list_ is a hack/temporary. Populate it for real.
	AvatarItem item_one = new AvatarItem();
	item_one.id_ = "222";  // blue
	item_one.img_url_ = "http://m.jact.com:3080/sites/default/files/avatar_selection/avatars-blue.jpg";
	avatars_list_.add(item_one);
	AvatarItem item_two = new AvatarItem();
	item_two.id_ = "223";  // green
	item_two.img_url_ = "http://m.jact.com:3080/sites/default/files/avatar_selection/avatars-green.jpg";
	avatars_list_.add(item_two);
	AvatarItem item_three = new AvatarItem();
	item_three.id_ = "224";  // red
	item_three.img_url_ = "http://m.jact.com:3080/sites/default/files/avatar_selection/avatars-red.jpg";
	avatars_list_.add(item_three);
	
    list_ = (ListView) findViewById(R.id.avatar_list);

    // Getting adapter by passing xml data ArrayList.
    adapter_ = new AvatarAdapter(this, R.layout.avatar_item_layout, avatars_list_);
    list_.setAdapter(adapter_);

    // Click event on Avatar.
    list_.setOnItemClickListener(this);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.new_user_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.new_user_label);
    setSupportActionBar(toolbar);
  }
  
  private void ClearForm() {
	EditText username_et = (EditText) findViewById(R.id.new_user_name_et);
	username_et.setText("");
	EditText email_et = (EditText) findViewById(R.id.new_user_email_et);
    email_et.setText("");
	EditText password_et = (EditText) findViewById(R.id.new_user_password_et);
    password_et.setText("");
	EditText pass2_et = (EditText) findViewById(R.id.new_user_password_confirm_et);
    pass2_et.setText("");
    // TODO(PHB): Unselect avatar too, once Selection of Avatars has been implemented
  }
    
  @Override
  protected void onResume() {
	super.onResume();
	SetAvatars();
	ClearForm();
	fadeAllViews(false);
	avatar_id_ = "";
  }

  @Override
  public void onBackPressed() {
    Log.e("PHB TEMP", "ForgotPassword::onBackPressed");
    JactLoginActivity.SetRequireLogin(true);
    super.onBackPressed();
  }
  
  public void doRegisterClick(View view) {
	EditText username_et = (EditText) findViewById(R.id.new_user_name_et);
    String username = username_et.getText().toString().trim();
    if (username.isEmpty()) {
      EmptyEditTextWarning("Must Enter a Username");
      return;
    }
	EditText email_et = (EditText) findViewById(R.id.new_user_email_et);
    String email = email_et.getText().toString().trim();
    if (email.isEmpty()) {
      EmptyEditTextWarning("Must Enter an Email Address");
      return;
    }
	EditText password_et = (EditText) findViewById(R.id.new_user_password_et);
    String password = password_et.getText().toString().trim();
    if (password.isEmpty()) {
      EmptyEditTextWarning("Must Enter a Password");
      return;
    }
	EditText pass2_et = (EditText) findViewById(R.id.new_user_password_confirm_et);
    String pass2 = pass2_et.getText().toString().trim();
    if (!pass2.equals(password)) {
      EmptyEditTextWarning("Passwords Do Not Match. Re-enter Password(s)");
      return;
    }
    // TODO(PHB): Update this when we handle avatars for real.
    if (avatar_id_.isEmpty()) {
      EmptyEditTextWarning("You Must Select an Avatar");
      return;
    }
    
    // TODO(PHB): Add avatar selection.
    SendRequest(username, email, password, pass2, avatar_id_);
  }
  
  private void SendRequest(String username, String email, String password, String pass2, String avatar_id) {
	GetUrlTask task = new GetUrlTask(this, GetUrlTask.TargetType.JSON);
	GetUrlTask.UrlParams params = new GetUrlTask.UrlParams();
	params.url_ = new_user_url_;
	params.connection_type_ = "POST";
	params.extra_info_ = REGISTER_NEW_USER_TASK;
  	ArrayList<String> header_info = new ArrayList<String>();
    header_info.add(GetUrlTask.CreateHeaderInfo("Content-Type", "application/json"));
    ArrayList<String> form_info = new ArrayList<String>();
    form_info.add(GetUrlTask.CreateFormInfo("name", username));
    form_info.add(GetUrlTask.CreateFormInfo("mail", email));
    form_info.add(GetUrlTask.CreateFormInfo("pass", password));
    //PHBform_info.add(GetUrlTask.CreateFormInfo("pass%5Bpass1%5D", password));
    //PHBform_info.add(GetUrlTask.CreateFormInfo("pass%5Bpass2%5D", pass2));
    // TODO(PHB): Implement avatar selection, and possibly timezone (key="timezone", value=e.g."America/New_York")
    form_info.add(GetUrlTask.CreateFormInfo("select_avatar", avatar_id));
  	params.post_string_ = GetUrlTask.CreatePostString(header_info, form_info);
  	fadeAllViews(true);
  	Log.e("PHB TEMP", "NewUserActivity::SendRequest. post string: " + params.post_string_);
	task.execute(params);
  }
  
  private void EmptyEditTextWarning(String warning) {
	dialog_ = new JactDialogFragment(warning);
	dialog_.show(getSupportFragmentManager(), warning);
  }
  
  public void doDialogOkClick(View view) {
    // Close Dialog window.
    dialog_.dismiss();
  }
  
  private void Login() {
	fadeAllViews(false);
	EditText username_et = (EditText) findViewById(R.id.new_user_name_et);
    String username = username_et.getText().toString().trim();
    if (username.isEmpty()) {
      Log.e("NewUserActivity::Login", "Username field empty; unable to login.");
      return;
    }
	EditText password_et = (EditText) findViewById(R.id.new_user_password_et);
    String password = password_et.getText().toString().trim();
    if (password.isEmpty()) {
      Log.e("NewUserActivity::Login", "Password field empty; unable to login.");
      return;
    }
    SharedPreferences user_info = getSharedPreferences(getString(R.string.ui_master_file), MODE_PRIVATE);
    SharedPreferences.Editor editor = user_info.edit();
    editor.putString(getString(R.string.ui_username), username);
    editor.putString(getString(R.string.ui_password), password);
    editor.commit();
	startActivity(new Intent(this, JactLoginActivity.class));
  }
  
  public void fadeAllViews(boolean should_fade) {
    ProgressBar spinner = (ProgressBar) findViewById(R.id.new_user_progress_bar);
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.new_user_content_frame);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	AvatarAdapter.AvatarViewHolder holder = (AvatarAdapter.AvatarViewHolder) view.getTag();
	avatar_id_ = holder.id_.getText().toString().trim();
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	if (extra_params.indexOf(REGISTER_NEW_USER_TASK) == 0) {
	  Login();
	} else {
	  Log.e("NewUserActivity::ProcessUrlResponse", "Unexpected task: " + extra_params);
	}
  }

  @Override
  public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	// TODO Auto-generated method stub
  }

  @Override
  public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	fadeAllViews(false);
	if (extra_params.indexOf(REGISTER_NEW_USER_TASK) == 0) {
	  if (status == FetchStatus.ERROR_NEW_USER_BAD_AVATAR) {
		EmptyEditTextWarning("Unrecognized Avatar. Please Re-Select Avatar.");
	  } else if (status == FetchStatus.ERROR_NEW_USER_BAD_EMAIL) {
		EditText email_et = (EditText) findViewById(R.id.new_user_email_et);
	    String email = email_et.getText().toString().trim();
		EmptyEditTextWarning("The Email Address '" + email + "' is not Valid");
	  } else if (status == FetchStatus.ERROR_NEW_USER_DUPLICATE_EMAIL) {
		EditText email_et = (EditText) findViewById(R.id.new_user_email_et);
		String email = email_et.getText().toString().trim();
		EmptyEditTextWarning("The Email Address '" + email + "' is Already Registered");
	  } else if (status == FetchStatus.ERROR_NEW_USER_BAD_NAME) {
		EditText username_et = (EditText) findViewById(R.id.new_user_name_et);
	    String username = username_et.getText().toString().trim();
		EmptyEditTextWarning("The Username '" + username + "' is Already Taken");
	  } else if (status == FetchStatus.ERROR_NEW_USER_MISMATCHING_PASSWORDS) {
		EmptyEditTextWarning("Passwords Do Not Match. Re-enter Password(s)");
	  } else if (status == FetchStatus.ERROR_UNKNOWN_406) {
		Log.e("NewUserActivity::ProcessFailedResponse", "Unknown 406 error");
	  } else {
		EmptyEditTextWarning("Unknown Error");
		Log.e("NewUserActivity::ProcessFailedResponse", "Unknown status: " + status);
	  }
	} else {
	  Log.e("NewUserActivity::ProcessUrlResponse", "Unexpected task: " + extra_params);
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