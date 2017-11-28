package com.jasonette.seed.Action;
import android.content.Context;
import android.content.Intent;

import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;
import com.jasonette.seed.Service.key.JasonKeyService;

import org.json.JSONObject;

public class JasonKeyAction {

    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("request", action, data, event, context);
    }
    public void add(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("add", action, data, event, context);
    }
    public void remove(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("remove", action, data, event, context);
    }
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("set", action, data, event, context);
    }
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("reset", action, data, event, context);
    }
    public void password(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        _forward("password", action, data, event, context);
    }


    // Called from dispatchIntent
    // Called right after the user has successfully updated the password
    public void on_register(Intent intent, final JSONObject options) {
        try {
            JSONObject action = options.getJSONObject("action");
            JSONObject event = options.getJSONObject("event");
            Context context = (Context) options.get("context");

            // Password update was successful, therefore it's safe to go onto the next action
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {

        }
    }

    // Called from dispatchIntent
    // Called right after the user has successfully authenticated
    public void on_authenticate(Intent intent, final JSONObject options) {
        try {
            JSONObject action = options.getJSONObject("action");
            JSONObject data = options.getJSONObject("data");
            JSONObject event = options.getJSONObject("event");
            Context context = (Context) options.get("context");
            String type = action.getString("type");
            String name = type.split("\\.")[1];

            // Call the JasonKeyService method one more time.
            // The first time 'authenticated' was false and was redirected to _auth.
            // but this time 'authenticated' will be true, therefore will execute the actual intended action
            JasonKeyService keyService = (JasonKeyService)((Launcher)context.getApplicationContext()).services.get("JasonKeyService");
            keyService.forward(name, action, data, event);

        } catch (Exception e) {

        }
    }

    private void _forward(String name, final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).forward("JasonKeyService", name, action, data, event, context);
    }
}
