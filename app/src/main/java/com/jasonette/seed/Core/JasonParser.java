package com.jasonette.seed.Core;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class JasonParser {
    private static final String PARSER_HTML_ASSET = "parser.html";
    private static final String ASSETS_URL_PREFIX = "file:///android_asset/";
    private static final String TEXT_MIME_TYPE = "text/html";
    private static final String HISTORY_URL = "";
    private static final String JSON_DATA_TYPE_VALUE = "json";
    private static final String PARSER_JSON_FUNCTION_NAME = "parser.json";
    private static final String PARSER_HTML_FUNCTION_NAME = "parser.html";
    private static final long WEBVIEW_READY_TIMEOUT_MS = 1000;

    private static JasonParser instance = null;
    private Handler mHandler;
    private CountDownLatch mWebviewReadyLatch = new CountDownLatch(1);

    private JasonParser(){
        this.listener = null;
    }

    static WebView mWebView;

    public interface JasonParserListener {
        void onFinished(JSONObject json);
    }

    private JasonParserListener listener;
    public void setParserListener(JasonParserListener listener){
        this.listener = listener;
    }



    public static JasonParser getInstance(Context context){
        if(instance == null)
        {
            instance = new JasonParser();
            instance.mHandler = new Handler();
            try {
                String html = JasonHelper.read_file(PARSER_HTML_ASSET, context);

                mWebView = new WebView(context);
                WebSettings webSettings = mWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);

                // If we want to redirect console output...
                // ref: https://stackoverflow.com/a/40485201/85472
//                mWebView.setWebChromeClient(new WebChromeClient() {
//                    @Override
//                    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
//                        Timber.e(consoleMessage.message() + " -- From line "
//                                + consoleMessage.lineNumber() + " of "
//                                + consoleMessage.sourceId());
//                        return super.onConsoleMessage(consoleMessage);
//                    }
//                });

                mWebView.setWebViewClient(new WebViewClient(){
                    public void onPageFinished(WebView view, String url){
                        Timber.d("Parser webview READY");
                        instance.mWebviewReadyLatch.countDown();
                    }
                });

                mWebView.loadDataWithBaseURL(ASSETS_URL_PREFIX, html, TEXT_MIME_TYPE, "utf-8", HISTORY_URL);
            } catch (Exception e){
                Timber.w(e);
            }
        }
        return instance;
    }

    /**
     * Run call methods in parser.js in a webview to parse the jason template and data
     *
     * @param data_type
     * @param data
     * @param template
     * @param context
     */
    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){

        String templateJson = template.toString();
        String dataJson = data.toString();

        final String script;
        if(data_type.equalsIgnoreCase(JSON_DATA_TYPE_VALUE)) {
            script = String.format("%s(%s, %s, true);", PARSER_JSON_FUNCTION_NAME, templateJson, dataJson);
        } else {
            String raw_data = null;
            try {
                raw_data = data.getString(JasonModel.JASON_PROP_NAME);
            } catch (JSONException e) {
                Timber.e(e);
            }
            script = String.format("%s(%s, %s, true);",PARSER_HTML_FUNCTION_NAME, templateJson, raw_data);
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mWebviewReadyLatch.await(WEBVIEW_READY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Timber.w(e);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWebView.evaluateJavascript(script, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            try {
                                if (value != null) {
                                    // The string returned by the webview is a JSONObject or JSONArray or value
                                    // in the case of parser.js we know it will only be a Object.
                                    // BUT the string returned from a webview is also always wrapped in double quotes
                                    // AND has double quotes escaped, eg. "{ \"foo]" : \"bar\"}"
                                    // so we need the 2 replace calls to strip out both
                                    // ref: https://stackoverflow.com/a/14884111/85472
                                    //      https://stackoverflow.com/a/2608682/85472
                                    value = value.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                                }
                                listener.onFinished((value != null && !"null".equals(value)) ? new JSONObject(value) : null);
                            } catch (JSONException e) {
                                Timber.e(e);
                            }
                        }
                    });
                } else {
                    /**
                     * For pre-KitKat+ you should use loadUrl("javascript:<JS Code Here>");
                     * To then call back to Java you would need to use addJavascriptInterface()
                     * and have your JS call the interface
                     **/
//                mWebView.loadUrl("javascript:"+javascript);
                    throw new UnsupportedOperationException("Support for Pre-KitKat pending");
                }
            }
        });
    }
}

