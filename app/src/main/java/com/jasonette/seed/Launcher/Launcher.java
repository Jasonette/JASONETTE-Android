package com.jasonette.seed.Launcher;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bumptech.glide.request.target.ViewTarget;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static android.R.attr.action;

public class Launcher extends Application {
    private JSONObject handlers;

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
                }
            }

            // handler init
            handlers = new JSONObject();
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }

    }


    /***************************
     *
        Intent schedule/trigger
     *
     ***************************/

    public void on(String key, JSONObject val){
        try {
            JSONObject store = new JSONObject();
            store.put("type", "on");
            store.put("content", val);
            handlers.put(key, store);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }
    public void once(String key, JSONObject val){
        try {
            JSONObject store = new JSONObject();
            store.put("type", "once");
            store.put("content", val);
            handlers.put(key, store);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }
    public void trigger(JSONObject intent_to_resolve, JasonViewActivity context) {
        try {
            String type = intent_to_resolve.getString("type");
            if (type.equalsIgnoreCase("success")) {
                // success
                JSONObject handler = getHandler(String.valueOf(intent_to_resolve.getInt("name")));
                Intent intent = (Intent) intent_to_resolve.get("intent");

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
            Log.d("Error", e.toString());
            return new JSONObject();
        }
    }

}
