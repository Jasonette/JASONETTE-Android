package com.jasonette.seed.Action;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Launcher.Launcher;
import com.jasonette.seed.Service.agent.JasonAgentService;

import org.json.JSONObject;

public class JasonAgentAction {
    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            ((Launcher)context.getApplicationContext()).call("JasonAgentService", "jason_request", action, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void refresh(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            JasonAgentService agentService = (JasonAgentService)((Launcher)context.getApplicationContext()).services.get("JasonAgentService");
            agentService.refresh(action, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void clear(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            JasonAgentService agentService = (JasonAgentService)((Launcher)context.getApplicationContext()).services.get("JasonAgentService");
            agentService.clear(action, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void inject(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            JasonAgentService agentService = (JasonAgentService)((Launcher)context.getApplicationContext()).services.get("JasonAgentService");
            agentService.inject(action, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
