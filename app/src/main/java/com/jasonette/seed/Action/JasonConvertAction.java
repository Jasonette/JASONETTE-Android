package com.jasonette.seed.Action;

import android.content.Context;

import com.jasonette.seed.Core.JSEngineHelper;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

import timber.log.Timber;

//import com.eclipsesource.v8.JavaVoidCallback;
//import com.eclipsesource.v8.V8;
//import com.eclipsesource.v8.V8Array;
//import com.eclipsesource.v8.V8Object;

public class JasonConvertAction {
    private static final String CSV_HTML_ASSET = "csv.html";
    private static final String CONVERT_CSV_FUNCTION_NAME = "csv.run";


    private JSONObject action;
    private Context context;
    private JSONObject event_cache;
    private JSEngineHelper mJSEngine;

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
                    mJSEngine = new JSEngineHelper(context, html);
                    mJSEngine.evaluate(script, new JSEngineHelper.WebViewResultListener() {
                        @Override
                        public void onResult(Object json) {
                            if (json != null) {
                                JasonHelper.next("success", action, json.toString(), event, context);
                            } else {
                                Timber.e("null returned by JS");
                                JasonHelper.next("error", action, "{}", event, context);
                            }
                        }
                    });
                }
            } else {
                String result = "[]";
                JasonHelper.next("success", action, result, event, context);
            }

        } catch (Exception e){
            handle_exception(e);
        }
    }

    public void rss(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
//        this.action = action;
//        this.context = context;
//        event_cache = event;
//        try{
//            final JSONObject options = action.getJSONObject("options");
//            String rss_data = options.getString("data");
//            String js = JasonHelper.read_file("rss", context);
//            String timers = JasonHelper.read_file("timers", context);
//            V8 runtime = V8.createV8Runtime();
//            runtime.executeVoidScript(js);
//
//            // Shim to support javascript timer functions in V8
//            runtime.registerJavaMethod(new Sleep(), "sleep");
//            runtime.executeVoidScript(timers);
//            runtime.executeVoidScript("var timerLoop = makeWindowTimer(this, sleep);");
//
//            V8Object rss = runtime.getObject("rss");
//            V8Array parameters = new V8Array(runtime).push(rss_data.toString());
//            // Register a callback to receive the RSS JSON data
//            runtime.registerJavaMethod(new RSSCallback(), "callback");
//            rss.executeObjectFunction("run", parameters);
//
//            // Now we need to kick off the timer loop to get the parsing started
//            runtime.executeVoidScript("timerLoop()");
//
//            parameters.release();
//            rss.release();
//            runtime.release();
//        } catch (Exception e){
//            handle_exception(e);
//        }
    }

    /**
     * Converts a JSON object to a string using the javascript method `JSON.stringify`
     * @param runtime     a V8 runtime
     * @param jsonObject  a JSON object (V8Object)
     * @return            a String representation of the JSON object
     */
//    private String stringify(final V8 runtime, V8Object jsonObject){
//        V8Array parameters = new V8Array(runtime).push(jsonObject);
//        V8Object json = runtime.getObject("JSON");
//        String result = json.executeStringFunction("stringify", parameters);
//        parameters.release();
//        jsonObject.release();
//        json.release();
//        return result;
//    }

    /**
     * Handles an exception by passing the error to JasonHelper.next if possible, otherwise log
     * the output
     * @param exc  Exception
     */
    private void handle_exception(Exception exc){
        try {
            JSONObject error = new JSONObject();
            error.put("data", exc.toString());
            JasonHelper.next("error", action, error, event_cache, context);
        } catch (Exception e){
            Timber.e(e);
        }
    }

    /**
     * Callback to handle the sleep function called from timer javascript code. Simply sleeps for
     * the number of milliseconds passed in the arguments.
     */
//    class Sleep implements JavaVoidCallback {
//        @Override
//        public void invoke(V8Object receiver, V8Array parameters) {
//            try {
//                Thread.sleep((long)parameters.get(0));
//            } catch (InterruptedException e) {
//                handle_exception(e);
//            }
//        }
//    }

    /**
     * Callback that gets run at the end of RSS parsing. The one parameter is the RSS data, as a
     * V8Array
     */
//    class RSSCallback implements JavaVoidCallback {
//        @Override
//        public void invoke(V8Object receiver, V8Array parameters) {
//            try {
//                V8Array rss_data = (V8Array) parameters.get(0);
//                String result = stringify(receiver.getRuntime(), rss_data);
//
//                JasonHelper.next("success", action, result, event_cache, context);
//            } catch (Exception e) {
//                handle_exception(e);
//            }
//        }
//    }
}
