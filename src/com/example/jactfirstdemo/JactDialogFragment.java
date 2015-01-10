package com.example.jactfirstdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

class JactDialogFragment extends DialogFragment {
	private String title_;
	private String message_;
	
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
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog.
        // Pass null as the parent view because its going in the dialog layout.
        View dialog_view = inflater.inflate(R.layout.jact_dialog_layout, null);
        
        // Construct Dialog.
        builder.setView(dialog_view);
        
        // Add title, if present.
        TextView title_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_title);
        if (title_tv == null) {
          Log.e("PHB ERROR", "JactDialogFragment::Constructor. Null title text view");
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
        TextView message_tv = (TextView) dialog_view.findViewById(R.id.jact_dialog_message);
        if (message_tv == null) {
          Log.e("PHB ERROR", "JactDialogFragment::Constructor. Null message text view");
          return builder.create();
        }
        if (!message_.isEmpty()) {
          message_tv.setText(message_);
        } else {
          message_tv.setHeight(0);
          message_tv.setVisibility(View.INVISIBLE);
        }
        
        return builder.create();
    }
}
