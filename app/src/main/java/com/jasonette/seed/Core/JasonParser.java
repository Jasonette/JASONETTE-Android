package com.jasonette.seed.Core;

import android.content.Context;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class JasonParser {


    private static final String PARSER_HTML_ASSET = "parser.html";
    private static final String JSON_DATA_TYPE_VALUE = "json";
    private static final String PARSER_JSON_FUNCTION_NAME = "parser.json";
    private static final String PARSER_HTML_FUNCTION_NAME = "parser.html";

    private static JasonParser instance = null;
    private static JSEngineHelper mJSEngine;

    private JasonParser(){
        this.listener = null;
    }

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
            try {
                String parserHtml = JasonHelper.read_file(PARSER_HTML_ASSET, context);
                mJSEngine = new JSEngineHelper(context, parserHtml, JSEngineHelper.ASSETS_URL_PREFIX+PARSER_HTML_ASSET);

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
        mJSEngine.evaluate(script, new JSEngineHelper.WebViewResultListener() {
            @Override
            public void onResult(Object json) {
                listener.onFinished((JSONObject) json);
            }
        });
    }
}

