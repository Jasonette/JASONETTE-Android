package com.jasonette.seed.Core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JasonModel{
    public String url;
    public JSONObject jason;
    public JSONObject rendered;
    public JSONObject state;

    JasonViewActivity view;

    // Variables
    public JSONObject var;      // $get
    public JSONObject cache;    // $cache
    public JSONObject params;   // $params
    public JSONObject session;

    public JasonModel(String url, Intent intent, JasonViewActivity view){
        this.url = url;
        this.view = view;

        // $params
        this.params = new JSONObject();
        if(intent != null && intent.hasExtra("params")){
            try {
                this.params = new JSONObject(intent.getStringExtra("params"));
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }

        // $get
        this.var = new JSONObject();

        // $cache
        this.cache = new JSONObject();
        SharedPreferences cache_pref = view.getSharedPreferences("cache", 0);
        if(cache_pref.contains(url)){
            String str = cache_pref.getString(url, null);
            try {
                this.cache = new JSONObject(str);
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }

        // session
        SharedPreferences session_pref = view.getSharedPreferences("session", 0);
        this.session = new JSONObject();
        try {
            URI uri_for_session = new URI(url.toLowerCase());
            String session_domain = uri_for_session.getHost();
            if (session_pref.contains(session_domain)) {
                String str = session_pref.getString(session_domain, null);
                this.session = new JSONObject(str);
            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        };
    }



    public void fetch(){


        try{
            Request request;
            Request.Builder builder = new Request.Builder();

            // SESSION HANDLING

            // Attach Header from Session
            if(session != null && session.has("header")) {
                Iterator<?> keys = session.getJSONObject("header").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("header").getString(key);
                    builder.addHeader(key, val);
                }
            }
            // Attach Params from Session
            if (session != null && session.has("body")) {
                Iterator<?> keys = session.getJSONObject("body").keys();
                Uri.Builder b = Uri.parse(url).buildUpon();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("body").getString(key);

                    b.appendQueryParameter(key, val);

                }
                Uri uri = b.build();
                url = uri.toString();
            }
            request = builder
                    .url(url)
                    .build();


            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    try {
                        String res = response.body().string();
                        jason = new JSONObject(res);
                        if(jason.has("$jason")){
                            view.build();
                        } else {

                        }
                    } catch (JSONException e) {
                        Log.d("Error", e.toString());
                    }
                }
            });
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }

    public void set(String name, JSONObject data){
        if(name.equalsIgnoreCase("jason")) {
            this.jason = data;
        } else if(name.equalsIgnoreCase("state")) {
            try {
                // Construct variable state (var => $get, cache => $cache, params => $params, etc)
                // by default, take the inline data
                if (jason.getJSONObject("$jason").has("head") && jason.getJSONObject("$jason").getJSONObject("head").has("data")) {
                    state = jason.getJSONObject("$jason").getJSONObject("head").getJSONObject("data");
                } else {
                    state = new JSONObject();
                }

                if (data instanceof JSONObject) {
                    Iterator<?> keys = data.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        Object val = data.get(key);
                        state.put(key, val);
                    }
                }

                // merge with passed in data
                state.put("$get", var);
                state.put("$cache", cache);
                state.put("$params", params);
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        } else {

        }
    }

}
