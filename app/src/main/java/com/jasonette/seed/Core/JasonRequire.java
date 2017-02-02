package com.jasonette.seed.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class JasonRequire implements Runnable{
    final String URL;
    final CountDownLatch latch;
    final Context context;
    final OkHttpClient client;

    JSONObject private_refs;

    public JasonRequire(String url, CountDownLatch latch, JSONObject refs, OkHttpClient client, Context context) {
        this.URL = url;
        this.latch = latch;
        this.private_refs = refs;
        this.context = context;
        this.client = client;
    }
    public void run(){
        Request request;
        Request.Builder builder = new Request.Builder();

        // Session Handling
        try {
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            JSONObject session = null;
            URI uri_for_session = new URI(this.URL.toLowerCase());
            String session_domain = uri_for_session.getHost();
            if(pref.contains(session_domain)){
                String str = pref.getString(session_domain, null);
                session = new JSONObject(str);
            }

            // session.header
            if(session != null && session.has("header")) {
                Iterator<?> keys = session.getJSONObject("header").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("header").getString(key);
                    builder.addHeader(key, val);
                }
            }

            // session.body
            Uri.Builder b = Uri.parse(this.URL).buildUpon();
            // Attach Params from Session
            if(session != null && session.has("body")) {
                Iterator<?> keys = session.getJSONObject("body").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("body").getString(key);
                    b.appendQueryParameter(key, val);
                }
            }

            Uri uri = b.build();
            String url_with_session = uri.toString();
            request = builder
                    .url(url_with_session)
                    .build();


            // Actual call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    latch.countDown();
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        latch.countDown();
                        throw new IOException("Unexpected code " + response);
                    }
                    try {
                        String res = response.body().string();
                        // store the res under
                        if(res.trim().startsWith("[")) {
                            // array
                            private_refs.put(URL, new JSONArray(res));
                        } else if(res.trim().startsWith("{")){
                            // object
                            private_refs.put(URL, new JSONObject(res));
                        } else {
                            // string
                            private_refs.put(URL, res);
                        }
                        latch.countDown();
                    } catch (JSONException e) {
                        Log.d("Error", e.toString());
                    }
                }
            });

        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
}
