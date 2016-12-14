package com.jasonette.seed.Action;

import android.content.Context;
import android.util.Log;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;

public class JasonLogAction {
    public void info(final JSONObject action, JSONObject data, Context context) {
        log(action, data, context, "i");
    }
    public void debug(final JSONObject action, JSONObject data, Context context) {
        log(action, data, context, "d");
    }
    public void error(final JSONObject action, JSONObject data, Context context) {
        log(action, data, context, "e");
    }

    private void log(final JSONObject action, JSONObject data, Context context, String mode) {
        try {
            if (action.has("options")) {
                JSONObject options = action.getJSONObject("options");
                if (options.has("text")) {
                    if(mode.equalsIgnoreCase("i")){
                        Log.i("Log", options.getString("text"));
                    } else if(mode.equalsIgnoreCase("d")){
                        Log.d("Log", options.getString("text"));
                    } else if(mode.equalsIgnoreCase("e")){
                        Log.e("Log", options.getString("text"));
                    }
                }
            }
            JasonHelper.next("success", action, data, context);
        } catch (Exception e){
            Log.d("Error", e.toString());
        }

    }
}
