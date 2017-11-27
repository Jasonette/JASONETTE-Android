package com.jasonette.seed.Action;
import android.content.Context;
import com.jasonette.seed.Launcher.Launcher;
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


    private void _forward(String name, final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).forward("JasonKeyService", name, action, data, event, context);
    }
}
