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
    private V8 juice;
    public void setParserListener(JasonParserListener listener){
        this.listener = listener;
    }



    public static JasonParser getInstance(Context context){
        if(instance == null)
        {
            instance = new JasonParser();
            try {
                String js = JasonHelper.read_file("parser", context);
                instance.juice = V8.createV8Runtime();
                instance.juice.executeVoidScript(js);
                instance.juice.getLocker().release();
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        }
        return instance;
    }


    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){

        try{
            new Thread(new Runnable(){
                @Override public void run() {
                    try {

                        // thread handling - acquire handle
                        juice.getLocker().acquire();
                        V8Object parser = juice.getObject("parser");

                        String templateJson = template.toString();
                        String dataJson = data.toString();
                        String val = "{}";

                        if(data_type.equalsIgnoreCase("json")) {
                            V8Array parameters = new V8Array(juice).push(templateJson);
                            parameters.push(dataJson);
                            parameters.push(true);
                            val = parser.executeStringFunction("json", parameters);
                            parameters.release();
                        } else {
                            String raw_data = data.getString("$jason");
                            V8Array parameters = new V8Array(juice).push(templateJson);
                            parameters.push(raw_data);
                            parameters.push(true);
                            val = parser.executeStringFunction("html", parameters);
                            parameters.release();
                        }
                        parser.release();

                        // thread handling - release handle
                        juice.getLocker().release();

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
}



