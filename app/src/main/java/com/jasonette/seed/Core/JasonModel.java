package com.jasonette.seed.Core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public JSONObject refs;

    JasonViewActivity view;

    // Variables
    public JSONObject var;      // $get
    public JSONObject cache;    // $cache
    public JSONObject params;   // $params
    public JSONObject session;

    public OkHttpClient client;

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



    public void fetch() {
        if(url.startsWith("file://")) {
            fetch_local();
        } else {
            fetch_http();
        }
    }

    private void fetch_local(){
        try {
            jason = JasonHelper.read_json(url, this.view);
            if(jason.has("$jason")){
                view.build();
            } else {
                Log.d("Error", "Invalid jason");
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    private void fetch_http(){
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


            client = new OkHttpClient();
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
                    String res = response.body().string();
                    refs = new JSONObject();
                    resolve_and_build(res);
                }
            });
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }



    private void include(String res){
        String regex =  "\"(@)\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\"";
        Pattern require_pattern = Pattern.compile(regex);
        Matcher matcher = require_pattern.matcher(res);

        ArrayList<String> urls = new ArrayList<String>();

        while (matcher.find()) {
            //System.out.println("Path: " + matcher.group(3));
            // Fetch URL content and cache
            String matched = matcher.group(5);
            if(!matched.contains("$document")){
                urls.add(matcher.group(5));
            }
        }

        if(urls.size() > 0) {
            CountDownLatch latch = new CountDownLatch(urls.size());
            ExecutorService taskExecutor = Executors.newFixedThreadPool(urls.size());
            for (int i = 0; i < urls.size(); i++) {
                taskExecutor.submit(new JasonRequire(urls.get(i), latch, refs, client, view));
            }
            try {
                latch.await();
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }

        resolve_reference();
    }

    private void resolve_and_build(String res){
        try {

            jason = new JSONObject(res);

            // "include" handling
            // 1. check if it contains "+": "..."
            // 2. if it does, need to resolve it first.
            // 3. if it doesn't, just build the view immediately
            String regex = "\"(@)\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\"";
            Pattern require_pattern = Pattern.compile(regex);
            Matcher matcher = require_pattern.matcher(res);
            if (matcher.find()) {
                // if requires resolution, require first.
                include(res);
            } else {
                // otherwise just render
                if (jason.has("$jason")) {
                    view.build();
                } else {

                }
            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }

    private void resolve_reference(){
        // convert "+": "$document.blah.blah"
        // to "{{#include $root.$document.blah.blah}}": {}
        String str_jason = jason.toString();

        try {

            Log.d("str_jason = ", str_jason);

            String local_pattern_str = "\"@\"[ ]*:[ ]*\"[ ]*(\\$document[^\"]*)\"";
            Pattern local_pattern = Pattern.compile(local_pattern_str);
            Matcher local_matcher = local_pattern.matcher(str_jason);
            str_jason = local_matcher.replaceAll("\"{{#include \\$root.$1}}\": {}");

            String remote_pattern_with_path_str = "\"(@)\"[ ]*:[ ]*\"(([^\"@]+)(@))([^\"]+)\"";
            Pattern remote_pattern_with_path = Pattern.compile(remote_pattern_with_path_str);
            Matcher remote_with_path_matcher = remote_pattern_with_path.matcher(str_jason);
            str_jason = remote_with_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$5\\\\\"].$3}}\": {}");

            String remote_pattern_without_path_str = "\"(@)\"[ ]*:[ ]*\"([^\"]+)\"";
            Pattern remote_pattern_without_path = Pattern.compile(remote_pattern_without_path_str);
            Matcher remote_without_path_matcher = remote_pattern_without_path.matcher(str_jason);
            str_jason = remote_without_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$2\\\\\"]}}\": {}");

            JSONObject to_resolve = new JSONObject(str_jason);

            refs.put("$document", jason);
            /*
            Iterator<?> keys = refs.keys();
            while(keys.hasNext()) {
                String key = (String)keys.next();
                if(!key.equalsIgnoreCase("$document")) {
                    try {
                        refs.put(key, refs.get(key));
                    } catch (Exception e) {
                        Log.d("Error", e.toString());
                    }
                }
            }
            */

            // parse
            JasonParser.getInstance(this.view).setParserListener(new JasonParser.JasonParserListener() {
                @Override
                public void onFinished(JSONObject resolved_jason) {
                    try {
                        Log.d("j", resolved_jason.toString(2));
                        resolve_and_build(resolved_jason.toString());
                    } catch (Exception e) {
                        Log.d("Error", e.toString());
                    }
                }
            });
            Log.d("refs", refs.toString(2));
            Log.d("to_resolve", to_resolve.toString(2));
            JasonParser.getInstance(this.view).parse("json", refs, to_resolve, this.view);
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
