package com.jasonette.seed.Core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.hjson.JsonValue;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    public boolean offline;

    public JSONObject refs;

    JasonViewActivity view;

    // Variables
    public JSONObject var;      // $get
    public JSONObject cache;    // $cache
    public JSONObject params;   // $params
    public JSONObject session;
    public JSONObject action;   // latest executed action

    public OkHttpClient client;

    public JasonModel(String url, Intent intent, JasonViewActivity view){
        this.url = url;
        this.view = view;
        this.client = ((Launcher)view.getApplication()).getHttpClient(0);
        this.offline = false;

        // $params
        this.params = new JSONObject();
        if(intent != null && intent.hasExtra("params")){
            try {
                this.params = new JSONObject(intent.getStringExtra("params"));
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
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
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
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
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        };

        try {
            JSONObject v = new JSONObject();
            v.put("url", this.url);
            ((Launcher)(this.view.getApplicationContext())).setEnv("view", v);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        };
    }



    public void fetch() {
        if(url.startsWith("file://")) {
            fetch_local(url);
        } else {
            fetch_http(url);
        }
    }

    public void fetch_local(final String url){
        final JasonViewActivity context = this.view;
        try {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    jason = (JSONObject)JasonHelper.read_json(url, context);
                    refs = new JSONObject();
                    resolve_and_build(jason.toString());
                }
            };
            Thread t = new Thread(r);
            t.start();

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void fetch_http(String url){
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



            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if(!offline) fetch_local("file://error.json");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        if(!offline) fetch_local("file://error.json");
                    } else {
                        String res = response.body().string();
                        refs = new JSONObject();
                        resolve_and_build(res);
                    }
                }
            });
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    /**
     * Parses a string similar to https://www.example.com/api/data.json[post]
     * and returns a dictionary with original, parsed, and method
     * @param url
     * @return dictionary with original, parsed, and method url
     */
    private Dictionary get_method_for_url(String url) {
        Dictionary result = new Hashtable();
        result.put("original", url);
        result.put("parsed", url);
        result.put("method", "get");
        result.put("shouldDownload", true);

        String regex =  "\\[(POST|GET|PUT|HEAD|DELETE)\\]";
        Pattern require_pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = require_pattern.matcher(url);

        String matched = "";
        String parsed = "";

        while(matcher.find()) {
            Log.d("Debug", "Match Found");
            // group 0 contains [post]
            matched = matcher.group(0);
            parsed = url.substring(0, url.length() - matched.length());

            // group 1 contains only post
            result.put("method", matcher.group(1).toLowerCase());
            result.put("parsed", parsed);
        }

        // if the url contains brackets [] and its the same as the original
        // it means it maybe has wrong format
        // so its better to not download it or it could crash the app
        if(parsed.contentEquals(url) && url.contains("[]")) {
            Log.d("Warning", "Provided Url for Mixin have wrong format.");
            result.put("shouldDownload", false);
        }

        return result;
    }

    private void include(String res){
        Log.d("Debug", "Mixin Detected: " + res);
        String regex =  "\"([+@])\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\"";
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
            Dictionary parsedUrl = null;
            Boolean shouldDownload = false;
            for (int i = 0; i < urls.size(); i++) {
                parsedUrl = get_method_for_url(urls.get(i));
                shouldDownload = (Boolean) parsedUrl.get("shouldDownload");
                if(shouldDownload) {
                    taskExecutor.submit(new JasonRequire(parsedUrl, latch, refs, client, view));
                }
            }
            try {
                latch.await();
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }

        resolve_reference();
    }

    private void resolve_and_build(String res){
        try {

            String jsonString = JsonValue.readHjson(res).toString();
            jason = new JSONObject(jsonString);

            // "include" handling
            // 1. check if it contains "+": "..."
            // 2. if it does, need to resolve it first.
            // 3. if it doesn't, just build the view immediately

            // Exclude patterns that start with $ (will be handled by local resolve)
            String regex = "\"([+@])\"[ ]*:[ ]*\"(([^$\"@]+)(@))?([^$\"]+)\"";
            Pattern require_pattern = Pattern.compile(regex);
            Matcher matcher = require_pattern.matcher(res);
            if (matcher.find()) {
                // if requires resolution, require first.
                include(res);
            } else {
                // otherwise, resolve local once and then render (for $document)
                String local_regex = "\"([+@])\"[ ]*:[ ]*\"(([^\"@]+)(@))?([^\"]+)\"";
                Pattern local_require_pattern = Pattern.compile(local_regex);
                Matcher local_matcher = local_require_pattern.matcher(res);
                if (local_matcher.find()) {
                    resolve_local_reference();
                } else {
                    if (jason.has("$jason")) {
                        view.loaded = false;
                        view.build(jason);
                    } else {

                    }
                }
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void resolve_reference(){
        // convert "+": "$document.blah.blah"
        // to "{{#include $root.$document.blah.blah}}": {}
        String str_jason = jason.toString();

        try {

            // Exclude a pattern that starts with $ => will be handled by resolve_local_reference
            String remote_pattern_with_path_str = "\"([+@])\"[ ]*:[ ]*\"(([^$\"@]+)(@))([^\"]+)\"";
            Pattern remote_pattern_with_path = Pattern.compile(remote_pattern_with_path_str);
            Matcher remote_with_path_matcher = remote_pattern_with_path.matcher(str_jason);
            str_jason = remote_with_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$5\\\\\"].$3}}\": {}");

            // Exclude a pattern that starts with $ => will be handled by resolve_local_reference
            String remote_pattern_without_path_str = "\"([+@])\"[ ]*:[ ]*\"([^$\"]+)\"";
            Pattern remote_pattern_without_path = Pattern.compile(remote_pattern_without_path_str);
            Matcher remote_without_path_matcher = remote_pattern_without_path.matcher(str_jason);
            str_jason = remote_without_path_matcher.replaceAll("\"{{#include \\$root[\\\\\"$2\\\\\"]}}\": {}");

            JSONObject to_resolve = new JSONObject(str_jason);

            refs.put("$document", jason);

            // parse
            JasonParser.getInstance(this.view).setParserListener(new JasonParser.JasonParserListener() {
                @Override
                public void onFinished(JSONObject resolved_jason) {
                    try {
                        resolve_and_build(resolved_jason.toString());
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            });
            JasonParser.getInstance(this.view).parse("json", refs, to_resolve, this.view);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    private void resolve_local_reference(){
        // convert "+": "$document.blah.blah"
        // to "{{#include $root.$document.blah.blah}}": {}
        String str_jason = jason.toString();

        try {

            String local_pattern_str = "\"[+@]\"[ ]*:[ ]*\"[ ]*(\\$document[^\"]*)\"";
            Pattern local_pattern = Pattern.compile(local_pattern_str);
            Matcher local_matcher = local_pattern.matcher(str_jason);
            str_jason = local_matcher.replaceAll("\"{{#include \\$root.$1}}\": {}");

            JSONObject to_resolve = new JSONObject(str_jason);

            refs.put("$document", jason);

            // parse
            JasonParser.getInstance(this.view).setParserListener(new JasonParser.JasonParserListener() {
                @Override
                public void onFinished(JSONObject resolved_jason) {
                    try {
                        resolve_and_build(resolved_jason.toString());
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            });
            JasonParser.getInstance(this.view).parse("json", refs, to_resolve, this.view);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
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
                state.put("$global", ((Launcher)(this.view.getApplicationContext())).getGlobal());
                state.put("$env", ((Launcher)(this.view.getApplicationContext())).getEnv());
                state.put("$params", params);
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {

        }
    }

}
