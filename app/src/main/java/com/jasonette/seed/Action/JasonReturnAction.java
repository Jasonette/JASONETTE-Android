package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jasonette.seed.Core.JasonParser;
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
    private void next(final String type, final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if(event.has(type)){
            try{
                JSONObject options;
                if (action.has("options")) {
                    options = action.getJSONObject("options");
                } else {
                    options = new JSONObject();
                }
                JasonParser.getInstance(context).setParserListener(new JasonParser.JasonParserListener() {
                    @Override
                    public void onFinished(JSONObject parsed_options) {
                        try {
                            Intent intent = new Intent("call");
                            intent.putExtra("action", event.getJSONObject(type).toString());
                            intent.putExtra("data", parsed_options.toString());
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }
                    }
                });
                JasonParser.getInstance(context).parse("json", data, options, context);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {
            JasonHelper.next(type, new JSONObject(), data, event, context);
        }
    }


    public void success(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        next("success", action, data, event, context);
    }
    public void error(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        next("error", action, data, event, context);
    }
}
