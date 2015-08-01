package com.example.jactfirstdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

class JactDialogFragment extends DialogFragment {
	private String title_;
	private String message_;
	private String button_one_text_;
	private String button_two_text_;
	
	public JactDialogFragment() {
	  title_ = "";
	  message_ = "";
	}
	public JactDialogFragment(String title) {
	  title_ = title;
	  message_ = "";
	}
	public JactDialogFragment(String title, String message) {
	  title_ = title;
	  message_ = message;
	}
	
	public void SetButtonOneText(String text) {
	  button_one_text_ = text; 
	}
	
	public void SetButtonTwoText(String text) {
	  button_two_text_ = text;
	}
	
	/* PHB attempting to fix a bug that happened when an Error Dialog tried to be launched when 
	 * the activity that called for it was no longer active. Instead, this bug was addressed
	 * by Overriding onActivityResult and onPostResume, and via boolean show_dialog_.
	@Override
	public void show(FragmentManager manager, String message) {
	  if (!StartActivity.this.isFinishing()) {
	    this.show(manager, message);
	  }
	}*/
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog.
        // Pass null as the parent view because its going in the dialog layout.
        View dialog_view;
        TextView title_tv;
        TextView message_tv;
        Button button_one;
        Button button_two = null;
        if (button_two_text_ == null || button_two_text_.isEmpty()) {
          dialog_view = inflater.inflate(R.layout.jact_dialog_layout, null);
          title_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_title);
          message_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_message);
          button_one = (Button) dialog_view.findViewById(R.id.jact_dialog_button);
        } else {
          dialog_view = inflater.inflate(R.layout.jact_dialog_two_layout, null);
          title_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_two_title);
          message_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_two_message);
          button_one = (Button) dialog_view.findViewById(R.id.jact_dialog_two_button);
          button_two = (Button) dialog_view.findViewById(R.id.jact_dialog_two_button_two);
        }
        
        // Construct Dialog.
        builder.setView(dialog_view);
        
        // Add title, if present.
        if (title_tv == null) {
          if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "JactDialogFragment::Constructor. Null title text view");
          return builder.create();
        }
        if (!title_.isEmpty()) {
          title_tv.setText(title_);
        } else {
          title_tv.setHeight(0);
          title_tv.setVisibility(View.INVISIBLE);
          View divider_bar = dialog_view.findViewById(R.id.jact_dialog_divider);
          divider_bar.setVisibility(View.INVISIBLE);
        }
        
        // Add message, if present.
        if (message_tv == null) {
          if (!JactActionBarActivity.IS_PRODUCTION) Log.e("PHB ERROR", "JactDialogFragment::Constructor. Null message text view");
          return builder.create();
        }
        if (!message_.isEmpty()) {
          message_tv.setText(message_);
        } else {
          message_tv.setHeight(0);
          message_tv.setVisibility(View.INVISIBLE);
        }
        
        // Set Button One text, if present.
        if (button_one_text_ != null && !button_one_text_.isEmpty()) {
          button_one.setText(button_one_text_);
        }
        // Set Button Two text, if present.
        if (button_two_text_ != null && !button_two_text_.isEmpty() && button_two != null) {
          button_two.setText(button_two_text_);
        }
        
        return builder.create();
    }
}
