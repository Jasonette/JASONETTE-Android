package com.jasonette.seed.Service.key;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;
import com.securepreferences.SecurePreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class JasonKeyService {
    SharedPreferences keys;
    private Launcher launcher;
    private Boolean authenticated = false;
    private String caller;
    private Context context;
    public JasonKeyService (Launcher launcherParam) {
        launcher = launcherParam;
        keys = new SecurePreferences(launcher);
    }

    public void forward(String _caller, JSONObject action, JSONObject data, JSONObject event) {
        authenticated = true;
        if (caller.equalsIgnoreCase("request")) {
            request(action, data, event, context);
        } else if (caller.equalsIgnoreCase("add")) {
            add(action, data, event, context);
        } else if (caller.equalsIgnoreCase("remove")) {
            remove(action, data, event, context);
        } else if (caller.equalsIgnoreCase("update")) {
            update(action, data, event, context);
        } else if (caller.equalsIgnoreCase("clear")) {
            clear(action, data, event, context);
        } else if (caller.equalsIgnoreCase("password")) {
            password(action, data, event, context);
       }
    }

    private void _updatePassword(String _caller, JSONObject action, JSONObject data, JSONObject event, Context _context) {
        caller = _caller;
        context = _context;
        String encrypted_passcode = keys.getString("passcode", "");
        Intent intent = new Intent(context, com.jasonette.seed.Lib.PasscodeActivity.class);
        // Authenticated and requestion password,
        // Which means at this time we're trying to update the password.
        intent.putExtra("mode", "register");
        intent.putExtra("action", action.toString());
        intent.putExtra("data", data.toString());
        intent.putExtra("event", event.toString());
        intent.putExtra("caller", _caller);
        try {
            JSONObject callback = new JSONObject();
            callback.put("class", "JasonKeyAction");
            callback.put("method", "on_register");

            // Use dispatchIntent to open the activity so that it can process the callback after closing
            JasonHelper.dispatchIntent(action, data, event, context, intent, callback);
        } catch (Exception e) {

        }
    }
    private void _auth(String _caller, JSONObject action, JSONObject data, JSONObject event, Context _context) {
        caller = _caller;
        context = _context;
        String encrypted_passcode = keys.getString("passcode", "");
        Intent intent = new Intent(context, com.jasonette.seed.Lib.PasscodeActivity.class);
        if (_caller.equalsIgnoreCase("password")) {
            // Not authenticated yet
            // Need to authenticate user first before allowing password update
            intent.putExtra("mode", "authenticate");
        } else if (encrypted_passcode.length() > 0) {
            // authenticate
            intent.putExtra("mode", "authenticate");
        } else {
            // no password set yet. enter new password
            intent.putExtra("mode", "register");
        }

        try {
            JSONObject callback = new JSONObject();
            callback.put("class", "JasonKeyAction");
            callback.put("method", "on_authenticate");

            // Use dispatchIntent to open the activity so that it can process the callback after closing
            JasonHelper.dispatchIntent(action, data, event, context, intent, callback);
        } catch (Exception e) {

        }
    }
    public void register(String raw) {

        String encrypted = SecurePreferences.hashPrefKey(raw);
        SharedPreferences.Editor editor = keys.edit();
        editor.putString("passcode", encrypted);
        editor.commit();
    }
    public Boolean is_authenticated(String raw) {
        String encrypted_passcode = keys.getString("passcode", "");
        if (encrypted_passcode.length() > 0) {
            String after = SecurePreferences.hashPrefKey(raw);
            return after.equals(encrypted_passcode);
        } else {
            return false;
        }
    }

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


    private JSONArray _filter_public(JSONArray items) {
        JSONArray filtered_items = new JSONArray();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject filtered_item = new JSONObject();
                JSONArray keys = item.names();
                for (int j = 0; j < keys.length(); j++) {
                    String key = keys.getString(j);
                    JSONObject component = item.getJSONObject(key);
                    if (component.has("read") && component.getString("read").equalsIgnoreCase("public")) {
                        // "read": "public" component
                        // Add to filtered_components
                        filtered_item.put(key, component);
                    }
                }
                filtered_items.put(filtered_item);
            }
        } catch (Exception e) {
        }
        return filtered_items;
    }


    private JSONArray _filter_query(JSONArray items, JSONArray filter) {
        // items Key/Val Filtering
        try {
            /**

             filter := [{
                 "type": {
                     "value": "BTC"
                 },
                 "name": {
                     "value": "Bitcoin"
                 }
             }, {
                 "type": {
                     "value": "ETH"
                 }
             }]


             items := [{
                 "type": {
                     "value": "BTC",
                     "read": "public"
                 },
                 "name": {
                     "value": "Bitcoin",
                     "read": "public"
                 },
                 "publickey": {
                     "value": "0xm4ng83ng8wengn3kngdn3ngknal32ng".
                     "read": "public"
                 },
                 "privatekey": {
                     "value": "0xngn4bgbebgbgbejbgjbjbjbebsdjbdskb328fg3bgjewh2112hfdhg"
                 }
             }, {
                 ...
             }, {
                 ...
             }]


             Need to filter 'items' with 'filter'.

             Each item in the filter array is a query. All items that match at least one of the filter item gets selected
             for the final result.

             */

            JSONArray filtered_items = new JSONArray();
            // Go through each item and decide whether to include in the final item
            for (int item_index = 0; item_index < items.length(); item_index++) {

                // Testing nth item to decide whether to include
                JSONObject item = items.getJSONObject(item_index);

                // Loop for each filter item
                // and merge all the results
                for (int filter_index = 0; filter_index < filter.length(); filter_index++) {
                    JSONObject filter_item = filter.getJSONObject(filter_index);

                    /*

                        filter_item := {
                            "type": {
                                "value": "BTC"
                            },
                            "name": {
                                "value": "Bitcoin"
                            }
                        }

                        Must match ALL attributes for the filter_item
                    */

                    // filter_keys := ["type", "name"]
                    JSONArray filter_keys = filter_item.names();

                    // innocent by default and loop until proven guilty
                    boolean its_a_match = true;

                    for (int key_index = 0; key_index < filter_keys.length(); key_index++) {
                        String filter_key = filter_keys.getString(key_index);
                        JSONObject filter_value = filter_item.getJSONObject(filter_key);

                        /*

                            filter_key := "type"
                            filter_value := {
                                "value": "BTC"
                            }
                            item := {
                                 "type": {
                                     "value": "BTC",
                                     "read": "public"
                                 },
                                 "name": {
                                     "value": "Bitcoin",
                                     "read": "public"
                                 },
                                 "publickey": {
                                     "value": "0xm4ng83ng8wengn3kngdn3ngknal32ng".
                                     "read": "public"
                                 },
                                 "privatekey": {
                                     "value": "0xngn4bgbebgbgbejbgjbjbjbebsdjbdskb328fg3bgjewh2112hfdhg"
                                 }
                             }

                             check:
                             1. item[filter_key] exists
                             2. item[filter_key]["value"] == filter_value["value"]

                        */

                        if (item.has(filter_key)
                            && item.getJSONObject(filter_key).has("value")
                            && filter_value.has("value")
                            && item.getJSONObject(filter_key).get("value").equals(filter_value.get("value"))){
                            // this condition must match, every time.
                            // Otherwise it needs to fail out.
                        } else {
                            its_a_match = false;
                            break;
                        }
                    }

                    if (its_a_match) {
                        // If this item is a match, no need to look further.
                        filtered_items.put(item);
                        // Go on to checking the next item by breaking
                        break;
                    }
                }
            }
            return filtered_items;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void _fetch(JSONObject action, JSONObject event, JSONObject parsed, Context context) {

        try {
            String serialized = keys.getString(parsed.getString("url"), "{}");
            JSONObject deserialized = new JSONObject(serialized);
            /*

                deserialized := {
                    "items": [{
                        "type": {
                            "value": "BTC",
                            "read": "public"
                        },
                        "name": {
                            "value": "Bitcoin",
                            "read": "public"
                        },
                        "publickey": {
                            "value": "0xm4ng83ng8wengn3kngdn3ngknal32ng".
                            "read": "public"
                        },
                        "privatekey": {
                            "value": "0xngn4bgbebgbgbejbgjbjbjbebsdjbdskb328fg3bgjewh2112hfdhg"
                        }
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
                    items = _filter_public(items);
                } else {
                    // Return the full object
                    // so don't do anything
                }

                // items Key/Val Filtering
                if (action.has("options")) {
                    JSONObject options = action.getJSONObject("options");
                    if (options.has("items")) {
                        JSONArray filter = options.getJSONArray("items");
                        items = _filter_query(items, filter);
                    }
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

     3. Request with query

     {
         "type": "$key.request",
         "options": {
             "url": "file://wallet.json",
             "items": [{
                "type": "BTC"
             }, {
                "type": "BCH"
             }]
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
                if (authenticated) {
                    // IF authenticated, fetch full key items
                    _fetch(action, event, parsed, context);
                    authenticated = false;
                } else {
                    // If not yet authenticated, open passcode
                    _auth("request", action, data, event, context);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    /*******************

     1. Appending to end

     {
         "type": "$key.add",
         "options": {
            "item": {
                "type": {
                    "value": "ETH"
                },
                "publickey": {
                    "value": "0xjdnfenfkdewfhk384b4"
                },
                "name": {
                    "value": "Ethereum"
                },
                "privatekey": {
                    "value": "0x8dbgjenb8fngjwev742gfh47gh8ds87fh3bv"
                }
             }
         }
     }

     2. Adding into index

     {
         "type": "$key.add",
         "options": {
            "index": 1,
            "item": {
                "type": {
                    "value": "ETH"
                },
                "publickey": {
                    "value": "0xjdnfenfkdewfhk384b4"
                },
                "name": {
                    "value": "Ethereum"
                },
                "privatekey": {
                    "value": "0x8dbgjenb8fngjwev742gfh47gh8ds87fh3bv"
                }
            }
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
                try {
                    // Get {"items": [...]} from preferences
                    JSONArray items = _deserialize(url);

                    // Add options object to the "items" array
                    JSONObject options = action.getJSONObject("options");
                    JSONObject item = options.getJSONObject("item");

                    boolean isvalid = _validate(item);
                    if (!isvalid) {
                        JSONObject error = new JSONObject();
                        error.put("message", "Invalid key format. Please follow the $key item schema");
                        JasonHelper.next("error", action, error, event, context);
                        return;
                    }

                    if (options.has("index")) {
                        JSONArray new_items = new JSONArray();
                        int index = options.getInt("index");
                        if (index == items.length()) {
                            new_items.put(item);
                        } else {
                            for (int i=0; i<items.length(); i++) {
                                if (i==options.getInt("index")) {
                                    new_items.put(item);
                                }
                                new_items.put(items.getJSONObject(i));
                            }
                        }
                        items = new_items;
                    } else {
                        items.put(item);
                    }

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


    private boolean _validate(JSONObject item) {
        /**

         item needs to be in the following format

         for each key,
            it has an attribute called 'value' or 'read'. Nothing else.

        {
            "type": {
                "value": [VAL]
            },
            "publickey": {
                "value": "0xjdnfenfkdewfhk384b4"
            },
            "name": {
                "value": "Ethereum"
            },
            "privatekey": {
                "value": "0x8dbgjenb8fngjwev742gfh47gh8ds87fh3bv"
            }
        }

         **/

        try {
            JSONObject allowed = new JSONObject();
            allowed.put("value", true);
            allowed.put("read", true);
            /**

             allowed := {
                "value": true,
                "read": true
             }

             */

            JSONArray keys = item.names();
            for(int i=0; i<keys.length(); i++) {
                JSONObject attrs = item.getJSONObject(keys.getString(i));
                JSONArray attr_keys = attrs.names();
                for(int j=0; j<attr_keys.length(); j++) {
                    if (!allowed.has(attr_keys.getString(j))) {
                        // if any of the attribute keys are not "value" or "read", it's invalid
                        return false;
                    }
                }

            }
        } catch (Exception e) {
            return false;
        }
        return true;
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
                if (authenticated) {
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

                    authenticated = false;
                } else {
                    _auth("remove", action, data, event, context);
                }

            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }





    /*********************************

        {
            "type": "$key.update",
            "options": {
                "index": "{{$jason.index}}",
                "item": {
                    "name": {
                        "value": "{{$jason.new_value}}"
                    },
                    "type": {
                        "value": "BTC"
                    }
                }
            },
            "success": {
                "type": "$render"
            }
        }

     *********************************/
    public void update(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");
            if (isRemote) {
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to update keys remotely");
                JasonHelper.next("error", action, error, event, context);
            } else {
                // local
                if (authenticated) {

                    JSONArray items = _deserialize(url);

                    if (action.has("options") && action.getJSONObject("options").has("index")) {
                        int index = action.getJSONObject("options").getInt("index");
                        JSONArray new_items = new JSONArray();
                        if (items.length() > index) {

                            JSONObject selected_item = items.getJSONObject(index);

                            if (action.getJSONObject("options").has("item")) {
                                // Iterate through all keys and update the value
                                JSONObject subtree_to_update = action.getJSONObject("options").getJSONObject("item");
                                JSONArray keys_to_update = subtree_to_update.names();

                                /**

                                 subtree_to_update := {
                                     "name": {
                                         "value": "{{$jason.new_value}}",
                                         "read": "public"
                                     },
                                     "type": {
                                         "value": "BTC"
                                     }
                                 }

                                 keys_to_update := ["name", "type"]

                                 **/


                                for(int i=0; i<keys_to_update.length(); i++) {
                                    String key = keys_to_update.getString(i);
                                    JSONObject attrs = subtree_to_update.getJSONObject(key);
                                    /**

                                        key := "name"

                                        attrs := {
                                            "value": "{{$jason.new_value}}",
                                            "read": "public"
                                        }

                                     */
                                    JSONArray attr_keys = attrs.names();
                                    for (int j=0; j<attr_keys.length(); j++) {
                                        String attr_key = attr_keys.getString(j);
                                        selected_item.getJSONObject(key).put(attr_key, attrs.get(attr_key));
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

                    authenticated = false;

                } else {
                    _auth("update", action, data, event, context);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }



    /*********************************

     {
         "type": "$key.clear",
     }

     *********************************/

    public void clear(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            Boolean isRemote = parsed.getBoolean("remote");
            String url = parsed.getString("url");
            if (isRemote) {
                JSONObject error = new JSONObject();
                error.put("message", "You are not allowed to clear keys remotely");
                JasonHelper.next("error", action, error, event, context);
            } else {
                if (authenticated) {
                    JSONArray items = new JSONArray();
                    JSONObject res = _serialize(url, items);
                    JasonHelper.next("success", action, res, event, context);
                    authenticated = false;
                } else {
                    _auth("clear", action, data, event, context);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    /*********************************

     {
         "type": "$key.password",
     }

     *********************************/

    public void password(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JSONObject parsed = _parse(action, context);
        try {
            // Password can be reset from anywhere
            if (authenticated) {
                // Called AFTER the user has authenticated
                // Now finally able to update the password
                _updatePassword("password", action, data, event, context);
            } else {
                // Called BEFORE the user authenticated therefore need to _auth() first.
                _auth("password", action, data, event, context);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
