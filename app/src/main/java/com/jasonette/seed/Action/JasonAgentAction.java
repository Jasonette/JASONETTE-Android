package com.jasonette.seed.Action;

import android.content.Context;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONObject;

public class JasonAgentAction {
    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            ((Launcher)context.getApplicationContext()).call("JasonAgentService", "jason_request", action, context);
        } catch (Exception e) {

        }
    }
}
