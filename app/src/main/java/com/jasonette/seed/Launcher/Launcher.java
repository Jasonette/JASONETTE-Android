package com.jasonette.seed.Launcher;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bumptech.glide.request.target.ViewTarget;
import com.jasonette.seed.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static android.R.attr.action;

public class Launcher extends Application {
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
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }

    }


}
