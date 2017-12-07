package com.jasonette.seed.Service.push;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class JasonPushMessageService extends FirebaseMessagingService {

    public JasonPushMessageService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> json = remoteMessage.getData();
            JSONObject payload = new JSONObject();
            JSONObject response = new JSONObject();
            try {
                for (Map.Entry<String, String> entry : json.entrySet())
                {
                    // Detect if the result is JSONObject, JSONArray, or String
                    String val = entry.getValue().trim();
                    if (val.startsWith("[")) {
                        payload.put(entry.getKey(), new JSONArray(val));
                    } else if (val.startsWith("{")) {
                        payload.put(entry.getKey(), new JSONObject(val));
                    } else {
                        payload.put(entry.getKey(), val);
                    }
                }
                response.put("$jason", payload);
                ((JasonViewActivity)Launcher.getCurrentContext()).simple_trigger("$push.onmessage", response, Launcher.getCurrentContext());
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }

}
