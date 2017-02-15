package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

public class JasonReturnAction {


    /*

    -------------------------------------------

    Throughout every action call chain, the original caller action is passed down inside the 'event' object.

    1. When we reach the end via `$return.success`, we look into  what event.success contains.
    Then we execute $lambda on it.

    2. When we reach the end via `$return.success`, we look into  what event.success contains.
    Then we execute $lambda on it.



    -------------------------------------------

    {
        "$jason": {
            "head": {
                "actions": {
                    "$load": {
                        "type": "$trigger",
                        "options": {
                            "name": "sync"
                        },
                        "success": {
                            "type": "$trigger",
                                "options": {
                                "name": "process"
                            },
                            "success": {
                                "type": "$render"
                            }
                        },
                        "error": {
                            "trigger": "err"
                        }
                    },
                    "sync": {
                        "type": "$network.request",
                        "options": {
                            "url": "https://www.jasonbase.com/things/4nf.json"
                        },
                        "success": {
                            "type": "$return.success"
                        }
                    },
                    "err": {
                        "type": "$util.banner",
                        "options": {
                            "title": "error",
                            "description": "Something went wrong."
                        }
                    }
                },
                "templates": {
                    ...
                }
            }
        }
    }


    whenever triggering something,
    attach the original action as event
    when reaching $return.success or $return.error, just replace it with $event.success
    */



    public void success(final JSONObject action, JSONObject data, final JSONObject event, Context context) {
        Intent intent = new Intent("call");
        if(event.has("success")){
            try{
                intent.putExtra("action", event.getJSONObject("success").toString());
                intent.putExtra("data", data.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        } else {
            JasonHelper.next("success", new JSONObject(), data, event, context);
        }
    }
    public void error(final JSONObject action, JSONObject data, final JSONObject event, Context context) {
        Intent intent = new Intent("call");
        if(event.has("error")){
            try{
                intent.putExtra("action", event.getJSONObject("error").toString());
                intent.putExtra("data", data.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        } else {
            JasonHelper.next("error", new JSONObject(), data, event, context);
        }
    }
}
