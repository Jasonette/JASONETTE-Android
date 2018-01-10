package com.jasonette.seed.Core;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

public class JasonCallback {
    public void href(Intent intent, final JSONObject options) {
        try {
            JSONObject action = options.getJSONObject("action");
            JSONObject event = options.getJSONObject("event");
            Context context = (Context)options.get("context");

            String return_string = intent.getStringExtra("return");
            JSONObject return_value = new JSONObject(return_string);
            JasonHelper.next("success", action, return_value, event, context);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
