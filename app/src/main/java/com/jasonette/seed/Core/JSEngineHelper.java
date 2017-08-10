package com.jasonette.seed.Core;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jasonette.seed.Helper.JasonHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Provide easy to use way to run JS within a WebView in a preloaded html content context.
 * This allows repeatedly executing JS scripts with a set of preloaded JS scripts/libraries.
 */

public class JSEngineHelper {

    public  static final String ASSETS_URL_PREFIX = "file:///android_asset/";

    private static final String TEXT_MIME_TYPE = "text/html";
    private static final long WEBVIEW_READY_TIMEOUT_MS = 1000;

    private final Handler mMainLoopHandler;
    private WebView mWebView;
    private CountDownLatch mWebviewReadyLatch = new CountDownLatch(1);

    /**
     *
     */
    public interface WebViewResultListener {
        void onResult(Object json);
    }

    /**
     * Construct Webview to use as a JS Engine for evaluating JS.
     *
     * @param context
     * @param html HTMl content to use in webview
     * @param url Url representing the webview, useful to "name" the webview, eg. for Remote Chrome DevTools
     */
    public JSEngineHelper(Context context, String html, String url) {
        this(context, html, url,  null, null);
    }

    /**
     * Construct Webview to use as a JS Engine for evaluating JS.     *
     *
     * @param context
     * @param html
     * @param url   Url representing the webview, useful to "name" the webview, eg. for Remote Chrome DevTools
     * @param object Java object to add to the JS global scope. See WebView.addJavascriptInterface
     *               regarding need to add annotations to publicly visible methods
     * @param name   Name of the property for the Java object in the JS global scope.
     */
    public JSEngineHelper(Context context, String html, String url, Object object, String name) {
        mMainLoopHandler = new Handler(context.getMainLooper());
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
                Timber.d("JSEngine Webview READY");
                mWebviewReadyLatch.countDown();
            }
        });

        if (object != null && name != null) {
            mWebView.addJavascriptInterface(object, name);
        }

        mWebView.loadDataWithBaseURL(ASSETS_URL_PREFIX, html, TEXT_MIME_TYPE, "utf-8", url);
    }

    public void evaluate(final String script, final WebViewResultListener listener) {
        mMainLoopHandler.post(new Runnable() {
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
                            if (value != null) {
                                // The string returned by the webview is a JSONObject or JSONArray or value
                                // in the case of parser.js we know it will only be a Object.
                                // BUT the string returned from a webview is also always wrapped in double quotes
                                // AND has double quotes escaped, eg. "{ \"foo]" : \"bar\"}"
                                // so we need the 2 replace calls to strip out both
                                //
                                // WE can't just use StringEscapeUtils.unescapeJava because we need to keep "\n"
                                // as those maybe being used in strings coming from back in the result from the webview
                                //
                                // Finally we need to call replace("\\\"", "\"") Twice as the JS code that runs in the
                                // webview maybe returning strings that it itself has escaped.
                                //
                                // ref: https://stackoverflow.com/a/14884111/85472
                                //      https://stackoverflow.com/a/2608682/85472
                                value = value.replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\"", "\"");

                            }
                            listener.onResult((value != null && !"null".equals(value)) ? JasonHelper.objectify(value) : null);
                        }
                    });
                } else {
                    /**
                     * For pre-KitKat+ you should use loadUrl("javascript:<JS Code Here>");
                     * To then call back to Java you would need to use addJavascriptInterface()
                     * and have your JS call the interface
                     **/
//                  mWebView.loadUrl("javascript:"+javascript);
                    throw new UnsupportedOperationException("Support for Pre-KitKat pending");
                }
            }
        });
    }

}
