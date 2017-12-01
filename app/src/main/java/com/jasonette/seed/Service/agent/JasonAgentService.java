package com.jasonette.seed.Service.agent;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class JasonAgentService {

    // Initialize
    public JasonAgentService() {
    }
    private class JasonAgentInterface {

        private WebView agent;
        private Context context;

        public JasonAgentInterface(WebView agent, Context context) {
            this.agent = agent;
            this.context = context;
        }

        // $agent to Jasonette interface
        @JavascriptInterface
        public void postMessage(String json) {
            /****************************************

             A message can be:

             1. Request

             {
                 "request": {
                     "data": [RPC Object],
                     "nonce": [Auto-generated nonce to handle return values later]
                 }
             }

             2. Response

             {
                 "response": {
                     "data": [Return Value]
                 }
             }

             3. Trigger

             {
                 "trigger": {
                     "name": [Jasonette Event Name],
                     "data": [The "$jason" value to pass along with the event]
                 }
             }

             4. Href

             {
                 "href": {
                     "data": [Jasonette HREF object]
                 }
             }

             ****************************************/


            try {
                JSONObject message = new JSONObject(json);
                JSONObject transaction = (JSONObject) this.agent.getTag();

                if (message.has("request")) {
                    /***
                     1. Request: Agent making request to another agent

                     {
                         "request": {
                             "data": [RPC Object],
                             "nonce": [Auto-generated nonce to handle return values later]
                         }
                     }
                     ***/


                    if (transaction.has("to")) {
                        JSONObject content = message.getJSONObject("request");

                        /**
                            Compose an agent_request object

                            agent_request := {
                                "from": [SOURCE AGENT ID],
                                "request": [JSON-RPC Object],
                                "nonce": [NONCE]
                            }

                        **/
                        JSONObject agent_request = new JSONObject();
                        agent_request.put("from", transaction.getString("to"));
                        agent_request.put("request", content.getJSONObject("data"));
                        agent_request.put("nonce", content.getString("nonce"));

                        request(null, agent_request, this.context);

                    }

                } else if (message.has("response")) {
                    /*****************

                        2. Response

                        {
                            "response": {
                                "data": [Return Value]
                            }
                        }

                     *****************/
                    if (transaction.has("to")) {
                        JSONObject res = message.getJSONObject("response");

                        // Params
                        Object param = res.get("data");


                        // "from": exists => the caller request was from another agent
                        if (transaction.has("from")) {
                            /**
                                Compose an agent_request object

                                agent_request := {
                                    "from": [SOURCE AGENT ID],
                                    "request": [JSON-RPC Object],
                                    "nonce": [NONCE]
                                }

                            **/
                            JSONArray params = new JSONArray();
                            params.put(param);

                            // from
                            String from = transaction.getString("from");

                            if (transaction.has("nonce")) {

                                // 1. Construct jsonrpc
                                JSONObject jsonrpc = new JSONObject();

                                // 1.1. Method: Call the callback method stored under $agent.callbacks object
                                String method = "$agent.callbacks[\"" + transaction.getString("nonce") + "\"]";
                                jsonrpc.put("method", method);

                                // 1.2. id: need to call the callback method on the "from" agent
                                jsonrpc.put("id", from);

                                // 1.3. params array
                                jsonrpc.put("params", params);

                                // 2. Construct the callback agent_request
                                JSONObject agent_request = new JSONObject();
                                // 2.1. "from" and "nonce" should be set to null, since this is a return value, and there is no coming back again.
                                agent_request.put("from", null);
                                agent_request.put("nonce", null);
                                // 2.2. set JSON RPC request
                                agent_request.put("request", jsonrpc);

                                request(null, agent_request, this.context);
                            }

                        // "from" doesn't exist => call from Jasonette => return via success callback
                        } else {
                            if (transaction.has("jason")) {
                                JSONObject original_action = transaction.getJSONObject("jason");
                                // return param as a return value
                                JasonHelper.next("success", original_action, param, new JSONObject(), context);
                            }
                        }

                    }

                } else if (message.has("trigger")) {
                    /************************

                     3. Trigger

                        {
                            "trigger": {
                                "name": [Jasonette Event Name],
                                "data": [The "$jason" value to pass along with the event]
                            }
                        }

                     ************************/
                    JSONObject trigger = message.getJSONObject("trigger");
                    JSONObject m = new JSONObject();
                    if(trigger.has("name")) {
                        // trigger
                        m.put("trigger", trigger.getString("name"));
                        // options
                        if (trigger.has("data")) {
                            m.put("options", trigger.get("data"));
                        }

                        // Call the Jasonette event
                        Intent intent = new Intent("call");
                        intent.putExtra("action", m.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    }


                } else if (message.has("href")) {
                    /************************************

                        4. Href

                        {
                            "href": {
                                "data": [Jasonette HREF object]
                            }
                        }

                     ************************************/

                    if (message.getJSONObject("href").has("data")) {
                        JSONObject m = message.getJSONObject("href").getJSONObject("data");
                        Intent intent = new Intent("call");
                        JSONObject href = new JSONObject();
                        href.put("type", "$href");
                        href.put("options", m);
                        intent.putExtra("action", href.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }


                }
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }



    }

    public WebView setup(final JasonViewActivity context, final JSONObject options, final String id) {

        /**

            1. Initialize WebView
                - Does an agent with the ID exist already?
                    YES => Use that one
                    NO =>
                        1. Create a new WebView
                        2. Attach to the parent view's agents object

            2. Creating a WebView
                - Hide it
                - Set metadata payload on it so it can be referenced later

            3. Filling a WebView
                - Is the state "empty"?
                    YES => Load
                    NO => Ignore

         **/
        WebView agent;


        /*******************************************
            1. Initialize WebView

             - Does an agent with the ID exist already?
                 YES => Use that one
                 NO =>
                     1. Create a new WebView
                     2. Attach to the parent view's agents object

         *******************************************/
        try {
            // An agent with the ID already exists
            if (context.agents.has(id)) {
                agent = (WebView) context.agents.get(id);

            // No such agent exists yet. Create one.
            } else {

                /*******************************************
                    2. Creating a WebView
                 *******************************************/

                // 2.1. Initialize
                CookieManager.getInstance().setAcceptCookie(true);
                agent = new WebView(context);
                agent.getSettings().setDefaultTextEncodingName("utf-8");
                agent.setWebChromeClient(new WebChromeClient());
                agent.setWebViewClient(new WebViewClient() {
                    public void onPageFinished(WebView view, final String url) {
                        // Inject agent.js
                        try {
                            String injection_script = JasonHelper.read_file("agent", context);
                            String interface_script = "$agent.interface.postMessage = function(r) { JASON.postMessage(JSON.stringify(r)); };";
                            view.loadUrl("javascript:" + injection_script + " " + interface_script);

                            // Trigger $agent.ready event
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject attrs = new JSONObject();
                                        attrs.put("id", id);
                                        attrs.put("url", url);
                                        JSONObject res = new JSONObject();
                                        res.put("$jason", attrs);
                                        context.simple_trigger("$agent.ready", res, context);
                                    } catch (Exception e) {
                                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                    }
                                }
                            });

                            JSONObject payload = (JSONObject)view.getTag();
                            payload.put("state", "rendered");
                            view.setTag(payload);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        try {
                            JSONObject payload = (JSONObject)view.getTag();
                            if (payload.has("state") && payload.getString("state").equalsIgnoreCase("rendered")) {
                                if (options.has("action")) {
                                    Intent intent = new Intent("call");
                                    intent.putExtra("action", options.get("action").toString());
                                    intent.putExtra("data", "{\"url\": \"" + url + "\"}");
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                    return true;
                                }
                            }
                        } catch (Exception e) {

                        }
                        return false;
                    }
                });
                agent.setVerticalScrollBarEnabled(false);
                agent.setHorizontalScrollBarEnabled(false);
                agent.setBackgroundColor(Color.TRANSPARENT);
                WebSettings settings = agent.getSettings();
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setAppCachePath(context.getCacheDir().getAbsolutePath());
                settings.setAllowFileAccess(true);
                settings.setAppCacheEnabled(true);
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);

                // 2.2. Create and Attach JavaScript Interface
                JasonAgentInterface agentInterface = new JasonAgentInterface(agent, context);
                agent.addJavascriptInterface(agentInterface, "JASON");

                // 2.3. Hide it
                agent.setVisibility(View.GONE);

                // 2.3. Set Payload
                JSONObject payload = new JSONObject();
                // 2.3.1. copy options
                for (Iterator<String> iterator = options.keys(); iterator.hasNext(); ) {
                    String      key     = iterator.next();
                    JSONObject  value   = options.optJSONObject(key);
                    payload.put(key, value);
                }
                // 2.3.2. Add 'id' and 'state'
                payload.put("to", id);
                payload.put("state", "empty");
                // 2.3.3. Set
                agent.setTag(payload);

                // 2.4. Attach to the parent view
                context.agents.put(id, agent);

            }

            /*******************************************

                 3. Filling the WebView with content
                     - Is the state "empty"?
                         YES => Load
                         NO => Ignore

             *******************************************/

            JSONObject payload = (JSONObject)agent.getTag();

            // Fill in the content if empty
            if(payload.has("state") && payload.getString("state").equalsIgnoreCase("empty")) {
                if (options.has("text")) {
                    String html = options.getString("text");
                    agent.loadDataWithBaseURL("http://localhost/", html, "text/html", "utf-8", null);
                } else if (options.has("url")) {
                    String url = options.getString("url");
                    // 1. file url
                    if (url.startsWith("file://")) {
                        agent.loadUrl("file:///android_asset/file/" + url.substring(7));
                    // 2. remote url
                    } else {
                        agent.loadUrl(url);
                    }
                }
            }

        } catch (Exception e) {
            agent = new WebView(context);
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return agent;
    }



    public void jason_request(final JSONObject jason_request, Context context) {
       request(jason_request, null, context);
    }
    public void request(final JSONObject jason_request, final JSONObject agent_request, Context context) {
        try {

            /*****************************

                1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options

                    jason_request := {
                        "type": "$agent.request",
                        "options": [JSON-RPC Object],
                        "success": [Next Jasonette Action]
                    }

                2. agent_request is a JSON-RPC object

                    agent_request := {
                        "from": [SOURCE AGENT ID],
                        "request": [JSON-RPC Object],
                        "nonce": [NONCE]
                    }

             *****************************/


            /***

                0. Check if it's jason_request or agent_request
                    - If jason_request => Use this to proceed
                    - If agent_request => Use this to proceed

                1. Get JSON-RPC arguments

                    - id
                    - method
                    - params

                2. Find the agent by ID
                    - iF agent exists, go on.

                3. Set the $source on the agent's payload (tag)

                4. Create a JavaScript call string

                5. Run the call script

            ***/

            // Get JSON RPC object
            JSONObject jsonrpc;
            if (jason_request != null) {
                /**
                     1. jason_request is an entire Jasonette action which contains a JSON-RPC object as its options

                     jason_request := {
                         "type": "$agent.request",
                         "options": [JSON-RPC Object],
                         "success": [Next Jasonette Action]
                     }

                 **/
                jsonrpc = jason_request.getJSONObject("options");
            } else {
                /**
                    2. agent_request is a JSON-RPC object

                    agent_request := {
                        "from": [SOURCE AGENT ID],
                        "request": [JSON-RPC Object],
                        "nonce": [NONCE]
                    }

                 **/
                jsonrpc = agent_request.getJSONObject("request");
            }



            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");

                // Special case handling for $webcontainers (For sandboxing per view)
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity)context).model.url;
                }

                if(((JasonViewActivity)context).agents.has(identifier)) {
                    // Find agent by ID
                   final WebView agent = (WebView)((JasonViewActivity)context).agents.get(identifier);


                   /**

                    A transaction looks like this:

                    {
                        "state": "empty",
                        "to": [Agent ID],
                        "from": [Desitnation Agent ID],
                        "nonce": [NONCE],
                        "jason": [The original Jason action that triggered everything],
                        "request": [JSON RPC object]
                    }

                    **/


                    // Set Transaction payload

                    // 1. Initialize
                    JSONObject transaction;
                    if (agent.getTag() != null) {
                        transaction = (JSONObject)agent.getTag();
                    } else {
                        transaction = new JSONObject();
                    }

                    // 2. Fill in
                    if (jason_request != null) {
                        // 2.1. From: Set it as the caller Jasonette action
                        transaction.put("from", null);
                        // 2.2. Nonce
                        transaction.put("nonce", null);
                        // 2.3. Original JASON Caller Action
                        transaction.put("jason", jason_request);
                    } else {
                        // 2.1. From: Set it as the caller agent ID
                        if (agent_request.has("from")) {
                            transaction.put("from", agent_request.getString("from"));
                        }
                        // 2.2. Nonce
                        if (agent_request.has("nonce")) {
                            transaction.put("nonce", agent_request.getString("nonce"));
                        }
                    }

                    agent.setTag(transaction);

                   // Create a JS call string
                    String params = "null";
                    if (jsonrpc.has("method")) {
                        String method = jsonrpc.getString("method");
                        if (jsonrpc.has("params")) {
                            params = jsonrpc.getJSONArray("params").toString();
                        }
                        final String callstring = method + ".apply(this, " + params + ");";
                        ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                agent.loadUrl("javascript:" + callstring);
                            }
                        });
                        return;
                    }
                }
            }

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void refresh(final JSONObject action, final Context context) {
        // Get JSON RPC object
        /**

         action := {
             "type": "$agent.refresh",
             "options": {
                "id": [AGENT ID]
             },
             "success": [Next Jasonette Action]
         }

         **/
        try {
            JSONObject jsonrpc = action.getJSONObject("options");
            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity) context).model.url;
                }
                if (((JasonViewActivity) context).agents.has(identifier)) {
                    final WebView agent = (WebView) ((JasonViewActivity) context).agents.get(identifier);
                    // Find agent by ID
                    ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            agent.loadUrl(agent.getUrl());
                        }
                    });
                    JasonHelper.next("success", action, new JSONObject(), new JSONObject(), context);
                    return;
                }
            }
        } catch (Exception e) {

        }
        JasonHelper.next("error", action, new JSONObject(), new JSONObject(), context);
    }
    public void clear(final JSONObject action, final Context context) {
        // Get JSON RPC object
        /**

         action := {
             "type": "$agent.clear",
             "options": {
                 "id": [AGENT ID]
             },
             "success": [Next Jasonette Action]
         }

         **/
        try {
            JSONObject jsonrpc = action.getJSONObject("options");
            if (jsonrpc.has("id")) {
                String identifier = jsonrpc.getString("id");
                if (identifier.equalsIgnoreCase("$webcontainer")) {
                    identifier = "$webcontainer@" + ((JasonViewActivity) context).model.url;
                }
                if (((JasonViewActivity) context).agents.has(identifier)) {
                    // Find agent by ID
                    final WebView agent = (WebView) ((JasonViewActivity) context).agents.get(identifier);
                    ((JasonViewActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            agent.loadUrl("about:blank");
                        }
                    });
                    JasonHelper.next("success", action, new JSONObject(), new JSONObject(), context);
                    return;
                }
            }
        } catch (Exception e) {

        }
        JasonHelper.next("error", action, new JSONObject(), new JSONObject(), context);
    }
}
