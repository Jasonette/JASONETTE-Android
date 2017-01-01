package com.jasonette.seed.Core;

import android.content.Context;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

public class JasonParser {
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


    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){

        try{
            new Thread(new Runnable(){
                @Override public void run() {
                    try {
                        String js = JasonHelper.read_file("parser", context);

                        V8 runtime = V8.createV8Runtime();
                        runtime.executeVoidScript(js);
                        V8Object parser = runtime.getObject("parser");

                        String templateJson = template.toString();
                        String dataJson = data.toString();
                        String val = "{}";

                        if(data_type.equalsIgnoreCase("json")) {
                            V8Array parameters = new V8Array(runtime).push(templateJson);
                            parameters.push(dataJson);
                            parameters.push(true);
                            val = parser.executeStringFunction("json", parameters);
                            parameters.release();
                        } else {
                            String raw_data = data.getString("$jason");
                            V8Array parameters = new V8Array(runtime).push(templateJson);
                            parameters.push(raw_data);
                            parameters.push(true);
                            //raw_data = escape_unicode(raw_data);
                            val = parser.executeStringFunction("html", parameters);
                            parameters.release();
                        }
                        parser.release();
                        runtime.release();

                        res = new JSONObject(val);
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



