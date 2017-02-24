package com.jasonette.seed.Action;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JasonNetworkAction {
    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try{
            final JSONObject options = action.getJSONObject("options");
            if(options.has("url")){
                String url = options.getString("url");

                // method
                String method = "GET";
                if(options.has("method")) {
                    method = options.getString("method").toUpperCase();
                }

                // Attach session if it exists
                SharedPreferences pref = context.getSharedPreferences("session", 0);
                JSONObject session = null;

                URI uri_for_session = new URI(url.toLowerCase());
                String session_domain = uri_for_session.getHost();

                if(pref.contains(session_domain)){
                    String str = pref.getString(session_domain, null);
                    session = new JSONObject(str);
                }


                Request request;
                Request.Builder builder = new Request.Builder();
                // Attach Header from Session
                if(session != null && session.has("header")) {
                    Iterator<?> keys = session.getJSONObject("header").keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        String val = session.getJSONObject("header").getString(key);
                        builder.addHeader(key, val);
                    }
                }


                if(method.equalsIgnoreCase("get")) {
                    Uri.Builder b = Uri.parse(url).buildUpon();

                    // Attach Params from Session
                    if(session != null && session.has("body")) {
                        Iterator<?> keys = session.getJSONObject("body").keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            String val = session.getJSONObject("body").getString(key);

                            b.appendQueryParameter(key, val);

                        }
                    }

                    // params
                    if (options.has("data")) {
                        JSONObject d = options.getJSONObject("data");
                        Iterator<String> keysIterator = d.keys();
                        try {
                            while (keysIterator.hasNext()) {
                                String key = (String) keysIterator.next();
                                String val = d.getString(key);
                                b.appendQueryParameter(key, val);
                            }
                        } catch (Exception e) {

                        }
                    }

                    Uri uri = b.build();
                    url = uri.toString();

                    request = builder
                            .url(url)
                            .build();
                } else {

                    // Params
                    FormBody.Builder bodyBuilder = new FormBody.Builder();
                    if (options.has("data")) {
                        JSONObject d = options.getJSONObject("data");
                        Iterator<String> keysIterator = d.keys();
                        try {
                            while (keysIterator.hasNext()) {
                                String key = (String) keysIterator.next();
                                String val = d.getString(key);
                                bodyBuilder.add(key, val);
                            }
                        } catch (Exception e) {

                        }
                    }
                    // Attach Params from Session
                    if(session != null && session.has("body")) {
                        Iterator<?> keys = session.getJSONObject("body").keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            String val = session.getJSONObject("body").getString(key);
                            bodyBuilder.add(key, val);
                        }
                    }
                    RequestBody requestBody = bodyBuilder.build();

                    request = builder
                            .method(method, requestBody)
                            .url(url)
                            .build();
                }


                OkHttpClient client = new OkHttpClient();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        try {
                            if (action.has("error")) {

                                JSONObject error = new JSONObject();
                                error.put("data", e.toString());

                                JasonHelper.next("error", action, error, event, context);
                            }
                        } catch (Exception err){
                            Log.d("Error", err.toString());
                        }
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            try {
                                if (action.has("error")) {
                                    JSONObject error = new JSONObject();
                                    error.put("data", response.toString());
                                    JasonHelper.next("error", action, error, event, context);
                                }
                            } catch (Exception err){
                                Log.d("Error", err.toString());
                            }
                        } else {
                            try {
                                String jsonData = response.body().string();
                                JasonHelper.next("success", action, jsonData, event, context);

                            } catch (Exception e) {
                                Log.d("Error", e.toString());
                            }
                        }
                    }
                });

            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
}
