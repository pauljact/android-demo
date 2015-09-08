package com.jact.jactapp;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class GetUrlTask extends AsyncTask<String, Void, Void> {
  
  public static class UrlParams {
    public String url_;
    public String connection_type_;
    public String cookies_;
    public String post_string_;
    public String extra_info_;
    
    public UrlParams() {
      url_ = "";
      connection_type_ = "";
      cookies_ = "";
      post_string_ = "";
      extra_info_ = "";
    }
  }
  
  private class HeaderInfo {
	  private String name_;
	  private String value_;
	  
	  private HeaderInfo() {
		  name_ = "";
		  value_ = "";
	  }
  }
	
  public enum TargetType {
    JSON,
    XML,	
    IMAGE
  }
  
  public enum FetchStatus {
	  SUCCESS,
	  ERROR_403,
	  ERROR_NO_CONTROLLER,
	  ERROR_CSRF_FAILED,
	  ERROR_RESPONSE_CODE,
	  ERROR_PARAMS_LENGTH,
	  ERROR_WEBPAGE_TYPE,
	  ERROR_IO_EXCEPTION,
	  ERROR_OTHER_EXCEPTION,
	  ERROR_EMPTY_WEBPAGE,
	  ERROR_BAD_TYPE_PARAMS,
	  ERROR_BAD_POST_PARAMS,
	  ERROR_BAD_URL_PARAMS,
	  ERROR_BAD_FORM_ITEM,
	  ERROR_INPUT_STREAM_ERROR,
	  ERROR_OUTPUT_STREAM_ERROR,
	  ERROR_UNABLE_TO_CONNECT,
	  ERROR_PROTOCOL_EXCEPTION,
	  ERROR_WRITE_RESPONSE_ENCODING,
	  ERROR_WRITE_RESPONSE_IO,
	  ERROR_UNSUPPORTED_CONTENT_TYPE,
	  ERROR_NEW_USER_BAD_NAME,
	  ERROR_NEW_USER_MISMATCHING_PASSWORDS,
	  ERROR_NEW_USER_BAD_EMAIL,
	  ERROR_NEW_USER_BAD_AVATAR,
	  ERROR_UNKNOWN_406,
	  ERROR_UNRECOGNIZED_USERNAME,
	  ERROR_BAD_USERNAME_OR_PASSWORD,
	  ERROR_NEW_USER_DUPLICATE_EMAIL,
  }
  
  static public final String JACT_DOMAIN = "https://m.jact.com:3081";
  //static public final String JACT_DOMAIN = "https://uatm.jact.com:443";
  static final String COOKIES_HEADER = "Set-Cookie";
  static private final String HEADER_NAME_VALUE_SEPERATOR = "_PHB_HEADER_NAME_VALUE_PHB_";
  static private final String FORM_NAME_VALUE_SEPERATOR = "_PHB_FORM_NAME_VALUE_PHB_";
  static private final String HEADER_SEPERATOR = "_PHB_HEADER_PHB_";
  static private final String FORM_SEPERATOR = "_PHB_FORM_PHB_";
  static private final String HEADER_FORM_SEPERATOR = "_PHB_HEADER_FORM_PHB_";
  static public final String COOKIES_SEPERATOR = "_PHB_COOKIE_PHB_";
  static public final String READ_TIMED_OUT = "READ_TIME_OUT";
  private ProcessUrlResponseCallback callback_;
  private TargetType type_;
  private Bitmap picture_;
  private String webpage_;
  private String cookies_;
  private String extra_params_;
  private FetchStatus status_;

  // Constructor; sets calling_activity_ and type_.
  public GetUrlTask(ProcessUrlResponseCallback callback, TargetType type) {
    callback_ = callback;
    type_ = type;
    picture_ = null;
    webpage_ = "";
    cookies_ = "";
  }
    
  // Given a (name, value) pair that should be put in the header of an HttpRequest,
  // concatenates them, with a dummy token seperating them (when the actual request
  // is made, the parser will look for this token and format the request properly).
  static public String CreateHeaderInfo(String name, String value) {
    return name + HEADER_NAME_VALUE_SEPERATOR + value;
  }
    
  // Given a (name, value) pair that should be put in a form of an XMLHttpRequest,
  // concatenates them, with a dummy token seperating them (when the actual request
  // is made, the parser will look for this token and format the request properly).
  static public String CreateFormInfo(String name, String value) {
    return name + FORM_NAME_VALUE_SEPERATOR + value;
  }
    
  // Given an ArrayList of header_info and one of form_info, returns a String that should be put
  // into UrlParams.post_string_ in order to be parsed into an XMLHttpRequest.
  // Note that the items (Strings) in header_info should have been created by (or at least have
  // a format consistent with) GetUrlTask::CreateHeaderInfo(); similarly form_info's elements
  // should have been created using GetUrlTask::CreateFormInfo().
  static public String CreatePostString(ArrayList<String> header_info, ArrayList<String> form_info) {
    String to_return = "";
    
    // Parse header_info, putting values into to_return, with each seperated by HEADER_SEPERATOR.
    if (header_info != null) {
      for (String s : header_info) {
        if (!to_return.isEmpty()) {
          to_return += HEADER_SEPERATOR;
        }
        to_return += s;
      }
    }
      
    // Add the seperator that marks the division between header and form info.
    to_return += HEADER_FORM_SEPERATOR;
    
    // Parse form_info, putting values into to_return, with each seperated by FORM_SEPERATOR.
    String form_str = "";
    if (form_info != null) {
      for (String s : form_info) {
        if (!form_str.isEmpty()) {
          form_str += FORM_SEPERATOR;
        }
        form_str += s;
      }
    }
    to_return += form_str;
    return to_return;
  }
    
  public void execute(UrlParams params) {
    if (params == null) return;
    execute(params.url_, params.connection_type_, params.cookies_, params.post_string_, params.extra_info_);
  }
    
  // 'params' are the String parameters passed in via the call to
  // GetUrlTask().execute(...). They are (order matters):
  //   1) (Mandatory) url
  //   2) (Mandatory) connection type (only valid values are 'GET' and 'POST')
  //   3) (Optional) cookies that should be used for the connection
  //   4) (Optional) info that should be sent with the request (e.g. url
  //      parameters); it is only used if connection type is 'POST'.
  //   5) (Optional) *Hack*: parameter that will be passed to calling activity
  //      that provides the calling activity with additional info it may need
  //      to handle the response.    
  protected Void doInBackground(String... params) {      
    // Sanity check params.
    if (params.length < 1) {
  	  Log.e("PHB ERROR", "GetUrlTask::doInBackground. No url specified to fetch.");
   	  status_ = FetchStatus.ERROR_PARAMS_LENGTH;
   	  return null;
    } else if (params.length < 2 ||
   		       (!params[1].equals("GET") && !params[1].equals("POST") && !params[1].equals("PUT"))) {
      Log.e("PHB ERROR", "GetUrlTask::doInBackground. No connection type (GET, POST, or PUT) specified.");
      status_ = FetchStatus.ERROR_PARAMS_LENGTH;
      return null;
    }
    if (params.length > 4) {
      extra_params_ = params[4];
    } else {
      extra_params_ = "";
    }
      
    // Use session cookies, if included in params.
    java.net.CookieManager cookie_manager = null;
    if (params.length > 2 && !params[2].isEmpty()) {
      cookie_manager = new java.net.CookieManager();
      List<String> cookie_headers = Arrays.asList(params[2].split(COOKIES_SEPERATOR));
      for (String cookie : cookie_headers) {
        cookie_manager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
      }
    }
      
  	// Read url and connection type from params.
    URL url;
	try {
	  url = new URL(params[0]);
	} catch (MalformedURLException e1) {
	  // TODO(PHB) Auto-generated catch block
	  e1.printStackTrace();
      status_ = FetchStatus.ERROR_BAD_URL_PARAMS;
	  return null;
	}
    String connection_type = params[1];  // Should be 'GET' or 'POST' or 'PUT'.
    if (!connection_type.equals("GET") && !connection_type.equals("POST") && !connection_type.equals("PUT")) {
      status_ = FetchStatus.ERROR_BAD_TYPE_PARAMS;
      return null;
    }
      
    // Initiate request.
    HttpURLConnection connection;
	try {
	  connection = (HttpURLConnection) url.openConnection();
	} catch (IOException e1) {
	  // TODO(PHB): Auto-generated catch block
	  e1.printStackTrace();
	  status_ = FetchStatus.ERROR_UNABLE_TO_CONNECT;
	  return null;
	}

    // Set connection parameters.
    connection.setReadTimeout(20000 /* milliseconds */);
    connection.setConnectTimeout(25000 /* milliseconds */);
    try {
	  connection.setRequestMethod(connection_type);
	} catch (ProtocolException e1) {
	  // TODO Auto-generated catch block
	  e1.printStackTrace();
      status_ = FetchStatus.ERROR_PROTOCOL_EXCEPTION;
	  return null;
	}
    connection.setDoInput(true);
    connection.setDoOutput(connection_type.equals("POST") || connection_type.equals("PUT"));
    // PHB TEMP. Print request.
    Log.w("PHB", "GetUrlTask::execute. Doing " + connection_type + " to " + params[0] +
  	             "; and with extra task info: " + (params.length > 4 ? params[4] : ""));
       	
    // Process POST/PUT parameters.
    if (params.length > 3 && params[3].contains(HEADER_FORM_SEPERATOR)) {
      ArrayList<HeaderInfo> header_info = new ArrayList<HeaderInfo>();
      params[3] = ParseHeadersFromParams(params[3], header_info);
      if (params[3].equals("PHB_ERROR")) {
        status_ = FetchStatus.ERROR_BAD_POST_PARAMS;
        return null;
      }
      for (HeaderInfo info : header_info) {
        connection.setRequestProperty(info.name_, info.value_);
      }
    } else {
      connection.setRequestProperty("Content-Type", "application/json");
    }
        
    // Set cookies, if they were provided.
    if (cookie_manager != null &&
      	cookie_manager.getCookieStore().getCookies().size() > 0) {
      connection.setRequestProperty(
    	  "Cookie", TextUtils.join(",", cookie_manager.getCookieStore().getCookies()));
    }
        
    // Set url parameters, if present.
    if ((connection_type.equals("POST") || connection_type.equals("PUT") || connection_type.equals("GET")) &&
        params.length > 3  && !params[3].isEmpty()) {
      OutputStream os;
	  try {
		os = connection.getOutputStream();
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		status_ = FetchStatus.ERROR_OUTPUT_STREAM_ERROR;
		return null;
	  }
      BufferedWriter writer;
	  try {
		writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
		writer.write(params[3]);
	    writer.flush();
	    writer.close();
	    os.close();
	  } catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    status_ = FetchStatus.ERROR_WRITE_RESPONSE_ENCODING;
		return null;
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    status_ = FetchStatus.ERROR_WRITE_RESPONSE_IO;
		return null;
	  }
    }
        
    // Establish connection.
    try {
	  connection.connect();
	} catch (IOException e1) {
	  // TODO Auto-generated catch block
	  e1.printStackTrace();
	  status_ = FetchStatus.ERROR_UNABLE_TO_CONNECT;
	  return null;
	}
        
    // Get response, make sure it has code 200 (successful).
    int response = 0;
    String input_stream = "";
    String error_stream = "";
    InputStream is = null;
    try {
      response = connection.getResponseCode();
      is = connection.getInputStream();
      if (type_ == TargetType.IMAGE) {
        picture_ = BitmapFactory.decodeStream(is);
      } else if (type_ == TargetType.JSON) {
        input_stream = ParseResultAsString(is);
      } else {
        Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unknown target url type.");
        status_ = FetchStatus.ERROR_WEBPAGE_TYPE;
        return null;
      }
    } catch (IOException e) {
      Log.w("PHB ERROR", "GetUrlTask::doInBackground. Failed to get response code and/or InputStream.");
	  // Surround fetching of the response code with try/catch is neccessary when the server returns 401
	  // errors; see e.g. this post explaining it:
	  // http://stackoverflow.com/questions/17121213/java-io-ioexception-no-authentication-challenges-found
	  // As suggested, try fetching the response code again now.
	  // Will return 401, because now connection has the correct internal state.
	  int responsecode = 0;
	  try {
	    responsecode = connection.getResponseCode();
	    is = connection.getErrorStream();
	    error_stream = ParseResultAsString(is);
	  } catch (IOException second_level_ex) {
	    Log.e("PHB ERROR", "GetUrlTask::doInBackground. Second level exeception. Input Stream: " +
	    		           input_stream + ". PHB. Error string: " + error_stream +
	   					   "; responsecode" + responsecode);
	  }
	  status_ = GetStatusForFailedResponse(responsecode, is, input_stream, error_stream);
	  return null;
    }

    // Handle HTTP Error codes.
    if (response != 200) {
      status_ = GetStatusForFailedResponse(response, is, input_stream, error_stream);
      return null;
    }
    
    // Handle special input_stream: Hack: ParseResultAsString returns READ_TIMED_OUT, since
    // for some reason this gets a response code of 200...not sure why.
    if (input_stream.equals(READ_TIMED_OUT)) {
      status_ = FetchStatus.ERROR_UNABLE_TO_CONNECT;
      return null;
    }
  	  
    if (input_stream.isEmpty() && picture_ == null) {
      status_ = FetchStatus.ERROR_EMPTY_WEBPAGE;
      return null;
    }
    webpage_ = input_stream;
        
    // Get cookies (will be required to pass to future requests for which
    // the server requires user to be logged-in to access).
    Map<String, List<String>> header_fields = connection.getHeaderFields();
    if (header_fields != null) {
      List<String> cookies_header_list = header_fields.get(COOKIES_HEADER);
      if (cookies_header_list != null && !cookies_header_list.isEmpty()) {
        cookies_ = TextUtils.join(COOKIES_SEPERATOR, cookies_header_list);
        Log.i("PHB", "BHP cookie header_length: " + cookies_header_list.size() + ", saving cookies:\n" + cookies_);
      }
    }
    status_ = FetchStatus.SUCCESS;
    return null;
  }
  
  private FetchStatus GetStatusForFailedResponse(
		  int responsecode, InputStream is, String input_stream, String error_stream) {
	FetchStatus to_return;
    if (responsecode == 400) {
	  Log.e("PHB ERROR", "GetUrlTask::doInBackground. Server didn't recognize a form field. Response (" +
	                     responsecode + "); response:\n" + input_stream +
	                     ". PHB. Error string: " + error_stream);
	  to_return = FetchStatus.ERROR_BAD_FORM_ITEM;
    } else if (responsecode == 401) {
  	  Log.w("PHB", "GetUrlTask::doInBackground. CSRF Failed. Response (" +
  	               responsecode + "); response:" + input_stream +
  	               ". PHB. Error string: " + error_stream);
  	  if (error_stream.indexOf("\"Missing required argument data\"") >= 0) {
  	    to_return = FetchStatus.ERROR_UNSUPPORTED_CONTENT_TYPE;
  	  } else if (error_stream.indexOf("Wrong username or password") >= 0) {
  		to_return = FetchStatus.ERROR_BAD_USERNAME_OR_PASSWORD;
  	  } else {
  	    to_return = FetchStatus.ERROR_CSRF_FAILED;
  	  }    
    } else if (responsecode == 403) {
      // TODO(PHB): Handle error here.
      Log.e("PHB ERROR", "GetUrlTask::doInBackground. " +
                         "Server responded with 403, likely because " +
                         "logged-in credentials (cookies) were not properly " +
                         "transferred to the server.");
      to_return = FetchStatus.ERROR_403;
    } else if (responsecode == 404) {
      Log.e("PHB ERROR", "GetUrlTask::doInBackground. Response 404. error: " + error_stream);	
      to_return = FetchStatus.ERROR_NO_CONTROLLER;
    } else if (responsecode == 406) {
      Log.e("PHB ERROR", "GetUrlTask::doInBackground. Response 406. error: " + error_stream);
      if (error_stream.indexOf("The name") >= 0 && error_stream.indexOf("is already taken.") >= 0) {
    	to_return = FetchStatus.ERROR_NEW_USER_BAD_NAME;
      } else if (error_stream.indexOf("The specified passwords do not match.") >= 0) {
    	to_return = FetchStatus.ERROR_NEW_USER_MISMATCHING_PASSWORDS;
      } else if (error_stream.indexOf("The e-mail address ") >= 0 && error_stream.indexOf("is not valid") >= 0) {
    	to_return = FetchStatus.ERROR_NEW_USER_BAD_EMAIL;
      } else if (error_stream.indexOf("The e-mail address ") >= 0 && error_stream.indexOf("is already registered") >= 0) {
      	to_return = FetchStatus.ERROR_NEW_USER_DUPLICATE_EMAIL;
      } else if (error_stream.indexOf("An illegal choice has been detected.") >= 0) {
    	to_return = FetchStatus.ERROR_NEW_USER_BAD_AVATAR;
      } else if (error_stream.indexOf("is not recognized as a user") >= 0) {
    	to_return = FetchStatus.ERROR_UNRECOGNIZED_USERNAME;
      } else {
    	to_return = FetchStatus.ERROR_UNKNOWN_406;
      }
    } else {
	  Log.e("PHB ERROR", "GetUrlTask::doInBackground. Bad response (" +
	                     responsecode + "); response:\n" + input_stream +
	                     ". PHB. Error string: " + error_stream);
	  to_return = FetchStatus.ERROR_RESPONSE_CODE;
	}
    
	// Makes sure that the InputStream is closed after the app is finished using it.
	if (is != null) {
	  try {
	    is.close();
	  } catch (IOException e5) {
		// TODO Auto-generated catch block
		e5.printStackTrace();
		return FetchStatus.ERROR_INPUT_STREAM_ERROR;
      }
	}
	return to_return;
  }

  private String ParseHeadersFromParams(String params, ArrayList<HeaderInfo> header_info) {
    String error_msg = "PHB_ERROR";
	int seperator_pos = params.indexOf(HEADER_FORM_SEPERATOR);
    boolean empty_header = seperator_pos == 0;
    boolean empty_form = seperator_pos == (params.length() - HEADER_FORM_SEPERATOR.length());
    ArrayList<String> header_and_form = new ArrayList<String>(Arrays.asList(params.split(HEADER_FORM_SEPERATOR)));
    if (header_and_form.size() > 2) {
      Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for UrlParams.post_params_: " +
                         "size=" + header_and_form.size() + ", post_params_: " + params);
      return error_msg;
    }
    if (!empty_header) {
      if (header_and_form.size() < 1) {
        Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for UrlParams.post_params_: " +
                           "size =" + header_and_form.size() + ", post_params_: " + params);
        return error_msg;
      }
      String headers_str = header_and_form.get(0);
      if (!headers_str.isEmpty()) {
        ArrayList<String> headers = new ArrayList<String>(Arrays.asList(headers_str.split(HEADER_SEPERATOR)));
        for (String header : headers) {
          ArrayList<String> name_value = new ArrayList<String>(Arrays.asList(header.split(HEADER_NAME_VALUE_SEPERATOR)));
          if (name_value.size() != 2) {
            Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for header. " +
                               "size=" + name_value.size() + ", header: " + header);
            return error_msg;
          }
          Log.i("PHB", "GetUrlTask::doInBackground. Header Name: " + name_value.get(0) +
                       ", Header Value: " + name_value.get(1));
          HeaderInfo new_header = new HeaderInfo();
          new_header.name_ = name_value.get(0);
          new_header.value_ = name_value.get(1);
          header_info.add(new_header);
        }
      }
    }
	
    // Now parse form info.
    String form_info = "";
	String json_str = "";
   	if (empty_form) return "";
   	String forms_str = "";
 	if (!empty_header) {
 	  if (header_and_form.size() != 2) {
   	    Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for UrlParams.post_params_: " +
   	                       "size = " + header_and_form.size() + ", post_params_: " + params);
   	    return error_msg;
 	  }
 	  forms_str = header_and_form.get(1);
 	} else if (header_and_form.size() == 2) {
 	  // Verify first part (header) is empty; then set forms_str to second part.
 	  if (!header_and_form.get(0).isEmpty()) {
 	 	Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for UrlParams.post_params_: " +
                           "size = " + header_and_form.size() + ", header_and_form.get(0): " +
                           header_and_form.get(0) + ", header_and_form.get(1): " +
                           header_and_form.get(1) + ", post_params_: " + params);
 	 	return error_msg;
 	  }
 	  forms_str = header_and_form.get(1);
   	} else if (header_and_form.size() != 1) {
 	  Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for UrlParams.post_params_: " +
 	                     "size = " + header_and_form.size() + ", post_params_: " + params);
 	  return error_msg;
   	} else {
 	  forms_str = header_and_form.get(0);
   	}
 	
   	if (forms_str.isEmpty()) return "";
    ArrayList<String> forms = new ArrayList<String>(Arrays.asList(forms_str.split(FORM_SEPERATOR)));
   	JSONObject json = new JSONObject();
   	try {
   	  for (String form : forms) {
        ArrayList<String> name_value = new ArrayList<String>(Arrays.asList(form.split(FORM_NAME_VALUE_SEPERATOR)));
     	if (name_value.size() != 2) {
       	  Log.e("PHB ERROR", "GetUrlTask::doInBackground. Unexpected format for form. " +
                             "size=" + name_value.size() + ", form: " + form);
  	      return error_msg;
  	    }
     	if (!form_info.isEmpty()) {
     	  form_info += "&";
     	}
     	//Log.i("PHB", "GetUrlTask::doInBackground. Adding form data, name: " + name_value.get(0) +
	    //           ", value: " + name_value.get(1));
     	form_info += name_value.get(0) + "=" + name_value.get(1);
   	    json.put(name_value.get(0), name_value.get(1)); 
      }
   	  json_str = json.toString();
   	} catch (JSONException e) {
   	  Log.e("PHB ERROR", "JactLoginActivity::Login. Error logging in:\n" + e.getMessage());
   	  // TODO(PHB): Implement this.
   	}
 	Log.i("PHB", "GetUrlTask::doInBackground. form_str: " + form_info);
 	Log.i("PHB", "GetUrlTask::doInBackground. json_str: " + json_str);
   	return json_str;
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
      Log.e("PHB ERROR", "GetUrlTask::ParseResultAsString. Error reading returned webpage:\n" + e.getMessage());
  	  if (e.getMessage().indexOf("Read timed out") >= 0) {
  		return READ_TIMED_OUT;
  	  }
  	  return "";
    }
  }
	  
  protected void onPostExecute(Void result) {
    if (callback_ == null) {
      Log.e("PHB ERROR", "GetUrlTask::onPostExecute. Null pointer here");
      return;
    }
    if (status_ == null) {
      Log.e("PHB ERROR", "GetUrlTask::onPostExecute. Null status");
  	  callback_.ProcessFailedResponse(status_, extra_params_);
    } else if (status_ != FetchStatus.SUCCESS) {
      Log.e("PHB ERROR", "GetUrlTask::onPostExecute. Non Success");
      callback_.ProcessFailedResponse(status_, extra_params_);
    } else if (webpage_ != null && !webpage_.isEmpty()) {
      callback_.ProcessUrlResponse(webpage_, cookies_, extra_params_);
    } else if (picture_ != null) {
      callback_.ProcessUrlResponse(picture_, cookies_, extra_params_);
    } else {
   	  Log.e("PHB ERROR", "GetUrlTask::onPostExecute. Webpage not successfully parsed.");
      callback_.ProcessFailedResponse(FetchStatus.ERROR_OTHER_EXCEPTION, extra_params_);
      // TODO(PHB): Error. Abort gracefully.
    }
  }
}