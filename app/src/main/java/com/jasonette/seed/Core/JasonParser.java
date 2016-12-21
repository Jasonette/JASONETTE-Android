package com.jasonette.seed.Core;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;
import com.squareup.duktape.Duktape;

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

    interface  Fun {
       String json(String template, String data, Boolean json);
       String html(String template, String data, Boolean json);
       String xml(String template, String data, Boolean json);
    }

    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){

        try{
            new Thread(new Runnable(){
                @Override public void run() {
                    try {
                        Duktape duktape = Duktape.create();
                        String js = JasonHelper.read_file("parser", context);
                        String jsonResult = "{}";
                        try {
                            duktape.evaluate(js);
                            Fun fun = duktape.get("parser", Fun.class);

                            if (data_type.equalsIgnoreCase("json")) {
                                String templateJson = template.toString();
                                String dataJson = data.toString();
                                jsonResult = fun.json(templateJson, dataJson, true);
                            } else {
                                String raw_data = data.getString("$jason");

                                // fix for emoji bug
                                //raw_data = escape_unicode(raw_data);
                                String templateJson = template.toString();
                                jsonResult = fun.html(templateJson, raw_data, true);
                            }
                        } catch (Exception e) {
                            Log.d("Error", e.toString());
                        } finally {
                            duktape.close();
                        }
                        JSONObject res = new JSONObject(jsonResult);
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



