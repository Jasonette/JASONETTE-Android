package com.jasonette.seed.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JasonRequire implements Runnable{
    final String URL;
    final Dictionary URLObject;
    final CountDownLatch latch;
    final Context context;
    final OkHttpClient client;
    final String method;
    final String keyURL;

    JSONObject private_refs;

    public JasonRequire(String url, CountDownLatch latch, JSONObject refs, OkHttpClient client, Context context) {
        this.URL = url.replace("\\", "");
        this.keyURL = this.URL;
        this.latch = latch;
        this.private_refs = refs;
        this.context = context;
        this.client = client;
        this.URLObject = null;
        this.method = "get";
    }

    /**
     * This constructor version takes an object instead of a string for the url
     * this url is typically from the mixin with [post] params used in JasonModel.java
     * @param url
     * @param latch
     * @param refs
     * @param client
     * @param context
     */
    public JasonRequire(Dictionary url, CountDownLatch latch, JSONObject refs, OkHttpClient client, Context context) {
        this.URLObject = url;
        this.URL = ((String) url.get("parsed")).replace("\\", "");
        this.method = (String) url.get("method");
        this.keyURL = ((String) url.get("original")).replace("\\", "");;
        this.latch = latch;
        this.private_refs = refs;
        this.context = context;
        this.client = client;
    }

    public void run() {
        if(this.URL.contains("file://")) {
            local();
        } else {
            remote();
        }
    }
    private void local(){
        Log.d("Debug", "Local file:// detected");
        try {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    Object json = JasonHelper.read_json(URL, context);
                    try {
                        private_refs.put(URL, json);
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                    latch.countDown();
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            latch.countDown();
        }
    }
    private void remote(){
        Log.d("Debug", "Remote file detected");
        Request request;
        Request.Builder builder = new Request.Builder();

        // Session Handling
        try {
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            JSONObject session = null;
            URI uri_for_session = new URI(this.URL);
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

            Log.d("Mixin", "Fetching: " + url_with_session);

            if(this.method.equals("post")) {
                Log.d("Mixin", "POST");
                request = builder.url(url_with_session).post(
                        RequestBody.create("{}",
                                MediaType.parse("application/json")
                        )
                ).build();

            } else {
                Log.d("Mixin", "GET");
                request = builder
                        .url(url_with_session)
                        .build();
            }

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
                        Log.d("Body", res);
                        // store the res under
                        if(res.trim().startsWith("[")) {
                            // array
                            private_refs.put(keyURL, new JSONArray(res));
                        } else if(res.trim().startsWith("{")){
                            // object
                            private_refs.put(keyURL, new JSONObject(res));
                        } else {
                            // string
                            private_refs.put(keyURL, res);
                        }
                        latch.countDown();
                    } catch (JSONException e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            });

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
