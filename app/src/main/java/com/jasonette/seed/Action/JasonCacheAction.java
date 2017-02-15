package com.jasonette.seed.Action;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Core.JasonViewActivity;
import org.json.JSONObject;

public class JasonCacheAction {
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            JasonViewActivity activity = (JasonViewActivity) context;
            SharedPreferences pref = context.getSharedPreferences("cache", 0);
            SharedPreferences.Editor editor = pref.edit();

            // Merge with the new input
            JSONObject options = action.getJSONObject("options");
            JSONObject old_cache = new JSONObject(pref.getString(activity.url, "{}"));
            JSONObject new_cache = JasonHelper.merge(old_cache, options);

            // Update SharedPreferences
            String stringified_cache = new_cache.toString();
            editor.putString(activity.url, stringified_cache);
            editor.commit();

            // Update model
            ((JasonViewActivity)context).model.cache = new_cache;

            // Execute next
            JasonHelper.next("success", action, new_cache, event, context);

        } catch (Exception e){
            Log.d("Error", e.toString());
        }


    }
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            // Update SharedPreferences
            JasonViewActivity activity = (JasonViewActivity) context;
            SharedPreferences pref = context.getSharedPreferences("cache", 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove(activity.url);
            editor.commit();

            // Update model
            ((JasonViewActivity)context).model.cache = new JSONObject();

            // Execute next
            JasonHelper.next("success", action, new JSONObject(), event, context);

        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
}
