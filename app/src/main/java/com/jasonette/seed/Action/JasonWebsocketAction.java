package com.jasonette.seed.Action;

import android.content.Context;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONObject;

public class JasonWebsocketAction {
    public void open(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "open", action);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void close(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "close", action);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void send(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "send", action);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
}
