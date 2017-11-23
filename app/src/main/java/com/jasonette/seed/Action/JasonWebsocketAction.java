package com.jasonette.seed.Action;

import android.content.Context;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONObject;

/*****************************************

 ### Actions:
     - There are 3 actions: Open, Close, Send
     - All actions are asynchronous => They don't wait for a response and immediately calls "success"
     - Instead of a return value, all of these actions trigger a service.
     - The corresponding service (can be seen at JasonWebsocketService) emits an event when there's a result.

 [1] Open
     {
         "type": "$websocket.open",
         "options": {
             "url": "..."
         },
         "success": { ... }
     }

 [2] Close
     {
         "type": "$websocket.close",
         "success": { ... }
     }

 [3] Send
     {
         "type": "$websocket.send",
         "options": {
             "message": "..."
         },
         "success": { ... }
     }

 *****************************************/

public class JasonWebsocketAction {
    public void open(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "open", action, context);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void close(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "close", action, context);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void send(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((Launcher)context.getApplicationContext()).call("JasonWebsocketService", "send", action, context);
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
}
