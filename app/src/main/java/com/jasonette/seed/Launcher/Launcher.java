package com.jasonette.seed.Launcher;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.request.target.ViewTarget;
import com.jasonette.seed.Core.JasonModel;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import com.jasonette.seed.BuildConfig;
import com.jasonette.seed.Service.agent.JasonAgentService;
import com.jasonette.seed.Service.websocket.JasonWebsocketService;


public class Launcher extends Application {
    private JSONObject handlers;
    private JSONObject global;
    private JSONObject env;
    private JSONObject models;
    public JSONObject services;
    private static Context currentContext;

    public void call(String serviceName, String methodName, JSONObject action, Context context) {
        try {
            Object service = services.get(serviceName);
            Method method = service.getClass().getMethod(methodName, action.getClass(), Context.class);
            method.invoke(service, action, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    // get current context from anywhere
    public static Context getCurrentContext() {
        return currentContext;
    }
    public static void setCurrentContext(Context context) {
        currentContext = context;
    }

    public void setTabModel(String url, JasonModel model) {
       try {
            models.put(url, model);
       } catch (Exception e) {
           Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
       }
    }
    public JasonModel getTabModel(String url) {
        try {
            if (models.has(url)) {
                return (JasonModel)models.get(url);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }


    public JSONObject getEnv(){
        return this.env;
    }
    public void setEnv(String key, Object json) {
        try {
            this.env.put(key, json);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public JSONObject getGlobal(){
        return this.global;
    }
    public void setGlobal(String key, Object json){
        try {
            this.global.put(key, json);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void resetGlobal(String key){
        try {
            this.global.remove(key);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public Launcher() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ViewTarget.setTagId(R.id.glide_request);

        // Look for all extensions and initialize them if they have initialize class methods
        try {
            String[] fileList = getAssets().list("file");
            for(int i = 0 ; i < fileList.length; i++){
               String filename = fileList[i];
                String jr = null;
                try {
                    InputStream is = getAssets().open("file/" + filename);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    jr = new String(buffer, "UTF-8");
                    JSONObject jrjson = new JSONObject(jr);
                    if(jrjson.has("classname")){
                        String resolved_classname = "com.jasonette.seed.Action." + jrjson.getString("classname");

                        String classmethodName = "initialize";
                        Class<?> classObject = Class.forName(resolved_classname);
                        Method classMethod = classObject.getMethod("initialize", Context.class);
                        classMethod.invoke(classObject, getApplicationContext());
                    }
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }

            services = new JSONObject();
            JasonWebsocketService websocketService = new JasonWebsocketService(this);
            JasonAgentService agentService = new JasonAgentService();
            services.put("JasonWebsocketService", websocketService);
            services.put("JasonAgentService", agentService);


            // handler init
            handlers = new JSONObject();

            // $global
            SharedPreferences global_pref = getSharedPreferences("global", 0);
            this.global = new JSONObject();
            if(global_pref != null){
                Map<String,?> map = global_pref.getAll();
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    try {
                        String val = (String) entry.getValue();
                        Object json = new JSONTokener(val).nextValue();
                        if (json instanceof JSONObject) {
                            this.global.put(entry.getKey(), new JSONObject(val));
                        } else if (json instanceof JSONArray) {
                            this.global.put(entry.getKey(), new JSONArray(val));
                        }
                    } catch (Exception e){
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            }

            this.env = new JSONObject();
            this.models = new JSONObject();

            // device info
            JSONObject device = new JSONObject();
            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

            float width = displayMetrics.widthPixels / displayMetrics.density;
            float height = displayMetrics.heightPixels / displayMetrics.density;
            device.put("width", width);
            device.put("height", height);
            device.put("language", Locale.getDefault().toString());

            JSONObject os = new JSONObject();
            os.put("name", "android");
            os.put("version", Build.VERSION.RELEASE);
            os.put("sdk", Build.VERSION.SDK_INT);

            device.put("os", os);

            this.env.put("device", device);

            JSONObject app = new JSONObject();
            app.put("version", BuildConfig.VERSION_NAME);
            app.put("build", Integer.toString(BuildConfig.VERSION_CODE));
            this.env.put("app", app);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }


    /***************************
     *
     *  Intent schedule/trigger
     *
     ***************************/

    public void on(String key, JSONObject val){
        try {
            JSONObject store = new JSONObject();
            store.put("type", "on");
            store.put("content", val);
            handlers.put(key, store);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void once(String key, JSONObject val){
        try {
            JSONObject store = new JSONObject();
            store.put("type", "once");
            store.put("content", val);
            handlers.put(key, store);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void trigger(JSONObject intent_to_resolve, JasonViewActivity context) {
        try {
            String type = intent_to_resolve.getString("type");
            if (type.equalsIgnoreCase("success")) {
                // success

                Object name = intent_to_resolve.get("name");

                JSONObject handler;
                if(name instanceof String) {
                    handler = getHandler((String)name);
                } else {
                    handler = getHandler(String.valueOf(intent_to_resolve.getInt("name")));
                }

                Intent intent;
                if(intent_to_resolve.has("intent")) {
                    intent = (Intent) intent_to_resolve.get("intent");
                } else {
                    intent = null;
                }

                String classname = handler.getString("class");
                String primaryClassname = "com.jasonette.seed.Action." + classname;
                String secondaryClassname = "com.jasonette.seed.Core." + classname;
                String methodname = handler.getString("method");

                Object module;
                if (context.modules.containsKey(primaryClassname)) {
                    module = context.modules.get(primaryClassname);
                } else if (context.modules.containsKey(secondaryClassname)) {
                    module = context.modules.get(secondaryClassname);
                } else {
                    Class<?> classObject = Class.forName(secondaryClassname);
                    Constructor<?> constructor = classObject.getConstructor();
                    module = constructor.newInstance();
                    context.modules.put(secondaryClassname, module);
                }

                Method method = module.getClass().getMethod(methodname, Intent.class, JSONObject.class);
                JSONObject options = handler.getJSONObject("options");
                method.invoke(module, intent, options);

            } else {
                // error
                JSONObject handler = ((Launcher) context.getApplicationContext()).getHandler(String.valueOf(intent_to_resolve.getInt("name")));
                if (handler.has("options")) {
                    JSONObject options = handler.getJSONObject("options");
                    JSONObject action = options.getJSONObject("action");
                    JSONObject event = options.getJSONObject("event");
                    Context ctxt = (Context) options.get("context");
                    JasonHelper.next("error", action, new JSONObject(), event, ctxt);
                }
            }

            // reset intent_to_resolve
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void callback(JSONObject handler, String result, JasonViewActivity context){
        try {
            String classname = handler.getString("class");
            classname = "com.jasonette.seed.Action." + classname;
            String methodname = handler.getString("method");

            Object module;
            if (context.modules.containsKey(classname)) {
                module = context.modules.get(classname);
            } else {
                Class<?> classObject = Class.forName(classname);
                Constructor<?> constructor = classObject.getConstructor();
                module = constructor.newInstance();
                context.modules.put(classname, module);
            }

            Method method = module.getClass().getMethod(methodname, JSONObject.class, String.class);
            //JSONObject options = handler.getJSONObject("options");
            method.invoke(module, handler, result);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    // Private

    private JSONObject getHandler(String key){
        try {
            // 1. gets the handler
            JSONObject handler = handlers.getJSONObject(key);
            if(handler.has("type")){
                if(handler.getString("type").equalsIgnoreCase("once")){
                    // "once" is a one time thing (Only triggered once):
                    // so we de-register it after getting
                    handlers.remove(key);
                }
            }
            return handler.getJSONObject("content");

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return new JSONObject();
        }
    }

    public OkHttpClient getHttpClient(long timeout) {
        if(timeout > 0) {
            return new OkHttpClient.Builder()
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .build();
        } else {
            return new OkHttpClient.Builder().build();
        }
    }
}
