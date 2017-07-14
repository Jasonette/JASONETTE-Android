package com.jasonette.seed.Component;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.jasonette.seed.Core.JasonViewActivity;

import org.json.JSONObject;


public class JasonHtmlComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                WebView webview = new WebView(context);
                webview.getSettings().setDefaultTextEncodingName("utf-8");

                return webview;
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {
            JasonComponent.build(view, component, parent, context);

            try {
                String text = component.getString("text");
                String html = text;
                CookieManager.getInstance().setAcceptCookie(true);
                ((WebView) view).loadDataWithBaseURL("http://localhost/", html, "text/html", "utf-8", null);
                ((WebView) view).setWebChromeClient(new WebChromeClient());
                ((WebView) view).setVerticalScrollBarEnabled(false);
                ((WebView) view).setHorizontalScrollBarEnabled(false);
                WebSettings settings = ((WebView) view).getSettings();
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setMediaPlaybackRequiresUserGesture(false);

                settings.setAppCachePath( context.getCacheDir().getAbsolutePath() );
                settings.setAllowFileAccess( true );
                settings.setAppCacheEnabled( true );
                settings.setCacheMode( WebSettings.LOAD_DEFAULT );


                // not interactive by default;
                Boolean responds_to_webview = false;
                if(component.has("action")){
                    if(component.getJSONObject("action").has("type")){
                        String action_type = component.getJSONObject("action").getString("type");
                        if(action_type.equalsIgnoreCase("$default")){
                            responds_to_webview = true;
                        }
                    }
                }
                if(responds_to_webview){
                    // webview receives click
                    ((WebView) view).setOnTouchListener(null);
                    // Don't add native listener to this component
                } else {
                    // webview shouldn't receive click

                    ((WebView)view).setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            JSONObject component = (JSONObject)v.getTag();
                            try {
                                // 1. if the action type $default is specified, do what's default for webview
                                if (component.has("action")) {
                                    JSONObject action = component.getJSONObject("action");
                                    if (action.has("type")) {
                                        if (action.getString("type").equalsIgnoreCase("$default")) {
                                            return false;
                                        }
                                    }
                                }

                                // But only trigger on UP motion
                                if (event.getAction() == MotionEvent.ACTION_UP) {

                                    if(component.has("action")){
                                        // if the current component contains an action, run that one
                                        JSONObject action = component.getJSONObject("action");
                                        ((JasonViewActivity) context).call(action.toString(), new JSONObject().toString(), "{}", v.getContext());
                                    } else {
                                        // otherwise, bubble up the event to the closest parent view with an 'action' attribute
                                        View cursor = v;
                                        while (cursor.getParent() != null) {
                                            JSONObject item = (JSONObject) (((View) cursor.getParent()).getTag());
                                            if (item != null && (item.has("action") || item.has("href"))) {
                                                ((View) cursor.getParent()).performClick();
                                                break;
                                            } else {
                                                cursor = (View) cursor.getParent();
                                            }
                                        }
                                    }
                                }
                                return true;
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                            return true;
                        }
                    });
                }

                view.requestLayout();
                return view;
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
        return new View(context);
    }
}
