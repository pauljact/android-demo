package com.example.jactfirstdemo;

import com.example.jactfirstdemo.GetUrlTask.FetchStatus;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class JactNavigationDrawer implements ProcessUrlResponseCallback {
  /* Non-SSL sites.
  private static final String rewards_url_ = "http://us7.jact.com:3080/rest/rewards.json";
  private static final String faq_url_ = "http://us7.jact.com:3080/faq-page";
  private static final String privacy_policy_url_ = "http://us7.jact.com:3080/privacy-policy";
  private static final String user_agreement_url_ = "http://us7.jact.com:3080/user-agreement";
  private static final String about_jact_url_ = "http://us7.jact.com:3080/about-jact";
  */
  /* Mobile sites */
  private static final String rewards_url_ = "https://m.jact.com:3081/rest/rewards.json";
  private static final String faq_url_ = "https://m.jact.com:3081/faq-page";
  private static final String contact_us_url_ = "https://m.jact.com:3081/contact";
  private static final String privacy_policy_url_ = "https://m.jact.com:3081/privacy-policy";
  private static final String user_agreement_url_ = "https://m.jact.com:3081/user-agreement";
  private static final String about_jact_url_ = "https://m.jact.com:3081/about-jact";
  /**/
  /* SSL, non-mobile sites
  private static final String rewards_url_ = "https://us7.jact.com:3081/rest/rewards.json";
  private static final String faq_url_ = "https://us7.jact.com:3081/faq-page";
  private static final String contact_us_url_ = "https://us7.jact.com:3081/contact";
  private static final String privacy_policy_url_ = "https://us7.jact.com:3081/privacy-policy";
  private static final String user_agreement_url_ = "https://us7.jact.com:3081/user-agreement";
  private static final String about_jact_url_ = "https://us7.jact.com:3081/about-jact";
  */
  public enum ActivityIndex {
    PROFILE,
    GAMES,
    REWARDS,
    COMMUNITY,
    LOGIN,
    PRIVACY_POLICY,
    FAQ,
    CONTACT_US,
    USER_AGREEMENT,
    CHECKOUT_VIA_MOBILE_SITE,
    CHECKOUT_MAIN,
    SHIPPING,
    SHIPPING_NEW,
    BILLING,
    BILLING_NEW,
    REVIEW_ORDER,
    EARN,
    BUX,
    ABOUT_JACT,
    FORGOT_PASSWORD,
    REGISTER,
  }

  private ActivityIndex parent_activity_index_;
  private int drawer_click_pos_;
  private DrawerLayout drawer_layout_;
  private ListView drawer_list_view_;
  private ActionBarDrawerToggle drawer_toggle_;
  private JactActionBarActivity parent_activity_;
  
  public JactNavigationDrawer(JactActionBarActivity a,
		                      View drawer_layout,
		                      View left_drawer,
		                      ActivityIndex parent_activity_index) {
    parent_activity_ = a;
    parent_activity_index_ = parent_activity_index;
    drawer_click_pos_ = -1;
    drawer_layout_ = (DrawerLayout) drawer_layout;
    
    // Toggles the icon that gets displayed in left side of Action Bar
    // (the "UP" button, also the menu button), based on whether the
    // drawer is open or closed.
    drawer_toggle_ =
        new ActionBarDrawerToggle(parent_activity_, drawer_layout_, 0, 0) {
          // Called when a drawer has settled in a completely closed state.
          public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            // Set default item selected to be the parent activity.
            if (ActivityToDrawerIndex(parent_activity_index_) >= 0) {
              drawer_list_view_.setItemChecked(ActivityToDrawerIndex(parent_activity_index_), true);
            }
          }
        };
    
    drawer_layout_.setDrawerListener(drawer_toggle_);
    drawer_list_view_ = (ListView) left_drawer;
    IconAndText menu_data[] = new IconAndText[] {
        new IconAndText(R.drawable.profile_transparent, "My Profile"),
        new IconAndText(R.drawable.trophy_transparent, "Rewards"),
        //PHB_BUXnew IconAndText(R.drawable.jact_transparent, "Purchase BUX"),
        new IconAndText(R.drawable.earn_transparent, "Earn"),
        new IconAndText(R.drawable.games_transparent, "Games"),
        new IconAndText(R.drawable.community2_transparent, "Community"),
        new IconAndText(R.drawable.cart_white_transparent, "Shopping Cart")
    };
    
    drawer_list_view_.setAdapter(new JactMenuAdapter(
        parent_activity_, R.layout.drawer_list_icon, menu_data));
    
    drawer_list_view_.setOnItemClickListener(new DrawerItemClickListener(this));
    
    // Set default item selected to be the parent activity.
    drawer_list_view_.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    if (ActivityToDrawerIndex(parent_activity_index_) >= 0) {
      drawer_list_view_.setItemChecked(ActivityToDrawerIndex(parent_activity_index_), true);
    }
  }
  
  public void setActivityIndex(ActivityIndex a) {
	parent_activity_index_ = a;
  }
  
  private int ActivityToDrawerIndex(ActivityIndex i) {
    switch (i) {
    case PROFILE:
      return 0;
    case REWARDS:
      return 1;
    //PHB_BUXcase BUX:
    //PHB_BUX  return 2;
    case EARN:
      return 2;
    case GAMES:
      return 3;
    case COMMUNITY:
      return 4;
    case CHECKOUT_MAIN:
      return 5;
    default:
      return -1;
    }
  }
  
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean found_match = true;
    switch (item.getItemId()) {
      case android.R.id.home:
        // This ID represents the 'UP' button, which for this activity is the 
    	// Jact Menu Drawer. Open or Close the drawer, as required.
        if (drawer_layout_.isDrawerOpen(drawer_list_view_)) {
          drawer_layout_.closeDrawer(drawer_list_view_);
        } else {
          drawer_layout_.openDrawer(drawer_list_view_);
        }
        break;
      case android.R.id.navigationBarBackground:
        // TODO(PHB): Not sure which click will cause this to happen.
    	// Log it, and implement later if necessary.
    	//Log.e("PHB", "JactNavigationDrawer.onOptionsItemSelected: Clicked on Navigation Bar Background");
        break;
      case android.R.id.selectedIcon:
        // TODO(PHB): Not sure which click will cause this to happen.
      	// Log it, and implement later if necessary.
    	//Log.e("PHB", "JactNavigationDrawer.onOptionsItemSelected: Clicked on SelectedIcon");
        break;
      case android.R.id.summary:
        // TODO(PHB): Not sure which click will cause this to happen.
      	// Log it, and implement later if necessary.
    	//Log.e("PHB", "JactNavigationDrawer.onOptionsItemSelected: Clicked on Summary");
        break;
      case android.R.id.widget_frame:
        // TODO(PHB): Not sure which click will cause this to happen.
        // Log it, and implement later if necessary.
    	//Log.e("PHB", "JactNavigationDrawer.onOptionsItemSelected: Clicked on Widget Frame");
        break;
      case R.id.menu_shopping_cart:
  	    if (parent_activity_index_ == ActivityIndex.CHECKOUT_MAIN) {
  	      // Nothing to do: User clicked on Cart Icon, which is the current activity.
  	      break;
  	    }
    	parent_activity_.fadeAllViews(true);
  	    startCheckoutActivity();
        break;
      case R.id.menu_main_profile:
	    if (parent_activity_index_ == ActivityIndex.PROFILE) {
	      // Nothing to do: User clicked on 'My Profile', which is the current activity.
	      break;
	    }
      	parent_activity_.fadeAllViews(true);
	    startMyProfileActivity();
        break;
      case R.id.menu_about_jact:
  	    if (parent_activity_index_ == ActivityIndex.ABOUT_JACT) {
  	      // Nothing to do: User clicked on 'About Jact', which is the current activity.
  	      break;
  	    }
       	parent_activity_.fadeAllViews(true);
       	startWebViewActivity(about_jact_url_, parent_activity_.getString(R.string.menu_about_jact));
        break;
      case R.id.menu_contact_us:
	    if (parent_activity_index_ == ActivityIndex.CONTACT_US) {
  	      // Nothing to do: User clicked on 'Contact Us', which is the current activity.
  	      break;
  	    }
       	parent_activity_.fadeAllViews(true);
       	startWebViewActivity(contact_us_url_, parent_activity_.getString(R.string.menu_contact_us));
       	break;
      case R.id.menu_faq:
  	    if (parent_activity_index_ == ActivityIndex.FAQ) {
  	      // Nothing to do: User clicked on 'FAQ', which is the current activity.
  	      break;
  	    }
       	parent_activity_.fadeAllViews(true);
       	startWebViewActivity(faq_url_, parent_activity_.getString(R.string.menu_faq));
        break;
      case R.id.menu_user_agreement:
  	    if (parent_activity_index_ == ActivityIndex.USER_AGREEMENT) {
  	      // Nothing to do: User clicked on 'User Agreement', which is the current activity.
  	      break;
  	    }
       	parent_activity_.fadeAllViews(true);
       	startWebViewActivity(user_agreement_url_, parent_activity_.getString(R.string.menu_user_agreement));
        break;
      case R.id.menu_privacy_policy:
  	    if (parent_activity_index_ == ActivityIndex.PRIVACY_POLICY) {
  	      // Nothing to do: User clicked on 'Privacy Policy', which is the current activity.
  	      break;
  	    }
       	parent_activity_.fadeAllViews(true);
       	startWebViewActivity(privacy_policy_url_, parent_activity_.getString(R.string.menu_privacy_policy));
        break;
      case R.id.menu_logout:
       	parent_activity_.fadeAllViews(true);
    	startLogoffActivity();
        break;
      default:
        found_match = false;
    }
    if (found_match) return true;
    if (drawer_toggle_.onOptionsItemSelected(item)) {
      return true;
    }
    return false;
  }
  
  protected void onPostCreate(Bundle savedInstanceState) {
    // Sync the toggle state after onRestoreInstanceState has occurred.
    drawer_toggle_.syncState();
  }

  public void onConfigurationChanged(Configuration newConfig) {
    drawer_toggle_.onConfigurationChanged(newConfig);
  }
  
  private void startWebViewActivity(String url, String title) {
    FaqActivity.SetUrlAndTitle(url, title);
    parent_activity_.startActivity(new Intent(parent_activity_, FaqActivity.class));
  }
  
  private void startLogoffActivity() {
    Intent login_intent =
        new Intent(parent_activity_, JactLoginActivity.class);
    login_intent.putExtra(parent_activity_.getString(R.string.logged_off_key), "true");
    parent_activity_.startActivity(login_intent);
  }
 
  private void startMyProfileActivity() {
    parent_activity_.startActivity(new Intent(parent_activity_, JactLoggedInHomeActivity.class));
  }
 
  private void startCheckoutActivity() {
    parent_activity_.startActivity(new Intent(parent_activity_, ShoppingCartActivity.class));
  }
  
  private void startEarnActivity() {
	EarnActivity.SetShouldRefreshEarnItems(true);
    parent_activity_.startActivity(new Intent(parent_activity_, EarnActivity.class));
  }
  
  private void startGamesActivity() {
    parent_activity_.startActivity(new Intent(parent_activity_, GamesActivity.class));
  }
  
  private void startCommunityActivity() {
    parent_activity_.startActivity(new Intent(parent_activity_, CommunityActivity.class));
  }
  
  public void ProcessDrawerClickResponse(String webpage, String cookies) {
    // Only should have empty webpage if click was on 'My Profile'.
    if (webpage.equals("") && drawer_click_pos_ != 0) {
      // TODO(PHB): Handle failed GET.
      Log.e("PHB ERROR", "JactNavigationDrawer::ProcessDrawClickResponse. Empty webpage.");
    } else if (drawer_click_pos_ == ActivityToDrawerIndex(parent_activity_index_)) {
      // Nothing to do; clicked on parent activity.
    } else if (drawer_click_pos_ == 0) {
      // Start 'My Profile' activity.
      startMyProfileActivity();
    } else if (drawer_click_pos_ == 1) {
      // Start 'ProductsActivity' (for Rewards).
      Intent products_activity =
          new Intent(parent_activity_, ProductsActivity.class);
      products_activity.putExtra("server_response", webpage);
      parent_activity_.startActivity(products_activity);
    //PHB_BUX} else if (drawer_click_pos_ == 2) {
    //PHB_BUX  // Start 'PurchaseBuxActivity' (for Purchase BUX).
    } else if (drawer_click_pos_ == 2) {
      // Position 2 corresponds to 'Earn'. We should never be here, as a click on this
      // drawer position should not have sent a request to the server here (rather,
      // EarnActivity should have been started directly).
      // TODO(PHB): Handle the error of being here.
      Log.e("PHB ERROR", "JactNavigationDrawer::ProcessDrawerClickResponse. Shouldn't be here for 'Earn'.");
    } else if (drawer_click_pos_ == 3) {
      // Position 3 corresponds to 'Games'. We should never be here, as a click on this
      // drawer position should not have sent a request to the server here (rather,
      // GamesActivity should have been started directly).
      // TODO(PHB): Handle the error of being here.
      Log.e("PHB ERROR", "JactNavigationDrawer::ProcessDrawerClickResponse. Shouldn't be here for 'Games'.");
    } else if (drawer_click_pos_ == 4) {
      // Position 4 corresponds to 'Community'. We should never be here, as a click on this
      // drawer position should not have sent a request to the server here (rather,
      // CommunityActivity should have been started directly).
      // TODO(PHB): Handle the error of being here.
      Log.e("PHB ERROR", "JactNavigationDrawer::ProcessDrawerClickResponse. Shouldn't be here for 'Community'.");
    } else {
      Log.e("PHB ERROR", "JactNavigationDrawer::ProcessDrawClickResponse. Unexpected drawer pos: " + drawer_click_pos_);
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
	    JactActionBarActivity context_;
	    private int layout_res_id_;
	    private IconAndText items_[] = null;

	    public JactMenuAdapter(JactActionBarActivity context, int layout_id, IconAndText[] data) {
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
	      views.text_view_.setText(list_item.text_);
	      views.img_view_.setImageResource(list_item.icon_res_id_);
	      return row;
	    }
	  }

	  private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    private JactNavigationDrawer parent_class_;
	    
	    public DrawerItemClickListener(JactNavigationDrawer a) {
	      parent_class_ = a;
	    }
	    
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	      drawer_click_pos_ = position;
	      drawer_layout_.closeDrawer(drawer_list_view_);
	      if (position == ActivityToDrawerIndex(parent_activity_index_)) {
	        // Nothing to do except close drawer.
	      } else if (position == 0) {
	        // Position '0' is for 'My Profile. Start 'JactLoggedInHomeActivity'.
	    	// Must be done via parent_class, which has access to the parent_activity.
		    parent_class_.parent_activity_.fadeAllViews(true);
	    	parent_class_.ProcessDrawerClickResponse("", "");
	      } else if (position == 1) {
	        // Position '1' is for Rewards (Products). Start that activity.
	    	new GetUrlTask((ProcessUrlResponseCallback) parent_class_, GetUrlTask.TargetType.JSON).execute(
	        		rewards_url_, "GET");
	    	parent_class_.parent_activity_.fadeAllViews(true);
	      //PHB_BUX} else if (position == 2) {
	    	//PHB_BUX// Position '2' is for Purchase BUX. Start that activity.
	      } else if (position == 2) {
            // Position '2' is for Earn. Start that activity.
	    	parent_class_.parent_activity_.fadeAllViews(true);
	    	parent_class_.startEarnActivity();
	      } else if (position == 3) {
	        // Position '3' is for Games. Start that activity.
		    parent_class_.parent_activity_.fadeAllViews(true);
		    parent_class_.startGamesActivity();
	      } else if (position == 4) {
	        // Position '4' is for Community. Start that activity.
		    parent_class_.parent_activity_.fadeAllViews(true);
		    parent_class_.startCommunityActivity();
	      } else if (position == 5) {
	    	// Position '5' is for Shopping Cart. Start that activity.
	        parent_class_.startCheckoutActivity();
	    	parent_class_.parent_activity_.fadeAllViews(true);
	      } else {
	        // Only 7 drawers expected. Error.
	        Log.e("PHB Error", "JactNavigationDrawer::onItemClick. Unexpected position: " + position);
	      }
	    }
	  }

	@Override
	public void ProcessUrlResponse(String webpage, String cookies, String extra_params) {
	  ProcessDrawerClickResponse(webpage, cookies);
	}

	@Override
	public void ProcessUrlResponse(Bitmap pic, String cookies, String extra_params) {
	  // TODO(PHB): Implement this.
	  Log.e("PHB ERROR", "JactNavigationDrawer.ProcessUrlResponse: " +
	                     "This should never be called for a bitmap input param.");
	}

	@Override
	public void ProcessFailedResponse(FetchStatus status, String extra_params) {
	  Log.e("PHB ERROR", "JactNavigationDrawer.ProcessUrlResponse: " +
                         "Failed to fetch URL. Status: " + status);
	  parent_activity_.fadeAllViews(false);
	}
}
