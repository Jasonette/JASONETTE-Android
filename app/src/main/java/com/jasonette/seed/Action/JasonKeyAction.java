package com.jasonette.seed.Action;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.securepreferences.SecurePreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class JasonKeyAction {
    SharedPreferences keys;

    public JSONObject _parse(final JSONObject action, Context context) {
        JSONObject res = new JSONObject();
        try {
            if (action.has("options")) {
                JSONObject options = action.getJSONObject("options");
                if (options.has("url")) {
                    res.put("url", options.getString("url"));
                    res.put("remote", true);
                } else {
                    res.put("url", ((JasonViewActivity)context).url);
                    res.put("remote", false);
                }
            } else {
                res.put("url", ((JasonViewActivity)context).url);
                res.put("remote", false);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return res;
    }
    private void _fetch(JSONObject action, JSONObject event, JSONObject parsed, Context context) {

        keys = new SecurePreferences(context);

        try {
            String serialized = keys.getString(parsed.getString("url"), "{}");
            JSONObject deserialized = new JSONObject(serialized);
            /*

                deserialized := {
                    "items": [{
                        ...
                    }, {
                        ...
                    }, {
                        ...
                    }]
                }

             */

            Boolean isRemote = parsed.getBoolean("remote");
            JSONArray items;
            if (!deserialized.has("items")) {
                items = new JSONArray();
            } else {
                items = deserialized.getJSONArray("items");
                if (isRemote) {
                    // Need to filter only the public attributes
                    JSONArray filtered_items = new JSONArray();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        JSONArray components = item.getJSONArray("components");
                        JSONArray filtered_components = new JSONArray();
                        for (int j = 0; j < components.length(); j++) {
                            JSONObject component = components.getJSONObject(j);
                            if (component.has("read")) {
                                if (component.getString("read").equalsIgnoreCase("public")) {
                                    // "read": "public" component
                                    // Add to filtered_components
                                    filtered_components.put(component);
                                }
                            }
                        }

                        JSONObject filtered_item = new JSONObject();
                        filtered_item.put("components", filtered_components);

                        filtered_items.put(filtered_item);
                    }

                    items = filtered_items;


                } else {
                    // Return the full object
                    // so don't do anything
                }
            }

            JSONObject res = new JSONObject();
            res.put("items", items);

            JasonHelper.next("success", action, res, event, context);

            // Filter only the "read
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }


    /**

     1. Local Request

     {
         "type": "$key.request"
     }

     2. Remote Request

     {
         "type": "$key.request",
         "options": {
             "url": "file://wallet.json"
         }
     }

     **/
    public void request(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            if (isRemote) {
                // remote.
                // fetch only the public attributes.
                _fetch(action, event, parsed, context);
            } else {
                // local
                // fetch full key items
                _fetch(action, event, parsed, context);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    /*******************
     {
         "type": "$key.add",
         "options": {
             "components": [{
                 "key": "type",
                 "val": "ETH"
             }, {
                 "key": "publickey",
                 "val": "0xjdnfenfkdewfhk384b4"
             }, {
                 "key": "name",
                 "val": "Ethereum"
             }, {
                 "key": "privatekey",
                 "val": "0x8dbgjenb8fngjwev742gfh47gh8ds87fh3bv"
             }]
         }
     }
     *******************/
    public void add(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");

            if (!action.has("options")) {
                JSONObject error = new JSONObject();
                error.put("message", "Must specify an item to add");
                JasonHelper.next("error", action, error, event, context);
                return;
            }

            if (!action.getJSONObject("options").has("components")) {
                JSONObject error = new JSONObject();
                error.put("message", "A key item must have at least one component");
                JasonHelper.next("error", action, error, event, context);
                return;
            }

            if (isRemote) {
                // remote.
                // cannot add from remote
                // 1. Construct error
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to add keys remotely");
                // 2. Trigger error callback
                JasonHelper.next("error", action, error, event, context);
            } else {
                // local
                keys = new SecurePreferences(context);

                try {
                    // Get {"items": [...]} from preferences
                    JSONArray items = _deserialize(url);

                    // Add options object to the "items" array
                    JSONObject options = action.getJSONObject("options");
                    items.put(options);

                    JSONObject res = _serialize(url, items);

                    // Return the updated {"items": [...]} as a return value
                    JasonHelper.next("success", action, res, event, context);

                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }


    private JSONObject _serialize(String url, JSONArray items) {
        try {
            // update the {"items": [...]} with the new "items" array
            JSONObject res = new JSONObject();
            res.put("items", items);

            // Update preference with the items
            SharedPreferences.Editor editor = keys.edit();
            editor.putString(url, res.toString());
            editor.commit();
            return res;

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return new JSONObject();
        }
    }
    private JSONArray _deserialize(String url) {
        try {
            String serialized = keys.getString(url, "{}");
            JSONObject deserialized = new JSONObject(serialized);
            if (deserialized.has("items")) {
                JSONArray items = deserialized.getJSONArray("items");
                return items;
            } else {
                return new JSONArray();
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return new JSONArray();
        }
    }

    /*******************
     {
         "type": "$key.remove",
         "options": {
             "index": 1
         }
     }
     *******************/
    public void remove(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {

            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");
            if (isRemote) {
                // remote
                // cannot remove from remote
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to remove keys remotely");
                JasonHelper.next("error", action, error, event, context);
            } else {
                // local
                keys = new SecurePreferences(context);

                try {
                    // Get {"items": [...]} from preferences
                    JSONArray items = _deserialize(url);

                    // Remove item at index
                    if (action.has("options") && action.getJSONObject("options").has("index")) {
                        int index = action.getJSONObject("options").getInt("index");
                        JSONArray new_items = new JSONArray();
                        if (items.length() > index) {
                            for(int i=0; i<items.length(); i++) {
                                if (i != index) {
                                    new_items.put(items.get(i));
                                }
                            }
                            JSONObject res = _serialize(url, new_items);
                            // Return the updated {"items": [...]} as a return value
                            JasonHelper.next("success", action, res, event, context);
                        } else {
                            JSONObject error = new JSONObject();
                            error.put("message", "Invalid index");
                            JasonHelper.next("error", action, error, event, context);
                        }

                    } else {
                        JSONObject error = new JSONObject();
                        error.put("message", "Need to specify an index to remove from");
                        JasonHelper.next("error", action, error, event, context);
                    }
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }





    /*********************************

         {
             "type": "$key.set",
             "options": {
                 "index": "{{$jason.index}}",
                 "components": [{
                     "key": "name",
                     "val": "{{$jason.new_value}}"
                 }]
             },
             "success": {
                 "type": "$render"
             }
         }

     *********************************/
    public void set(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");
            if (isRemote) {
                // remote
                // cannot remove from remote
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to set keys remotely");
                JasonHelper.next("error", action, error, event, context);
            } else {
                // local
                keys = new SecurePreferences(context);
                JSONArray items = _deserialize(url);

                if (action.has("options") && action.getJSONObject("options").has("index")) {
                    int index = action.getJSONObject("options").getInt("index");
                    JSONArray new_items = new JSONArray();
                    if (items.length() > index) {
                        if (action.getJSONObject("options").has("components")) {
                            JSONArray components_to_update = action.getJSONObject("options").getJSONArray("components");

                            JSONObject old_item = items.getJSONObject(index);
                            JSONArray old_components = old_item.getJSONArray("components");

                            for(int i=0; i<components_to_update.length(); i++) {
                                JSONObject component_to_update = components_to_update.getJSONObject(i);
                                for(int j=0; j<old_components.length(); j++) {
                                    JSONObject old_component = old_components.getJSONObject(j);
                                    if (old_component.getString("key").equalsIgnoreCase(component_to_update.getString("key"))) {
                                        // Match found!
                                        // 1. Update the temp component's val
                                        old_component.put("val", component_to_update.get("val"));
                                        old_components.put(j, old_component);
                                        break;
                                    }
                                }
                            }

                            JSONObject res = _serialize(url, items);
                            // Return the updated {"items": [...]} as a return value
                            JasonHelper.next("success", action, res, event, context);
                        } else {
                            JSONObject error = new JSONObject();
                            error.put("message", "Please specify at least one component to update");
                            JasonHelper.next("error", action, error, event, context);
                        }
                    } else {
                        JSONObject error = new JSONObject();
                        error.put("message", "Invalid index");
                        JasonHelper.next("error", action, error, event, context);
                    }

                } else {
                    JSONObject error = new JSONObject();
                    error.put("message", "Need to specify an index to update");
                    JasonHelper.next("error", action, error, event, context);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }
    public void reset(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");
            if (isRemote) {
                // remote
                // cannot remove from remote
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to reset keys remotely");
                JasonHelper.next("error", action, error, event, context);
            } else {
                // local
                keys = new SecurePreferences(context);
                JSONArray items = new JSONArray();
                JSONObject res = _serialize(url, items);
                JasonHelper.next("success", action, res, event, context);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
