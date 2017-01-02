package com.jasonette.seed.Component;

import android.content.Context;
import android.util.Log;
import android.view.View;
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
                String html = "<style>" + component.getString("css") + "</style>" + text;

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                view.setLayoutParams(params);
                
                ((WebView) view).loadData(html, "text/html; charset=utf-8", null);

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
