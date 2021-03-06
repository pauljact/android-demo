package com.jact.jactapp;

import android.net.http.SslError;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class JactWebViewClient extends WebViewClient {
  private JactActionBarActivity parent_;
  private WebView current_webview_;
  
  public JactWebViewClient(JactActionBarActivity parent) {
	parent_ = parent;
  }
	
  @Override
  public void onPageFinished(WebView view, String url) {
	parent_.fadeAllViews(false);
  }
  
  @Override
  public void onLoadResource(WebView view, String url) {
	super.onLoadResource(view, url);
  }
  
  @Override
  public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
	// TODO(PHB): The following requires API 21. Add TargetUrl etc. to call it when supported.
	// super.onReceivedClientCertRequest(view, request);
  }
  
  @Override
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::onReceivedError", "Error code: " +
              errorCode + ", description: " + description + ", url: " + failingUrl);
    }
    super.onReceivedError(view, errorCode, description, failingUrl);
  }
  
  @Override
  public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::onReceivedHttpAuthRequest", "Host: " + host + ", realm: " + realm);
    }
	super.onReceivedHttpAuthRequest(view, handler, host, realm);
  }
  
  @Override
  public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::onReceivedLoginRequest", "Realm: " +
              realm + ", account: " + account + ", args: " + args);
    }
	// TODO(PHB): The following requires API 21. Add TargetUrl etc. to call it when supported.
	// super.onReceivedLoginRequest(view, realm, account, args);
  }
  
  @Override
  public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::onReceivedSslError", "Error: " + error.toString());
    }
	super.onReceivedSslError(view, handler, error);
  }
  
  @Override
  public void onUnhandledInputEvent(WebView view, InputEvent event) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::onUnhandledInputEvent", "Event: " + event.toString());
    }
	// TODO(PHB): The following requires API 21. Add TargetUrl etc. to call it when supported.
	// super.onUnhandledInputEvent(view, event);
  }
  
  @Override
  public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::shouldOverrideKeyEvent", "Event: " + event.toString());
    }
	return super.shouldOverrideKeyEvent(view, event);
  }
  
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
	if (!JactActionBarActivity.IS_PRODUCTION) {
      Log.e("JWebViewClient::shouldOverrideUrlLoading", "Url: " + url);
    }
	current_webview_ = view;
	return parent_.doSomething(url);
  }
  
  public boolean CallSuperLoadUrl(String url) {
	if (current_webview_ == null) return false;
	return super.shouldOverrideUrlLoading(current_webview_, url);
  }

}
