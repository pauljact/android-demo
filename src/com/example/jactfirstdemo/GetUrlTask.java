package com.example.jactfirstdemo;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.util.ByteArrayBuffer;

import com.example.jactfirstdemo.JactLoggedInHomeActivity.OnTaskCompleted;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class GetUrlTask extends AsyncTask<String, Void, Void> {
  public enum TargetType {
    JSON,
    XML,	
    IMAGE
  }
  
  public enum FetchStatus {
	  SUCCESS,
	  ERROR_403,
	  ERROR_RESPONSE_CODE,
	  ERROR_PARAMS_LENGTH,
	  ERROR_WEBPAGE_TYPE,
	  ERROR_IO_EXCEPTION,
	  ERROR_OTHER_EXCEPTION,
	  ERROR_EMPTY_WEBPAGE,
	  ERROR_BAD_PARAMS
  }
  
  static final String COOKIES_HEADER = "Set-Cookie";
  private ProcessUrlResponseCallback callback_;
  private TargetType type_;
  private Bitmap picture_;
  private String webpage_;
  private String cookies_;
  private FetchStatus status_;

    // Constructor; sets calling_activity_ and type_.
    public GetUrlTask(ProcessUrlResponseCallback callback, TargetType type) {
      callback_ = callback;
      type_ = type;
      picture_ = null;
      webpage_ = "";
      cookies_ = "";
    }
    
    // 'params' are the String parameters passed in via the call to
    // GetUrlTask().execute(...). They are (order matters):
    //   1) (Mandatory) url
    //   2) (Mandatory) connection type (only valid values are 'GET' and 'POST')
    //   3) (Optional) cookies that should be used for the connection
    //   4) (Optional) info that should be sent with the request (e.g. url
    //      parameters); it is only used if connection type is 'POST'.
    protected Void doInBackground(String... params) {      
      // Sanity check params.
      if (params.length < 1) {
    	  Log.e("PHB", "Error: No url specified to fetch.");
    	  status_ = FetchStatus.ERROR_PARAMS_LENGTH;
    	  return null;
      } else if (params.length < 2 ||
    		     (params[1] != "GET" && params[1] != "POST")) { 
        Log.e("PHB", "Error: No connection type (GET or POST) specified.");
    	status_ = FetchStatus.ERROR_PARAMS_LENGTH;
    	return null;
      }
      
      // Use session cookies, if included in params.
      java.net.CookieManager cookie_manager = null;
      if (params.length > 2 && !params[2].isEmpty()) {
        cookie_manager = new java.net.CookieManager();
        List<String> cookie_headers = Arrays.asList(params[2].split("_|_"));
        for (String cookie : cookie_headers) {
          Log.e("PHB", "cookie: " + cookie);
          cookie_manager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
        }
      }
      
      // Initiate request.
      InputStream is = null;
      try {
    	// Read url and connection type from params.
       	URL url = new URL(params[0]);
       	String connection_type = params[1];  // Should be 'GET' or 'POST'.
        if (connection_type != "GET" && connection_type != "POST") {
          status_ = FetchStatus.ERROR_BAD_PARAMS;
          return null;
        }

    	// Set connection parameters.
       	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
       	connection.setReadTimeout(20000 /* milliseconds */);
       	connection.setConnectTimeout(25000 /* milliseconds */);
       	connection.setRequestMethod(connection_type);
       	connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(connection_type == "POST");
        //PHBconnection.setUseCaches (false);
        
        // Set cookies, if they were provided.
        if (cookie_manager != null &&
        	cookie_manager.getCookieStore().getCookies().size() > 0) {
          connection.setRequestProperty(
        	  "Cookie",
        	  TextUtils.join(",", cookie_manager.getCookieStore().getCookies()));
        }
        
        // Set url parameters, if present.
        if (connection_type == "POST" && params.length > 3) {
        	DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(params[3]);
            outputStream.flush();
            outputStream.close();
        }
        
        // Establish connection.
        connection.connect();
        
        // Get response, make sure it has code 200 (successful).
        int response = connection.getResponseCode();
       	if (response != 200) {
          if (response == 403) {
            // TODO(PHB): Handle error here.
            Log.e("bar", "Error: Server responded with 403, likely because " +
                         "logged-in credentials (cookies) were not properly " +
                         "transferred to the server.");
            status_ = FetchStatus.ERROR_403;
            return null;
          } else {
            // TODO(PHB): Handle error here.
    		Log.e("PHB", "Bad response (" + response + ") for task.");
            status_ = FetchStatus.ERROR_RESPONSE_CODE;
            return null;
          }
       	}
       	
        // Get cookies (will be required to pass to future requests for which
        // the server requires user to be logged-in to access).
        Map<String, List<String>> header_fields = connection.getHeaderFields();
        if (header_fields != null) {
          List<String> cookies_header_list = header_fields.get(COOKIES_HEADER);
          if (cookies_header_list != null && !cookies_header_list.isEmpty()) {
            cookies_ = TextUtils.join("_|_", cookies_header_list);
          }
        }
        
        // Parse response.
       	is = connection.getInputStream();
        if (type_ == TargetType.IMAGE) {
          picture_ = BitmapFactory.decodeStream(is);
        } else if (type_ == TargetType.JSON) {
          webpage_ = ParseResultAsString(is);
          if (webpage_.isEmpty()) {
        	  status_ = FetchStatus.ERROR_EMPTY_WEBPAGE;
        	  return null;
          }
        } else {
          Log.e("PHB", "Error: Unknown target url type.");
          status_ = FetchStatus.ERROR_WEBPAGE_TYPE;
          return null;
        }
      } catch (IOException e) {
        Log.e("PHB", "Error connecting to remote server:\n" + e.getMessage());
        // TODO(PHB): Handle Exception gracefully.
        status_ = FetchStatus.ERROR_IO_EXCEPTION;
        return null;
      } catch (NullPointerException e) {
          Log.e("PHB", "Null Ptr exception");
          // TODO(PHB): Handle Exception gracefully.
          status_ = FetchStatus.ERROR_IO_EXCEPTION;
          return null;
      } finally {
        // Makes sure that the InputStream is closed after the app is
        // finished using it.
        try {
          if (is != null) {
            is.close();
          }
        } catch (IOException ex) {
          // TODO(PHB): Handle exception.
          Log.e("PHB", "Error in closing is_ stream:\n" + ex.getMessage());
          status_ = FetchStatus.ERROR_OTHER_EXCEPTION;
          return null;
        }
      }
      status_ = FetchStatus.SUCCESS;
      return null;
    }

    private String ParseResultAsString(InputStream is) {
      try {
        // Convert the InputStream into a string
        BufferedInputStream bis = new BufferedInputStream(is);
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
	  
    protected void onPostExecute(Void result) {
      if (status_ == null || callback_ == null) {
    	  Log.e("PHB", "Null pointer here");
      }
      if (status_ != FetchStatus.SUCCESS) {
    	  callback_.ProcessFailedResponse(status_);
      } else if (!webpage_.isEmpty()) {
        callback_.ProcessUrlResponse(webpage_, cookies_);
      } else if (picture_ != null) {
        callback_.ProcessUrlResponse(picture_, cookies_);
      } else {
        Log.e("bar", "Error: Webpage not successfully parsed.");
        callback_.ProcessFailedResponse(FetchStatus.ERROR_OTHER_EXCEPTION);
        // TODO(PHB): Error. Abort gracefully.
      }
    }
}