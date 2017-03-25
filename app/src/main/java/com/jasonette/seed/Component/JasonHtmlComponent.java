package com.jasonette.seed.Component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import org.json.JSONObject;


public class JasonHtmlComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                WebView webview = new WebView(context);
                webview.getSettings().setDefaultTextEncodingName("utf-8");

                return webview;
            } catch (Exception e) {
                Log.d("Error", e.toString());
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
                JasonComponent.addListener(view, context);
                view.requestLayout();
                return view;
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }
        return new View(context);
    }
}
