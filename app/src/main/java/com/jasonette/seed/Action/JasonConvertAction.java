package com.jasonette.seed.Action;

import android.content.Context;
import android.webkit.JavascriptInterface;

import com.jasonette.seed.Core.JSEngineHelper;
import com.jasonette.seed.Helper.JasonHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class JasonConvertAction {
    private static final String CSV_HTML_ASSET = "csv.html";
    private static final String CONVERT_CSV_FUNCTION_NAME = "csv.run";
    private static final String RSS_HTML_ASSET = "rss.html";
    private static final String CONVERT_RSS_FUNCTION_NAME = "rss.run";
    private static final String RSS_RESULT_GLOBAL_OBJECT = "rssResult";

    private JSONObject action;
    private Context context;
    private JSONObject event_cache;

    private JSEngineHelper mCSVJSEngine;
    private JSEngineHelper mRSSJSEngine;

    public void csv(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            this.action = action;
            this.context = context;
            event_cache = event;

            final JSONObject options = action.getJSONObject("options");

            if(options.has("data")) {
                String csv_data = options.getString("data");
                if (!csv_data.isEmpty()) {
                    String script = String.format("%s(\"%s\");", CONVERT_CSV_FUNCTION_NAME, csv_data.toString());

                    String html = JasonHelper.read_file(CSV_HTML_ASSET, context);
                    mCSVJSEngine = new JSEngineHelper(context, html, JSEngineHelper.ASSETS_URL_PREFIX+CSV_HTML_ASSET);
                    Timber.d("eval script: %s", script);
                    mCSVJSEngine.evaluate(script, new JSEngineHelper.WebViewResultListener() {
                        @Override
                        public void onResult(Object json) {
                            if (json != null) {
                                JasonHelper.next("success", action, json.toString(), event, context);
                            } else {
                                handleError(new Exception("null returned by JS"));
                            }
                        }
                    });
                }
            } else {
                String result = "[]";
                JasonHelper.next("success", action, result, event, context);
            }

        } catch (Exception e){
            handleError(e);
        }
    }

    public void rss(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            this.action = action;
            this.context = context;
            event_cache = event;

            final JSONObject options = action.getJSONObject("options");

            if(options.has("data")) {
                String rss_data = options.getString("data");
                if (!rss_data.isEmpty()) {
                    String script = String.format("%s('%s')", CONVERT_RSS_FUNCTION_NAME, StringEscapeUtils.escapeJavaScript(rss_data.toString()));

                    String html = JasonHelper.read_file(RSS_HTML_ASSET, context);
                    mRSSJSEngine = new JSEngineHelper(context, html,
                            JSEngineHelper.ASSETS_URL_PREFIX+RSS_HTML_ASSET,
                            new RssResult(), RSS_RESULT_GLOBAL_OBJECT);
                    mRSSJSEngine.evaluate(script, new JSEngineHelper.WebViewResultListener() {
                        @Override
                        public void onResult(Object json) {
                            // don't care, result comes via RssResult.callback()
                        }
                    });
                }
            } else {
                String result = "[]";
                JasonHelper.next("success", action, result, event, context);
            }

        } catch (Exception e){
            handleError(e);
        }
    }


    class RssResult {
        @JavascriptInterface
        public void callback(String items) {
            try {
                JSONArray result = new JSONArray(items);
                JasonHelper.next("success", action, result, event_cache, context);
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    /**
     * Handles an exception by passing the error to JasonHelper.next if possible, otherwise log
     * the output
     * @param exc  Exception
     */
    private void handleError(Exception exc){
        Timber.w(exc, "handling error from running JS");
        try {
            JSONObject error = new JSONObject();
            error.put("data", exc.toString());
            JasonHelper.next("error", action, error, event_cache, context);
        } catch (Exception e){
            Timber.e(e);
        }
    }
}
