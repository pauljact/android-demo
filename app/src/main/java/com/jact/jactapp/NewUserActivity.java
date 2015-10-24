package com.jact.jactapp;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import com.jact.jactapp.GetUrlTask.FetchStatus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class NewUserActivity extends ActionBarActivity
                             implements OnItemSelectedListener, ProcessUrlResponseCallback {
  private static String new_user_url_;
  private JactDialogFragment dialog_;
  private boolean can_show_dialog_;
  private static ArrayList<AvatarItem> avatars_list_;
  private AvatarAdapter adapter_;
  private String avatar_id_;
  private static final String REGISTER_NEW_USER_TASK = "register_new_user_task";
  private String image_uri_;
  private boolean should_clear_form_on_resume_;
  
  ImageView user_img_;
  Spinner jact_avatar_spinner_;
  
  public static class AvatarItem {
    public String id_;
    public String img_url_;
    public Bitmap icon_;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    can_show_dialog_ = false;
    new_user_url_ = GetUrlTask.GetJactDomain() + "/rest/user/register";
    setContentView(R.layout.new_user_layout);
    Toolbar toolbar = (Toolbar) findViewById(R.id.jact_toolbar);
    TextView ab_title = (TextView) findViewById(R.id.toolbar_title_tv);
    ab_title.setText(R.string.new_user_label);
    TextView points = (TextView) findViewById(R.id.toolbar_title_points);
    points.setVisibility(View.GONE);
    ImageView points_icon = (ImageView) findViewById(R.id.toolbar_userpoints_pic);
    points_icon.setVisibility(View.GONE);
    setSupportActionBar(toolbar);

    should_clear_form_on_resume_ = true;
    image_uri_ = "";
    user_img_ = (ImageView) findViewById(R.id.new_user_temp_img);
    jact_avatar_spinner_ = (Spinner) findViewById(R.id.new_user_default_icon_spinner);
  }
    
  @Override
  protected void onResume() {
	super.onResume();
    new_user_url_ = GetUrlTask.GetJactDomain() + "/rest/user/register";
	can_show_dialog_ = true;
	SetAvatars();
	if (should_clear_form_on_resume_) {
	  ClearForm();
	}
	should_clear_form_on_resume_ = true;
	fadeAllViews(false);
	avatar_id_ = "";
  }

  @Override
  public void onBackPressed() {
    JactLoginActivity.SetRequireLogin(true);
    super.onBackPressed();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    can_show_dialog_ = true;

    if (resultCode == RESULT_OK) {
      Uri img_uri = data.getData();
      Bitmap bitmap;
      try {
        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(img_uri));
        if (bitmap == null) {
          if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::onActivityResult", "Null bitmap. Aborting.");
          return;
        }
        if (bitmap.getHeight() < 2000 && bitmap.getWidth() < 2000) {
          user_img_.setImageBitmap(bitmap);
        } else {
          user_img_.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 120, 120, false));        	
        }
        ToggleImageAndSelector(true);
        image_uri_ = img_uri.toString();
        /* TODO(PHB): Can uncomment below to convert bitmap to a jpeg/output stream that
         * can be sent to jact server.
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int quality = 50; // quality is an int from 0 (compress for small size) to 100 (max quality)
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
		byte[] array = os.toByteArray();
         */
      } catch (FileNotFoundException e) {
    	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::onActivityResult", "FNF Bitmap exception: " + e.getMessage());
      }
    }
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    can_show_dialog_ = false;
  }
  
  private void ToggleImageAndSelector(boolean focus_image) {
    AlphaAnimation alpha;
    if (focus_image) {
      avatar_id_ = "";
      user_img_.setVisibility(View.VISIBLE);
      alpha = new AlphaAnimation(0.5F, 0.5F);
    } else {
      image_uri_ = "";
      user_img_.setVisibility(View.INVISIBLE);
      alpha = new AlphaAnimation(1.0F, 1.0F);
    }
    // The AlphaAnimation will make the whole content frame transparent
    // (so that none of the views show).
    alpha.setDuration(0); // Make animation instant
    alpha.setFillAfter(true); // Tell it to persist after the animation ends
    jact_avatar_spinner_.startAnimation(alpha); // Add animation to the layout.
  }
  
  // TODO(PHB): Update this once I can fetch avatar images from phone; and/or
  // when we have a rest view for avatar images.
  private void SetAvatars() {
	avatars_list_ = new ArrayList<AvatarItem>();
	// TODO(PHB): Current populating of avatars_list_ is a hack/temporary. Populate it for real.
	AvatarItem item_zero = new AvatarItem();
	item_zero.id_ = "foo";
	avatars_list_.add(item_zero);
	AvatarItem item_one = new AvatarItem();
	item_one.id_ = "390";  // blue
	item_one.img_url_ = GetUrlTask.GetJactDomain() + "/sites/default/files/avatar_selection/coin_avatar.jpg";
	avatars_list_.add(item_one);
	AvatarItem item_two = new AvatarItem();
	item_two.id_ = "363";  // green
	item_two.img_url_ = GetUrlTask.GetJactDomain() + "/sites/default/files/avatar_selection/community_mgr.png";
	avatars_list_.add(item_two);
	AvatarItem item_three = new AvatarItem();
	item_three.id_ = "362";  // red
	item_three.img_url_ = GetUrlTask.GetJactDomain() + "/sites/default/files/avatar_selection/professor.png";
	avatars_list_.add(item_three);
	
    //PHB_OLDlist_ = (ListView) findViewById(R.id.avatar_list);

    // Getting adapter by passing xml data ArrayList.
	adapter_ = new AvatarAdapter(this, R.layout.avatar_item_layout, avatars_list_);
    jact_avatar_spinner_.setAdapter(adapter_);

    // Click event on Avatar.
    jact_avatar_spinner_.setOnItemSelectedListener(this);
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
  
  public void doGetPhonePhotoClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
	// After coming back from selecting a photo, onResume() is called. We don't want to clear
	// the form though.
	should_clear_form_on_resume_ = false;
    Intent intent = new Intent(Intent.ACTION_PICK,
    		                   android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(intent, 0);
  }

  public void doRegisterClick(View view) {
    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibe.vibrate(JactConstants.VIBRATION_DURATION);
	EditText username_et = (EditText) findViewById(R.id.new_user_name_et);
    String username = username_et.getText().toString().trim();
    if (username.isEmpty()) {
      EmptyEditTextWarning("Must Enter a Username");
      return;
    } else if (username.contains(" ")) {
      EmptyEditTextWarning("Invalid Username: Spaces not allowed");
      return;
    }
	EditText email_et = (EditText) findViewById(R.id.new_user_email_et);
    String email = email_et.getText().toString().trim();
    if (email.isEmpty()) {
      EmptyEditTextWarning("Must Enter an Email Address");
      return;
    } else if (email.contains(" ")) {
      EmptyEditTextWarning("Invalid Email: Spaces not allowed");
      return;
    }
	EditText password_et = (EditText) findViewById(R.id.new_user_password_et);
    String password = password_et.getText().toString().trim();
    if (password.isEmpty()) {
      EmptyEditTextWarning("Must Enter a Password");
      return;
    } else if (password.contains(" ")) {
      EmptyEditTextWarning("Invalid Password: Spaces not allowed");
      return;
    }
	EditText pass2_et = (EditText) findViewById(R.id.new_user_password_confirm_et);
    String pass2 = pass2_et.getText().toString().trim();
    if (!pass2.equals(password)) {
      EmptyEditTextWarning("Passwords Do Not Match. Re-enter Password(s)");
      return;
    }
    // TODO(PHB): Update this when we handle avatars for real.
    String avatar_id = avatar_id_.isEmpty() ? image_uri_ : avatar_id_;
    if (avatar_id.isEmpty()) {
      EmptyEditTextWarning("You Must Select an Avatar or a Photo");
      return;
    }
    
    // TODO(PHB): Add avatar selection.   
    SendRequest(username, email, password, pass2, avatar_id);
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
  	if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "NewUserActivity::SendRequest. post string: " + params.post_string_);
	task.execute(params);
  }
  
  private void EmptyEditTextWarning(String warning) {
	if (can_show_dialog_) {
	  dialog_ = new JactDialogFragment(warning);
	  dialog_.show(getSupportFragmentManager(), warning);
	}
  }
  
  private void EmptyEditTextWarning(String title, String warning) {
	if (can_show_dialog_) {
	  dialog_ = new JactDialogFragment(title, warning);
	  dialog_.show(getSupportFragmentManager(), title);
	}
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
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::Login", "Username field empty; unable to login.");
      return;
    }
	EditText password_et = (EditText) findViewById(R.id.new_user_password_et);
    String password = password_et.getText().toString().trim();
    if (password.isEmpty()) {
      if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::Login", "Password field empty; unable to login.");
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
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.new_user_main_rl);
    layout.startAnimation(alpha); // Add animation to the layout.
  }
  
  @Override
  public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	if (extra_params.indexOf(REGISTER_NEW_USER_TASK) == 0) {
	  Login();
	} else {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::ProcessUrlResponse", "Unexpected task: " + extra_params);
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
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::ProcessFailedResponse", "Unknown 406 error");
	  } else {
		EmptyEditTextWarning("Unknown Error", "Please Try Again");
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::ProcessFailedResponse", "Unknown status: " + status);
	  }
	} else {
	  if (!JactActionBarActivity.IS_PRODUCTION) Log.e("NewUserActivity::ProcessUrlResponse", "Unexpected task: " + extra_params);
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
	if (position != 0) {
	  ToggleImageAndSelector(false);
	  AvatarAdapter.AvatarViewHolder holder = (AvatarAdapter.AvatarViewHolder) view.getTag();
	  avatar_id_ = holder.id_.getText().toString().trim();
	} else {
	  avatar_id_ = "";	
	}
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
		if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB TEMP", "NewUserActivity::onNothingSelected.");
  }
}