package com.jasonette.seed.Core;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSException;
import org.liquidplayer.webkit.javascriptcore.JSFunction;
import org.liquidplayer.webkit.javascriptcore.JSON;
import org.liquidplayer.webkit.javascriptcore.JSObject;
import org.liquidplayer.webkit.javascriptcore.JSValue;

public class JasonParser implements JSContext.IJSExceptionHandler {
    static JSONObject res;
    private static Context context;

    private static JasonParser instance = null;

    private JasonParser(){
        this.listener = null;
    }

    public interface JasonParserListener {
        public void onFinished(JSONObject json);
    }
    private JasonParserListener listener;
    public void setParserListener(JasonParserListener listener){
        this.listener = listener;
    }



    public static JasonParser getInstance(){
        if(instance == null)
        {
            instance = new JasonParser();
        }
        return instance;
    }


    @Override
    public void handle(JSException exception) {
        Log.d("Error", exception.toString());
    }

    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){

        try{
            new Thread(new Runnable(){
                @Override public void run() {
                    try {
                        String js = JasonHelper.read_file("parser", context);
                        JSContext jscontext = new JSContext();
                        jscontext.setExceptionHandler(JasonParser.getInstance());
                        jscontext.evaluateScript(js);
                        JSObject parser = jscontext.property("parser").toObject();
                        JSFunction json = parser.property(data_type).toFunction();
                        JSValue val;
                        if(data_type.equalsIgnoreCase("json")){
                            val = json.call(null, JSON.parse(jscontext, template.toString()), JSON.parse(jscontext, data.toString()));
                        } else {
                            String raw_data = data.getString("$jason");

                            // fix for emoji bug
                            raw_data = escape_unicode(raw_data);
                            val = json.call(null, JSON.parse(jscontext, template.toString()), raw_data);
                        }
                        res = new JSONObject(val.toJSON());
                        listener.onFinished(res);
                    } catch (Exception e){
                        Log.d("Error", e.toString());
                    }
               }
            }).start();
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }

    public static String escape_unicode(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (i < (input.length() - 1)) {
                if (Character.isSurrogatePair(input.charAt(i), input.charAt(i + 1))) {
                    i += 1;
                    continue;
                }
            }
            sb.append(input.charAt(i));
        }
        return sb.toString();
    }
}



