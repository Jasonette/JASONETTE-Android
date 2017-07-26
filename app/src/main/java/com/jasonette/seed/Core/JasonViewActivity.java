package com.jasonette.seed.Core;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jasonette.seed.Component.JasonComponentFactory;
import com.jasonette.seed.Component.JasonImageComponent;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;
import com.jasonette.seed.Lib.BackgroundCameraManager;
import com.jasonette.seed.Lib.MaterialBadgeTextView;
import com.jasonette.seed.R;
import com.jasonette.seed.Section.ItemAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.bumptech.glide.Glide.with;

public class JasonViewActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView listView;
    public String url;
    public JasonModel model;
    private ProgressBar loading;

    private ArrayList<RecyclerView.OnItemTouchListener> listViewOnItemTouchListeners;

    private boolean firstResume = true;
    private boolean loaded;
    private boolean fetched;

    private int header_height;
    private ImageView logoView;
    private ArrayList<JSONObject> section_items;
    private HashMap<Integer, AHBottomNavigationItem> bottomNavigationItems;
    public HashMap<String, Object> modules;
    private SwipeRefreshLayout swipeLayout;
    public LinearLayout sectionLayout;
    public RelativeLayout rootLayout;
    public View backgroundCurrentView;
    public WebView backgroundWebview;
    public ImageView backgroundImageView;
    private SurfaceView backgroundCameraView;
    private BackgroundCameraManager cameraManager;
    private AHBottomNavigation bottomNavigation;
    private LinearLayout footerInput;
    private View footer_input_textfield;
    private SearchView searchView;
    private HorizontalDividerItemDecoration divider;
    ArrayList<View> layer_items;

    Parcelable listState;
    JSONObject intent_to_resolve;

    /*************************************************************
     *
     * JASON ACTIVITY LIFECYCLE MANAGEMENT
     *
     ************************************************************/



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        loaded = false;


        // Initialize Parser instance
        JasonParser.getInstance(this);

        listViewOnItemTouchListeners = new ArrayList<RecyclerView.OnItemTouchListener>();

        layer_items = new ArrayList<View>();
        // Setup Layouts

        // 1. Create root layout (Relative Layout)
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        if(rootLayout == null) {
            // Create the root layout
            rootLayout = new RelativeLayout(JasonViewActivity.this);
            rootLayout.setLayoutParams(rlp);
            rootLayout.setFitsSystemWindows(true);
        }


        // 2. Add Swipe layout
        if(swipeLayout == null) {
            swipeLayout = new SwipeRefreshLayout(JasonViewActivity.this);
            swipeLayout.setLayoutParams(rlp);
            rootLayout.addView(swipeLayout);
        }

        // 3. Create body.header
        if(toolbar == null) {
            toolbar = new Toolbar(this);
            toolbar.setTitle("");
        }
        setSupportActionBar(toolbar);

        // 4. Create body.sections

        // 4.1. RecyclerView
        listView = new RecyclerView(this);
        listView.setItemViewCacheSize(20);
        listView.setDrawingCacheEnabled(true);
        listView.setHasFixedSize(true);

        // Create adapter passing in the sample user data
        ItemAdapter adapter = new ItemAdapter(this, this, new ArrayList<JSONObject>());
        // Attach the adapter to the recyclerview to populate items
        listView.setAdapter(adapter);
        // Set layout manager to position the items
        listView.setLayoutManager(new LinearLayoutManager(this));

        // 4.2. LinearLayout
        if(sectionLayout == null){
            // Create LinearLayout
            sectionLayout = new LinearLayout(JasonViewActivity.this);
            sectionLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLayout.setLayoutParams(p);

            // Add toolbar to LinearLayout
            if(toolbar!=null) sectionLayout.addView(toolbar);

            // Add RecyclerView to LinearLayout
            if(listView!=null) sectionLayout.addView(listView);

            // Add LinearLayout to Swipe Layout
            swipeLayout.addView(sectionLayout);
        }

        // 5. Start Loading
        RelativeLayout.LayoutParams loadingLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        loadingLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        loading = new ProgressBar(this);
        loading.setLayoutParams(loadingLayoutParams);
        loading.setVisibility(View.INVISIBLE);
        rootLayout.addView(loading);

        // 6. set root layout as content
        setContentView(rootLayout);

        firstResume = true;

        modules = new HashMap<String, Object>();

        // Parsing Intent
        Intent intent = getIntent();
        if(intent.hasExtra("url")){
            url = intent.getStringExtra("url");
        } else {
            url = getString(R.string.url);
        }

        // Create model
        model = new JasonModel(url, intent, this);

        Uri uri = getIntent().getData();
        if(uri != null && uri.getHost().contains("oauth")) {
            loaded = true; // in case of oauth process we need to set loaded to true since we know it's already been loaded.
            return;
        }

        if(savedInstanceState != null) {
            // Restore model and url
            // Then rebuild the view
            try {
                url = savedInstanceState.getString("url");
                model.url = url;
                if(savedInstanceState.getString("jason")!=null) model.jason = new JSONObject(savedInstanceState.getString("jason"));
                if(savedInstanceState.getString("rendered")!=null) model.rendered = new JSONObject(savedInstanceState.getString("rendered"));
                if(savedInstanceState.getString("state")!=null) model.state = new JSONObject(savedInstanceState.getString("state"));
                if(savedInstanceState.getString("var")!=null) model.var = new JSONObject(savedInstanceState.getString("var"));
                if(savedInstanceState.getString("cache")!=null) model.cache = new JSONObject(savedInstanceState.getString("cache"));
                if(savedInstanceState.getString("params")!=null) model.params = new JSONObject(savedInstanceState.getString("params"));
                if(savedInstanceState.getString("session")!=null) model.session = new JSONObject(savedInstanceState.getString("session"));

                listState = savedInstanceState.getParcelable("listState");

                setup_body(model.rendered);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {

            // offline: true logic
            // 1. check if the url + params signature exists
            // 2. if it does, use that to construct the model and setup_body
            // 3. Go on to fetching (it will be re-rendered if fetch is successful)

            SharedPreferences pref = getSharedPreferences("offline", 0);
            String signature = model.url + model.params.toString();
            if(pref.contains(signature)){
                String offline = pref.getString(signature, null);
                try {
                    JSONObject offline_cache = new JSONObject(offline);
                    model.jason = offline_cache.getJSONObject("jason");
                    model.rendered = offline_cache.getJSONObject("rendered");
                    setup_body(model.rendered);
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            } else {
                if(!model.url.startsWith("file://")) {
                    // only load loading.json if loading from a remote JSON
                    model.fetch_local("file://loading.json");
                }
            }

            // Fetch
            model.fetch();
        }

    }


    
    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onSuccess);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onError);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onCall);

        // Store model to shared preference
        SharedPreferences pref = getSharedPreferences("model", 0);
        SharedPreferences.Editor editor = pref.edit();

        JSONObject temp_model = new JSONObject();
        try {
            if (model.url != null) temp_model.put("url", model.url);
            if (model.jason != null) temp_model.put("jason", model.jason);
            if (model.rendered != null) temp_model.put("rendered", model.rendered);
            if (model.state != null) temp_model.put("state", model.state);
            if (model.var != null) temp_model.put("var", model.var);
            if (model.cache != null) temp_model.put("cache", model.cache);
            if (model.params != null) temp_model.put("params", model.params);
            if (model.session != null) temp_model.put("session", model.session);
            if (model.action != null) temp_model.put("action", model.action);
            if (model.url!= null){
                editor.putString(model.url, temp_model.toString());
                editor.commit();
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(onSuccess, new IntentFilter("success"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onError, new IntentFilter("error"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onCall, new IntentFilter("call"));

        SharedPreferences pref = getSharedPreferences("model", 0);
        if(model.url!=null && pref.contains(model.url)) {
            String str = pref.getString(model.url, null);
            try {
                JSONObject temp_model = new JSONObject(str);
                if(temp_model.has("url")) model.url = temp_model.getString("url");
                if(temp_model.has("jason")) model.jason = temp_model.getJSONObject("jason");
                if(temp_model.has("rendered")) model.rendered = temp_model.getJSONObject("rendered");
                if(temp_model.has("state")) model.state = temp_model.getJSONObject("state");
                if(temp_model.has("var")) model.var = temp_model.getJSONObject("var");
                if(temp_model.has("cache")) model.cache = temp_model.getJSONObject("cache");
                if(temp_model.has("params")) model.params = temp_model.getJSONObject("params");
                if(temp_model.has("session")) model.session = temp_model.getJSONObject("session");
                if(temp_model.has("action")) model.action = temp_model.getJSONObject("action");

                // Delete shared preference after resuming
                SharedPreferences.Editor editor = pref.edit();
                editor.remove(model.url);
                editor.commit();

            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }

        if (!firstResume) {
            onShow();
        }
        firstResume = false;

        Uri uri = getIntent().getData();
        if(uri != null && uri.getHost().contains("oauth")) {
            try {
                intent_to_resolve = new JSONObject();
                intent_to_resolve.put("type", "success");
                intent_to_resolve.put("name", "oauth");
                intent_to_resolve.put("intent", getIntent());
            } catch (JSONException e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }

        // Intent Handler
        // This part is for handling return values from external Intents triggered
        // We set "intent_to_resolve" from onActivityResult() below, and then process it here.
        // It's because onCall/onSuccess/onError callbacks are not yet attached when onActivityResult() is called.
        // Need to wait till this point.
        try {
            if(intent_to_resolve != null) {
                if(intent_to_resolve.has("type")){
                    ((Launcher)getApplicationContext()).trigger(intent_to_resolve, JasonViewActivity.this);
                    intent_to_resolve = null;
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

        super.onResume();

        if (listState != null) {
            listView.getLayoutManager().onRestoreInstanceState(listState);
        }

    }


    // This gets executed automatically when an external intent returns with result
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            // We can't process the intent here because
            // we need to wait until onResume gets triggered (which comes after this callback)
            // onResume reattaches all the onCall/onSuccess/onError callbacks to the current Activity
            // so we need to wait until that happens.
            // Therefore here we only set the "intent_to_resolve", and the actual processing is
            // carried out inside onResume()

            intent_to_resolve = new JSONObject();
            if(resultCode == RESULT_OK) {
                intent_to_resolve.put("type", "success");
                intent_to_resolve.put("name", requestCode);
                intent_to_resolve.put("intent", intent);
            } else {
                intent_to_resolve.put("type", "error");
                intent_to_resolve.put("name", requestCode);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if(model.url!=null) savedInstanceState.putString("url", model.url.toString());
        if(model.jason!=null) savedInstanceState.putString("jason", model.jason.toString());
        if(model.rendered!=null) savedInstanceState.putString("rendered", model.rendered.toString());
        if(model.state!=null) savedInstanceState.putString("state", model.state.toString());
        if(model.var!=null) savedInstanceState.putString("var", model.var.toString());
        if(model.cache!=null) savedInstanceState.putString("cache", model.cache.toString());
        if(model.params!=null) savedInstanceState.putString("params", model.params.toString());
        if(model.session!=null) savedInstanceState.putString("session", model.session.toString());
        if(model.action!=null) savedInstanceState.putString("action", model.action.toString());

        // Store RecyclerView state
        listState = listView.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable("listState", listState);

        super.onSaveInstanceState(savedInstanceState);
    }


    /*************************************************************

     ## Event Handlers Rule ver2.

     1. When there's only $show handler
         - $show: Handles both initial load and subsequent show events

     2. When there's only $load handler
         - $load: Handles Only the initial load event

     3. When there are both $show and $load handlers
         - $load : handle initial load only
         - $show : handle subsequent show events only


     ## Summary

     $load:
         - triggered when view loads for the first time.
     $show:
         - triggered at load time + subsequent show events (IF $load handler doesn't exist)
         - NOT triggered at load time BUT ONLY at subsequent show events (IF $load handler exists)


     *************************************************************/
    void onShow(){
        loaded = true;
        try {
            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject events = head.getJSONObject("actions");
            simple_trigger("$show", new JSONObject(), this);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    void onLoad(){
        loaded = true;
        simple_trigger("$load", new JSONObject(), this);
        try {
            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject events = head.getJSONObject("actions");
            if(events!=null && events.has("$load")){
                // nothing
            } else {
                onShow();
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    void onForeground(){
        // Not implemented yet
    }


    /*************************************************************
     *
     * JASON ACTION DISPATCH
     *
     ************************************************************/

    // How action calls work:
    // 1. First need to resolve the action in case the root level is an array => This means it's an if statment and needs to be parsed once before going forward.
    //      if array => "call" method parses the action once and then calls final_call
    //      else     => "call" immediately calls final_call
    // 2. Then need to parse the "option" part of the action, so that the options will have been filled in. (But NOT success and error, since they need to be parsed AFTER the current action is over)
    //
    // 3. Only then, we actually make the invocation.


    //public void call(final Object action, final JSONObject data, final Context context) {
    public void call(final String action_json, final String data_json, final String event_json, final Context context) {
        try {

            Object action = JasonHelper.objectify(action_json);
            final JSONObject data = (JSONObject)JasonHelper.objectify(data_json);

            JSONObject ev;
            try {
                ev = (JSONObject) JasonHelper.objectify(event_json);
            } catch (Exception e){
                ev = new JSONObject();
            }
            final JSONObject event = ev;

            model.set("state", (JSONObject)data);

            if (action instanceof JSONArray) {
                // resolve
                JasonParser.getInstance(this).setParserListener(new JasonParser.JasonParserListener() {
                    @Override
                    public void onFinished(JSONObject reduced_action) {
                        final_call(reduced_action, data, event, context);
                    }
                });

                JasonParser.getInstance(this).parse("json", model.state, action, context);

            } else {
                final_call((JSONObject)action, data, event, context);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    };
    private void final_call(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {

        try {
            // Handle trigger first
            if (action.has("trigger")) {
                trigger(action, data, event, context);
            } else {
                if(action.length() == 0){
                    return;
                }
                // If not trigger, regular call
                if(action.has("options")){
                    // if action has options, we need to parse out the options first
                    Object options = action.get("options");
                    JasonParser.getInstance(this).setParserListener(new JasonParser.JasonParserListener() {
                        @Override
                        public void onFinished(JSONObject parsed_options) {
                            try {
                                JSONObject action_with_parsed_options = new JSONObject(action.toString());
                                action_with_parsed_options.put("options", parsed_options);
                                exec(action_with_parsed_options, model.state, event, context);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        }
                    });
                    JasonParser.getInstance(this).parse("json", model.state, options, context);
                } else {
                    // otherwise we can just call immediately
                    exec(action, model.state, event, context);
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void trigger(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {

        /****************************************************************************************

        This method is a syntactic sugar for calling a $lambda action.
        The syntax is as follows:

        {
            "trigger": "twitter.get",
            "options": {
                "endpoint": "timeline"
            },
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Uh oh. Something went wrong"
                 }
            }
        }

        Above is a syntactic sugar for the below "$lambda" type action call:

        $lambda action is a special purpose action that triggers another action by name and waits until it returns.
        This way we can define a huge size action somewhere and simply call them as a subroutine and wait for its return value.
        When the subroutine (the action that was triggered by name) returns via `"type": "$return.success"` action,
        the $lambda action picks off where it left off and starts executing its "success" action with the value returned from the subroutine.

        Notice that:
        1. we get rid of the "trigger" field and turn it into a regular action of `"type": "$lambda"`.
        2. the "trigger" value (`"twitter.get"`) gets mapped to "options.name"
        3. the "options" value (`{"endpoint": "timeline"}`) gets mapped to "options.options"


        {
            "type": "$lambda",
            "options": {
                "name": "twitter.get",
                "options": {
                    "endpoint": "timeline"
                }
            },
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Uh oh. Something went wrong"
                 }
            }
        }

        The success / error actions get executed AFTER the triggered action has finished and returns with a return value.

        ****************************************************************************************/


        try {

            // construct options

            if(action.has("options")) {
                Object options = action.get("options");
                JasonParser.getInstance(this).setParserListener(new JasonParser.JasonParserListener() {
                    @Override
                    public void onFinished(JSONObject parsed_options) {
                        try {
                            invoke_lambda(action, data, parsed_options, context);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }
                    }
                });
                JasonParser.getInstance(this).parse("json", model.state, options, context);
            } else {
                JSONObject options = new JSONObject();
                invoke_lambda(action, data, null, context);
            }


        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }



    }
    private void invoke_lambda(final JSONObject action, final JSONObject data, final JSONObject options, final Context context) {

        try {
            // construct lambda
            JSONObject lambda = new JSONObject();
            lambda.put("type", "$lambda");

            JSONObject args = new JSONObject();
            args.put("name", action.getString("trigger"));
            if(options!=null) {
                args.put("options", options);
            }
            lambda.put("options", args);

            if(action.has("success")) {
                lambda.put("success", action.get("success"));
            }
            if(action.has("error")) {
                lambda.put("error", action.get("error"));
            }

            call(lambda.toString(), data.toString(), "{}", context);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    public void simple_trigger(final String event_name, JSONObject data, Context context){
        try{
            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject events = head.getJSONObject("actions");
            // Look up an action by event_name
            Object action = events.get(event_name);
            call(action.toString(), data.toString(), "{}", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void exec(final JSONObject action, final JSONObject data, final JSONObject event, final Context context){
        try {
            String type = action.getString("type");
            if (type.startsWith("$") || type.startsWith("@")){

                String[] tokens = type.split("\\.");
                String className;
                String fileName;
                String methodName;

                if(tokens.length == 1){
                    // Core
                    methodName = type.substring(1);
                    Method method = JasonViewActivity.class.getMethod(methodName, JSONObject.class, JSONObject.class, JSONObject.class, Context.class);
                    method.invoke(this, action, model.state, event, context);
                } else {

                    className = type.substring(1, type.lastIndexOf('.'));


                    // Resolve classname by looking up the json files
                    String resolved_classname = null;
                    String jr = null;
                    try {
                        InputStream is = getAssets().open("file/$" + className + ".json");
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        is.close();
                        jr = new String(buffer, "UTF-8");
                        JSONObject jrjson = new JSONObject(jr);
                        if(jrjson.has("classname")){
                            resolved_classname = jrjson.getString("classname");
                        }
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }


                    if(resolved_classname != null) {
                        fileName = "com.jasonette.seed.Action." + resolved_classname;
                    } else {
                        fileName = "com.jasonette.seed.Action.Jason" + className.toUpperCase().charAt(0) + className.substring(1) + "Action";
                    }

                    methodName = type.substring(type.lastIndexOf('.') + 1);

                    // Look up the module registry to see if there's an instance already
                    // 1. If there is, use that
                    // 2. If there isn't:
                    //      A. Instantiate one
                    //      B. Add it to the registry
                    Object module;
                    if (modules.containsKey(fileName)) {
                        module = modules.get(fileName);
                    } else {
                        Class<?> classObject = Class.forName(fileName);
                        Constructor<?> constructor = classObject.getConstructor();
                        module = constructor.newInstance();
                        modules.put(fileName, module);
                    }

                    Method method = module.getClass().getMethod(methodName, JSONObject.class, JSONObject.class, JSONObject.class, Context.class);
                    model.action = action;
                    method.invoke(module, action, model.state, event, context);
                }


            }
        } catch (Exception e){
            // Action doesn't exist yet
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            try {

                JSONObject alert_action = new JSONObject();
                alert_action.put("type", "$util.banner");

                JSONObject options = new JSONObject();
                options.put("title", "Not implemented");
                String type = action.getString("type");
                options.put("description", action.getString("type") + " is not implemented yet.");

                alert_action.put("options", options);


                call(alert_action.toString(), new JSONObject().toString(), "{}", JasonViewActivity.this);

            } catch (Exception e2){
                Log.d("Warning", e2.getStackTrace()[0].getMethodName() + " : " + e2.toString());
            }
        }
    }


    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver onSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String action_string = intent.getStringExtra("action");
                String data_string = intent.getStringExtra("data");
                String event_string = intent.getStringExtra("event");

                // Wrap return value with $jason
                JSONObject data = addToObject("$jason", data_string);

                // call next
                call(action_string, data.toString(), event_string, JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    };
    private BroadcastReceiver onError = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action_string = intent.getStringExtra("action");
                String data_string = intent.getStringExtra("data");
                String event_string = intent.getStringExtra("event");

                // Wrap return value with $jason
                JSONObject data = addToObject("$jason", data_string);

                // call next
                call(action_string, data.toString(), event_string, JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    };

    private BroadcastReceiver onCall = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action_string = intent.getStringExtra("action");
                String event_string = intent.getStringExtra("event");
                String data_string = intent.getStringExtra("data");
                if(data_string == null){
                    data_string = new JSONObject().toString();
                }

                // Wrap return value with $jason
                JSONObject data = addToObject("$jason", data_string);

                // call next
                call(action_string, data.toString(), event_string, JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    };

    private JSONObject addToObject(String prop, String json_data) {
        JSONObject data = new JSONObject();
        try {
            // Detect if the result is JSONObject, JSONArray, or String
            if(json_data.trim().startsWith("[")) {
                // JSONArray
                data = new JSONObject().put("$jason", new JSONArray(json_data));
            } else if(json_data.trim().startsWith("{")){
                // JSONObject
                data = new JSONObject().put("$jason", new JSONObject(json_data));
            } else {
                // String
                data = new JSONObject().put("$jason", json_data);
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return data;
    }



    /*************************************************************
     *
     * JASON CORE ACTION API
     *
     ************************************************************/


    /**
     * Renders a template using data
     * @param {String} template_name - the name of the template to render
     * @param {JSONObject} data - the data object to render
     */

    public void lambda(final JSONObject action, JSONObject data, JSONObject event, Context context){

        /*

        # Similar to `trigger` keyword, but with a few differences:
        1. Trigger was just for one-off triggering and finish. Lambda waits until the subroutine returns and continues where it left off.
        2. `trigger` was a keyword, but lambda itself is just another type of action. `{"type": "$lambda"}`
        3. Lambda can pass arguments via `options`

        # How it works
        1. Triggers another action by name
        2. Waits for the subroutine to return via `$return.success` or `$return.error`
        3. When the subroutine calls `$return.success`, continue executing from `success` action, using the return value from the subroutine
        4. When the subroutine calls `$return.error`, continue executing from `error` action, using the return value from the subroutine

        # Example 1: Basic lambda (Same as trigger)
        {
            "type": "$lambda",
            "options": {
                "name": "fetch"
            }
        }


        # Example 2: Basic lambda with success/error handlers
        {
            "type": "$lambda",
            "options": {
                "name": "fetch"
            }
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Error"
                }
            }
        }


        # Example 3: Passing arguments
        {
            "type": "$lambda",
            "options": {
                "name": "fetch",
                "options": {
                    "url": "https://www.jasonbase.com/things/73g"
                }
            },
            "success": {
                "type": "$render"
            },
            "error": {
                "type": "$util.toast",
                "options": {
                    "text": "Error"
                }
            }
        }

        # Example 4: Using the previous action's return value

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": {
                    "name": "draw"
                },
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

        # Example 5: Using the previous action's return value as well as custom options

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": {
                    "name": "draw",
                    "options": {
                        "p1": "another param",
                        "p2": "yet another param"
                    }
                },
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

        # Example 6: Using the previous action's return value as well as custom options

        {
            "type": "$network.request",
            "options": {
                "url": "https://www.jasonbase.com/things/73g"
            },
            "success": {
                "type": "$lambda",
                "options": [{
                    "{{#if $jason}}": {
                        "name": "draw",
                        "options": {
                            "p1": "another param",
                            "p2": "yet another param"
                        }
                    }
                }, {
                    "{{#else}}": {
                        "name": "err",
                        "options": {
                            "text": "No content to render"
                        }
                    }
                }],
                "success": {
                    "type": "$render"
                },
                "error": {
                    "type": "$util.toast",
                    "options": {
                        "text": "Error"
                    }
                }
            }
        }

         */

        try{
            if(action.has("options")){
                JSONObject options = action.getJSONObject("options");
                // 1. Resolve the action by looking up from $jason.head.actions
                String event_name = options.getString("name");
                JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
                JSONObject events = head.getJSONObject("actions");
                final Object lambda = events.get(event_name);

                final String caller = action.toString();

                // 2. If `options` exists, use that as the data to pass to the next action
                if(options.has("options")){
                    Object new_options = options.get("options");

                    // take the options and parse it with current model.state
                    JasonParser.getInstance(this).setParserListener(new JasonParser.JasonParserListener() {
                        @Override
                        public void onFinished(JSONObject parsed_options) {
                            try {
                                JSONObject wrapped = new JSONObject();
                                wrapped.put("$jason", parsed_options);
                                call(lambda.toString(), wrapped.toString(), caller, JasonViewActivity.this);
                            } catch (Exception e){
                                JasonHelper.next("error", action, new JSONObject(), new JSONObject(), JasonViewActivity.this);
                            }
                        }
                    });
                    JasonParser.getInstance(this).parse("json", model.state, new_options, context);

                }

                // 3. If `options` doesn't exist, forward the data from the previous action
                else {
                    call(lambda.toString(), data.toString(), caller, JasonViewActivity.this);
                }
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            JasonHelper.next("error", action, new JSONObject(), new JSONObject(), JasonViewActivity.this);
        }

    }

    public void require(final JSONObject action, JSONObject data, final JSONObject event, final Context context){
        /*

         {
            "type": "$require",
            "options": {
                "items": ["https://...", "https://...", ....],
                "item": "https://...."
            }
         }

         Crawl all the items in the array and assign it to the key

         */

        try {
            if (action.has("options")) {
                JSONObject options = action.getJSONObject("options");

                ArrayList<String> urlSet = new ArrayList<>();
                Iterator<?> keys = options.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    Object val = options.get(key);

                    // must be either array or string
                    if(val instanceof JSONArray){
                        for (int i = 0; i < ((JSONArray)val).length(); i++) {
                            if(!urlSet.contains(((JSONArray) val).getString(i))){
                                urlSet.add(((JSONArray) val).getString(i));
                            }
                        }
                    } else if(val instanceof String){
                        if(!urlSet.contains(val)){
                            urlSet.add(((String)val));
                        }
                    }
                }
                if(urlSet.size()>0) {
                    JSONObject refs = new JSONObject();

                    CountDownLatch latch = new CountDownLatch(urlSet.size());
                    ExecutorService taskExecutor = Executors.newFixedThreadPool(urlSet.size());
                    for (String key : urlSet) {
                        taskExecutor.submit(new JasonRequire(key, latch, refs, model.client, this));
                    }
                    try {
                        latch.await();
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }

                    JSONObject res = new JSONObject();

                    Iterator<?> ks = options.keys();
                    while (ks.hasNext()) {
                        String key = (String) ks.next();
                        Object val = options.get(key);
                        if(val instanceof JSONArray){
                            JSONArray ret = new JSONArray();
                            for (int i = 0; i < ((JSONArray)val).length(); i++) {
                                String url = ((JSONArray) val).getString(i);
                                ret.put(refs.get(url));
                            }
                            res.put(key, ret);
                        } else if(val instanceof String){
                            res.put(key, refs.get((String)val));
                        }
                    }
                    JasonHelper.next("success", action, res, event, context);
                }
            } else {
                JasonHelper.next("error", action, new JSONObject(), event, context);
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            JasonHelper.next("error", action, new JSONObject(), event, context);
        }

        // get all urls

    }

    public void render(final JSONObject action, JSONObject data, final JSONObject event, final Context context){
        JasonViewActivity activity = (JasonViewActivity) context;
        try{
            String template_name = "body";
            String type = "json";

            if(action.has("options")){
                JSONObject options = action.getJSONObject("options");
                if(options.has("template")){
                    template_name = options.getString("template");
                }
                // parse the template with JSON
                if(options.has("data")){
                    data.put("$jason", options.get("data"));
                }


                if(options.has("type")){
                    type = options.getString("type");
                }
            }

            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject templates = head.getJSONObject("templates");

            JSONObject template = templates.getJSONObject(template_name);
            JasonParser.getInstance(this).setParserListener(new JasonParser.JasonParserListener() {
                @Override
                public void onFinished(JSONObject body) {

                    setup_body(body);
                    JasonHelper.next("success", action, new JSONObject(), event, context);
                }
            });

            JasonParser.getInstance(this).parse(type, data, template, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            JasonHelper.next("error", action, new JSONObject(), event, context);
        }
    }
    public void set(final JSONObject action, JSONObject data, JSONObject event, Context context){
        try{
            if(action.has("options")){
                JSONObject options = action.getJSONObject("options");
                model.var = JasonHelper.merge(model.var, options);
            }
            JasonHelper.next("success", action, new JSONObject(), event, context);

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public void href(final JSONObject action, JSONObject data, JSONObject event, Context context){
        try {
            if (action.has("options")) {
                String url = action.getJSONObject("options").getString("url");
                String transition = "push";
                if(action.getJSONObject("options").has("transition")){
                    transition = action.getJSONObject("options").getString("transition");
                }

                // "view": "web"
                if (action.getJSONObject("options").has("view")) {
                    String view_type = action.getJSONObject("options").getString("view");
                    if(view_type.equalsIgnoreCase("web") || view_type.equalsIgnoreCase("app")){
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                    return;
                }

                // "view": "jason" (default)
                // Set params for the next view (use either 'options' or 'params')
                String params = null;
                if(action.getJSONObject("options").has("options")){
                    params = action.getJSONObject("options").getJSONObject("options").toString();
                } else if (action.getJSONObject("options").has("params")) {
                    params = action.getJSONObject("options").getJSONObject("params").toString();
                }

                // Reset SharedPreferences so it doesn't overwrite the model onResume
                SharedPreferences pref = getSharedPreferences("model", 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.remove(url);
                editor.commit();

                if(transition.equalsIgnoreCase("replace")){
                    // remove all touch listeners before replacing
                    // Use case : Tab bar
                    removeListViewOnItemTouchListeners();

                    Intent intent = new Intent(this, JasonViewActivity.class);
                    if(params!=null) {
                        intent.putExtra("params", params);
                    }
                    model = new JasonModel(url, intent, this);
                    model.fetch();
                } else {
                    Intent intent = new Intent(this, JasonViewActivity.class);
                    intent.putExtra("url", url);
                    if(params != null) {
                        intent.putExtra("params", params);
                    }
                    startActivity(intent);
                }
            }

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public void back ( final JSONObject action, JSONObject data, JSONObject event, Context context){
        finish();
    }
    public void close ( final JSONObject action, JSONObject data, JSONObject event, Context context){
       finish();
    }
    public void unlock ( final JSONObject action, JSONObject data, JSONObject event, Context context){
        JasonViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    loading.setVisibility(View.GONE);
                    if (swipeLayout != null) {
                        swipeLayout.setRefreshing(false);
                    }
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
    }


    public void reload ( final JSONObject action, JSONObject data, JSONObject event, Context context){
        if(model != null){
            model.fetch();
            try {
                JasonHelper.next("success", action, new JSONObject(), event, context);
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }

    public void flush ( final JSONObject action, JSONObject data, JSONObject event, Context context){
        // there's no default caching on Android. So don't do anything for now
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public void snapshot ( final JSONObject action, JSONObject data, final JSONObject event, final Context context){
        View v1 = getWindow().getDecorView().getRootView();
        v1.setDrawingCacheEnabled(true);
        final Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                String encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("data:image/jpeg;base64,");
                stringBuilder.append(encoded);
                String data_uri = stringBuilder.toString();

                try {
                    JSONObject ret = new JSONObject();
                    ret.put("data", encoded);
                    ret.put("data_uri", data_uri);
                    ret.put("content_type", "image/png");
                    JasonHelper.next("success", action, ret, event, context);
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }

            }
        }).start();
    }

    /*************************************************************
     *
     * JASON VIEW
     *
     ************************************************************/

    public void build(JSONObject jason){
        // set fetched to true since build() is only called after network.request succeeds
        fetched = true;
        if(jason!=null) {
            try {

                // Set up background

                if (jason.getJSONObject("$jason").has("body")) {
                    final JSONObject body;
                    body = (JSONObject) jason.getJSONObject("$jason").getJSONObject("body");
                    setup_body(body);
                }

                if (jason.getJSONObject("$jason").has("head")) {
                    final JSONObject head = jason.getJSONObject("$jason").getJSONObject("head");
                    if (head.has("data")) {
                        if (head.has("templates")) {
                            if (head.getJSONObject("templates").has("body")) {
                                model.set("state", new JSONObject());
                                render(new JSONObject(), model.state, new JSONObject(), this);

                                // return here so onLoad() below will NOT be triggered.
                                // onLoad() will be triggered after render has finished
                                return;
                            }
                        }
                    }
                }

                onLoad();

            } catch (JSONException e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }

    }

    private void setup_body(final JSONObject body) {

        // Store to offline cache in case head.offline == true
        try {
            model.rendered = body;
            invalidateOptionsMenu();
            if(model.jason != null && model.jason.has("$jason") && model.jason.getJSONObject("$jason").has("head") && model.jason.getJSONObject("$jason").getJSONObject("head").has("offline")){
                SharedPreferences pref = getSharedPreferences("offline", 0);
                SharedPreferences.Editor editor = pref.edit();

                String signature = model.url + model.params.toString();
                JSONObject offline_cache = new JSONObject();
                offline_cache.put("jason", model.jason);
                offline_cache.put("rendered", model.rendered);

                editor.putString(signature, offline_cache.toString());
                editor.commit();

            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }



        JasonViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // First need to remove all handlers because they will be reattached after render
                    removeListViewOnItemTouchListeners();

                    if(swipeLayout !=null) {
                        swipeLayout.setRefreshing(false);
                    }
                    sectionLayout.setBackgroundColor(JasonHelper.parse_color("rgb(255,255,255)"));
                    getWindow().getDecorView().setBackgroundColor(JasonHelper.parse_color("rgb(255,255,255)"));

                    if (body.has("style")) {
                        JSONObject style = body.getJSONObject("style");
                        if (style.has("background")) {
                            // sectionLayout must be transparent to see the background
                            sectionLayout.setBackgroundColor(JasonHelper.parse_color("rgba(0,0,0,0)"));

                            // we remove the current view from the root layout
                            if (backgroundCurrentView != null) {
                                rootLayout.removeView(backgroundCurrentView);
                                backgroundCurrentView = null;
                            }

                            if(style.get("background") instanceof String){
                                String background = style.getString("background");
                                JSONObject c = new JSONObject();
                                c.put("url", background);
                                if(background.matches("(file|http[s]?):\\/\\/.*")) {
                                    if(backgroundImageView == null) {
                                        backgroundImageView = new ImageView(JasonViewActivity.this);
                                    }

                                    backgroundCurrentView = backgroundImageView;

                                    DiskCacheStrategy cacheStrategy = DiskCacheStrategy.RESULT;
                                    // gif doesn't work with RESULT cache strategy
                                    // TODO: Check with Glide V4
                                    if (background.matches(".*\\.gif")) {
                                        cacheStrategy = DiskCacheStrategy.SOURCE;
                                    }

                                    with(JasonViewActivity.this)
                                            .load(JasonImageComponent.resolve_url(c, JasonViewActivity.this))
                                            .diskCacheStrategy(cacheStrategy)
                                            .centerCrop()
                                            .into(backgroundImageView);
                                } else if(background.matches("data:image.*")){
                                    String base64;
                                    if(background.startsWith("data:image/jpeg")){
                                        base64 = background.substring("data:image/jpeg;base64,".length());
                                    } else if(background.startsWith("data:image/png")){
                                        base64 = background.substring("data:image/png;base64,".length());
                                    } else if(background.startsWith("data:image/gif")){
                                        base64 = background.substring("data:image/gif;base64,".length());
                                    } else {
                                        base64 = "";    // exception
                                    }
                                    byte[] bs = Base64.decode(base64, Base64.NO_WRAP);

                                    with(JasonViewActivity.this).load(bs).into(new SimpleTarget<GlideDrawable>() {
                                        @Override
                                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                        sectionLayout.setBackground(resource);
                                        }
                                    });
                                } else {
                                    sectionLayout.setBackgroundColor(JasonHelper.parse_color(background));
                                    getWindow().getDecorView().setBackgroundColor(JasonHelper.parse_color(background));
                                }
                            } else {
                                JSONObject background = style.getJSONObject("background");
                                String type = background.getString("type");
                                if(type.equalsIgnoreCase("html")){
                                    if(background.has("text")){
                                        String html = background.getString("text");
                                        CookieManager.getInstance().setAcceptCookie(true);
                                        if(backgroundWebview==null) {
                                            backgroundWebview = new WebView(JasonViewActivity.this);
                                            backgroundWebview.getSettings().setDefaultTextEncodingName("utf-8");
                                            backgroundWebview.setWebChromeClient(new WebChromeClient());
                                            backgroundWebview.setVerticalScrollBarEnabled(false);
                                            backgroundWebview.setHorizontalScrollBarEnabled(false);
                                            backgroundWebview.setBackgroundColor(Color.TRANSPARENT);
                                            WebSettings settings = backgroundWebview.getSettings();
                                            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                                            settings.setJavaScriptEnabled(true);
                                            settings.setDomStorageEnabled(true);
                                            settings.setMediaPlaybackRequiresUserGesture(false);
                                            settings.setJavaScriptCanOpenWindowsAutomatically(true);
                                            settings.setAppCachePath( getCacheDir().getAbsolutePath() );
                                            settings.setAllowFileAccess( true );
                                            settings.setAppCacheEnabled( true );
                                            settings.setCacheMode( WebSettings.LOAD_DEFAULT );

                                            // not interactive by default;
                                            Boolean responds_to_webview = false;
                                            if(background.has("action")){
                                                if(background.getJSONObject("action").has("type")){
                                                    String action_type = background.getJSONObject("action").getString("type");
                                                    if(action_type.equalsIgnoreCase("$default")){
                                                        responds_to_webview = true;
                                                    }
                                                }
                                            }
                                            if(responds_to_webview){
                                                // webview receives click
                                                backgroundWebview.setOnTouchListener(null);
                                            } else {
                                                // webview shouldn't receive click
                                                backgroundWebview.setOnTouchListener(new View.OnTouchListener() {
                                                    @Override
                                                    public boolean onTouch(View v, MotionEvent event) {
                                                        return true;
                                                    }
                                                });
                                            }
                                        }

                                        backgroundCurrentView = backgroundWebview;
                                        backgroundWebview.loadDataWithBaseURL("http://localhost/", html, "text/html", "utf-8", null);
                                    }
                                }
                                else if(type.equalsIgnoreCase("camera")) {
                                    int side = BackgroundCameraManager.FRONT;
                                    if (background.has("options")) {
                                        JSONObject options = background.getJSONObject("options");
                                        if (options.has("device") && options.getString("device").equals("back")) {
                                            side = BackgroundCameraManager.BACK;
                                        }
                                    }

                                    if (cameraManager == null) {
                                        cameraManager = new BackgroundCameraManager(JasonViewActivity.this);
                                        backgroundCameraView = cameraManager.getView();
                                    }

                                    backgroundCurrentView = backgroundCameraView;
                                    cameraManager.setSide(side);
                                }
                            }

                            if (backgroundCurrentView != null) {
                                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.MATCH_PARENT);
                                rootLayout.addView(backgroundCurrentView, 0, rlp);
                            }
                        }
                    }



                    // Set header
                    if (body.has("header")) {
                        setup_header(body.getJSONObject("header"));
                    }
                    // Set sections
                    if (body.has("sections")) {
                        setup_sections(body.getJSONArray("sections"));
                        if(body.has("style") && body.getJSONObject("style").has("border")){
                            String border = body.getJSONObject("style").getString("border");
                            if(border.equalsIgnoreCase("none")){
                                if(divider != null){
                                    listView.removeItemDecoration(divider);
                                    divider = null;
                                }

                            } else {
                                int color = JasonHelper.parse_color(border);
                                listView.removeItemDecoration(divider);
                                divider = new HorizontalDividerItemDecoration.Builder(JasonViewActivity.this)
                                            .color(color)
                                            .showLastDivider()
                                            .positionInsideItem(true)
                                            .build();
                                listView.addItemDecoration(divider);
                            }
                        } else {
                            listView.removeItemDecoration(divider);
                            int color = JasonHelper.parse_color("#eaeaea"); // default color
                            divider = new HorizontalDividerItemDecoration.Builder(JasonViewActivity.this)
                                    .color(color)
                                    .showLastDivider()
                                    .positionInsideItem(true)
                                    .build();
                            listView.addItemDecoration(divider);
                        }
                    } else {
                        setup_sections(null);
                    }


                    swipeLayout.setEnabled(false);
                    if(model.jason.has("$jason") && model.jason.getJSONObject("$jason").has("head")){
                        final JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
                        if(head.has("actions") && head.getJSONObject("actions").has("$pull")) {
                            // Setup refresh listener which triggers new data loading
                            swipeLayout.setEnabled(true);
                            swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                @Override
                                public void onRefresh() {
                                    try {
                                        JSONObject action = head.getJSONObject("actions").getJSONObject("$pull");
                                        call(action.toString(), new JSONObject().toString(), "{}", JasonViewActivity.this);
                                    } catch (Exception e) {
                                    }
                                }
                            });
                        }
                    }


                    if (body.has("style")) {
                        JSONObject style = body.getJSONObject("style");
                        if (style.has("align")) {
                            if (style.getString("align").equalsIgnoreCase("bottom")) {
                                ((LinearLayoutManager) listView.getLayoutManager()).setStackFromEnd(true);
                                listView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                                    @Override
                                    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                                        if (i3 < i7) {
                                            listView.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    listView.smoothScrollToPosition( listView.getAdapter().getItemCount() - 1);
                                                }
                                            }, 100);
                                        }
                                    }
                                });

                            }
                        }
                    }

                    // Set footer
                    if (body.has("footer")) {
                        setup_footer(body.getJSONObject("footer"));
                    }


                    // Set layers
                    if (body.has("layers")){
                        setup_layers(body.getJSONArray("layers"));
                    } else {
                        setup_layers(null);
                    }
                    rootLayout.requestLayout();

                    // if the first time being loaded
                    if(!loaded){
                        // and if the content has finished fetching (not via remote: true)
                        if(fetched) {
                            // trigger onLoad.
                            // onLoad shouldn't be triggered when just drawing the offline cached view initially
                            onLoad();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }



    private void setup_header(JSONObject header){
        try{
            String backgroundColor = header.getJSONObject("style").getString("background");
            toolbar.setBackgroundColor(JasonHelper.parse_color(backgroundColor));
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

        try{
            String color = header.getJSONObject("style").getString("color");
            toolbar.setTitleTextColor(JasonHelper.parse_color(color));
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }


    private void setup_sections(JSONArray sections){
        section_items = new ArrayList<JSONObject>();
        if(sections!=null) {
            try {
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = (JSONObject) sections.getJSONObject(i);

                    // Determine if it's a horizontal section or vertical section
                    // if it's vertical, simply keep adding to the section as individual items
                    // if it's horizontal, start a nested recyclerview
                    if (section.has("type") && section.getString("type").equals("horizontal")) {
                        // horizontal type
                        // TEMPORARY: Add header as an item
                        if (section.has("header")) {
                            JSONObject header = (JSONObject) section.getJSONObject("header");
                            section_items.add(header);
                        }
                        if (section.has("items")) {
                            // Let's add the entire section as an item, under:
                            // "horizontal_section": [items]
                            JSONObject horizontal_section = new JSONObject();
                            horizontal_section.put("horizontal_section", section.getJSONArray("items"));
                            section_items.add(horizontal_section);
                        }
                    } else {
                        // vertical type (default)
                        if (section.has("header")) {
                            JSONObject header = (JSONObject) section.getJSONObject("header");
                            section_items.add(header);
                        }
                        if (section.has("items")) {
                            JSONArray items = (JSONArray) section.getJSONArray("items");
                            for (int j = 0; j < items.length(); j++) {
                                JSONObject item = (JSONObject) items.getJSONObject(j);
                                section_items.add(item);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }

        // Create adapter passing in the sample user data
        ItemAdapter adapter = new ItemAdapter(this, this, section_items);
        // Attach the adapter to the recyclerview to populate items
        listView.setAdapter(adapter);
        // Set layout manager to position the items
        listView.setLayoutManager(new LinearLayoutManager(this));


    }

    private void setup_footer(JSONObject footer){
        try {
            if(footer.has("tabs")){
                setup_tabs(footer.getJSONObject("tabs"));
            } else if(footer.has("input")){
                setup_input(footer.getJSONObject("input"));
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void setup_input(JSONObject input){
       // Set up a horizontal linearlayout
        // which sticks to the bottom
        rootLayout.removeView(footerInput);
        int height = (int) JasonHelper.pixels(JasonViewActivity.this, "60", "vertical");
        int spacing = (int) JasonHelper.pixels(JasonViewActivity.this, "5", "vertical");
        int outer_padding = (int) JasonHelper.pixels(JasonViewActivity.this, "10", "vertical");
        footerInput = new LinearLayout(this);
        footerInput.setOrientation(LinearLayout.HORIZONTAL);
        footerInput.setGravity(Gravity.CENTER_VERTICAL);
        footerInput.setPadding(outer_padding,0,outer_padding,0);
        rootLayout.addView(footerInput);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height);

        params.bottomMargin = 0;
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        footerInput.setLayoutParams(params);

        try {
            if(input.has("style")){
                if(input.getJSONObject("style").has("background")){
                    int color = JasonHelper.parse_color(input.getJSONObject("style").getString("background"));
                    footerInput.setBackgroundColor(color);
                }
            }
            if(input.has("left")) {
                JSONObject json = input.getJSONObject("left");
                JSONObject style;
                if(json.has("style")){
                    style = json.getJSONObject("style");
                }  else {
                    style = new JSONObject();
                }
                style.put("height", "25");
                if(json.has("image")) {
                    json.put("url", json.getString("image"));
                }
                json.put("type", "button");
                json.put("style", style);
                View leftButton = JasonComponentFactory.build(null, json, null, JasonViewActivity.this);
                leftButton.setPadding(spacing,0,spacing,0);
                JasonComponentFactory.build(leftButton, input.getJSONObject("left"), null, JasonViewActivity.this);
                footerInput.addView(leftButton);
            }

            JSONObject textfield;
            if(input.has("textfield")) {
                textfield = input.getJSONObject("textfield");
            } else {
                textfield = input;
            }
            textfield.put("type", "textfield");
            // First build only creates the stub.
            footer_input_textfield= JasonComponentFactory.build(null, textfield, null, JasonViewActivity.this);
            int padding = (int) JasonHelper.pixels(JasonViewActivity.this, "10", "vertical");
            // Build twice because the first build only builds the stub.
            JasonComponentFactory.build(footer_input_textfield, textfield, null, JasonViewActivity.this);
            footer_input_textfield.setPadding(padding, padding, padding, padding);
            footerInput.addView(footer_input_textfield);
            LinearLayout.LayoutParams layout_params = (LinearLayout.LayoutParams)footer_input_textfield.getLayoutParams();
            layout_params.height = LinearLayout.LayoutParams.MATCH_PARENT;
            layout_params.weight = 1;
            layout_params.width = 0;
            layout_params.leftMargin = spacing;
            layout_params.rightMargin = spacing;
            layout_params.topMargin = spacing;
            layout_params.bottomMargin = spacing;

            if(input.has("right")) {
                JSONObject json = input.getJSONObject("right");
                JSONObject style;
                if(json.has("style")){
                    style = json.getJSONObject("style");
                }  else {
                    style = new JSONObject();
                }
                if(!json.has("image") && !json.has("text")){
                    json.put("text", "Send");
                }
                if(json.has("image")) {
                    json.put("url", json.getString("image"));
                }
                style.put("height", "25");

                json.put("type", "button");
                json.put("style", style);
                View rightButton = JasonComponentFactory.build(null, json, null, JasonViewActivity.this);
                JasonComponentFactory.build(rightButton, input.getJSONObject("right"), null, JasonViewActivity.this);
                rightButton.setPadding(spacing,0,spacing,0);
                footerInput.addView(rightButton);
            }

            footerInput.requestLayout();

            listView.setClipToPadding(false);
            listView.setPadding(0,0,0,height);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void setup_tabs(JSONObject tabs){
        try {
            final JSONArray items = tabs.getJSONArray("items");
            if(bottomNavigation == null) {
                bottomNavigation = new AHBottomNavigation(this);
                rootLayout.addView(bottomNavigation);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = 0;
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                bottomNavigation.setLayoutParams(params);
            }
            bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_HIDE);
            bottomNavigation.setBehaviorTranslationEnabled(true);

            bottomNavigation.setDefaultBackgroundColor(Color.parseColor("#FEFEFE"));
            JSONObject style;

            if(tabs.has("style")){
                style = tabs.getJSONObject("style");
                if (style.has("color")) {
                    int color = JasonHelper.parse_color(style.getString("color"));
                    bottomNavigation.setAccentColor(color);
                }
                if (style.has("color:disabled")) {
                    int disabled_color = JasonHelper.parse_color(style.getString("color:disabled"));
                    bottomNavigation.setInactiveColor(disabled_color);
                }
                if (style.has("background")) {
                    int background = JasonHelper.parse_color(style.getString("background"));
                    bottomNavigation.setDefaultBackgroundColor(background);
                    bottomNavigation.setBackgroundColor(background);
                }
            }


            if(bottomNavigation.getItemsCount() == items.length()){
                // if the same number as the previous state, try to fill in the items instead of re-instantiating them all


                for (int i = 0; i < items.length(); i++) {
                    final JSONObject item = items.getJSONObject(i);
                    if(item.has("image")) {
                        final int index = i;
                        JSONObject c = new JSONObject();
                        c.put("url", item.getString("image"));
                        Glide
                                .with(this)
                                .load(JasonImageComponent.resolve_url(c, JasonViewActivity.this))
                                .asBitmap()
                                .into(new SimpleTarget<Bitmap>(100, 100) {
                                    @Override
                                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                                        String text = "";
                                        try {
                                            if (item.has("text")) {
                                                text = item.getString("text");
                                                bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
                                            }
                                        } catch (Exception e) {
                                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                        }
                                        AHBottomNavigationItem tab_item = bottomNavigation.getItem(index);
                                        bottomNavigationItems.put(Integer.valueOf(index), tab_item);
                                        Drawable drawable = new BitmapDrawable(getResources(), resource);
                                        tab_item.setDrawable(drawable);
                                        tab_item.setTitle(text);
                                    }
                                });

                    } else if(item.has("text")){
                        String text = "";
                        try {
                            text = item.getString("text");
                            bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }
                        AHBottomNavigationItem tab_item = bottomNavigation.getItem(i);
                        bottomNavigationItems.put(Integer.valueOf(i), tab_item);
                        ColorDrawable d = new ColorDrawable(Color.TRANSPARENT);
                        tab_item.setDrawable(d);
                        tab_item.setTitle(text);
                    }
                }
            } else {

                bottomNavigationItems = new HashMap<Integer, AHBottomNavigationItem>();
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject item = items.getJSONObject(i);
                    final int index = i;
                    if(item.has("image")) {
                        JSONObject c = new JSONObject();
                        c.put("url", item.getString("image"));
                        with(this)
                            .load(JasonImageComponent.resolve_url(c, JasonViewActivity.this))
                            .asBitmap()
                            .into(new SimpleTarget<Bitmap>(100, 100) {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                                    String text = "";
                                    try {
                                        if (item.has("text")) {
                                            text = item.getString("text");
                                            bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
                                        }
                                    } catch (Exception e) {
                                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                    }
                                    Drawable drawable = new BitmapDrawable(getResources(), resource);
                                    AHBottomNavigationItem item = new AHBottomNavigationItem(text, drawable);
                                    bottomNavigationItems.put(Integer.valueOf(index), item);
                                    if(bottomNavigationItems.size() >= items.length()){
                                        for(int j = 0; j < bottomNavigationItems.size(); j++){
                                            bottomNavigation.addItem(bottomNavigationItems.get(Integer.valueOf(j)));
                                        }
                                    }
                                }
                            });

                    } else if(item.has("text")){
                        String text = "";
                        try {
                            if (item.has("text")) {
                                text = item.getString("text");
                                bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
                            }
                        } catch (Exception e) {
                            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                        }

                        ColorDrawable d = new ColorDrawable(Color.TRANSPARENT);
                        AHBottomNavigationItem tab_item = new AHBottomNavigationItem(text,d);
                        bottomNavigationItems.put(Integer.valueOf(index), tab_item);
                        if(bottomNavigationItems.size() >= items.length()){
                            for(int j = 0; j < bottomNavigationItems.size(); j++){
                                bottomNavigation.addItem(bottomNavigationItems.get(Integer.valueOf(j)));
                            }
                        }
                    }
                }

            }
            bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
                @Override
                public boolean onTabSelected(int position, boolean wasSelected) {
                    try {
                        int current = bottomNavigation.getCurrentItem();
                        JSONObject item = items.getJSONObject(position);
                        if(item.has("href")) {
                            JSONObject action = new JSONObject();
                            JSONObject href = item.getJSONObject("href");
                            if (href.has("transition")) {
                                // nothing
                            } else {
                                href.put("transition", "replace");
                            }
                            action.put("options", href);
                            href(action, new JSONObject(), new JSONObject(), JasonViewActivity.this);
                        } else if(item.has("action")){
                            call(item.get("action").toString(), "{}", "{}", JasonViewActivity.this);
                            return false;
                        } else if(item.has("url")) {
                            String url = item.getString("url");
                            JSONObject action = new JSONObject();
                            JSONObject options = new JSONObject();
                            options.put("url", url);
                            options.put("transition", "replace");
                            action.put("options", options);
                            href(action, new JSONObject(), new JSONObject(), JasonViewActivity.this);
                        }
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                    return true;
                }
            });

            listView.setClipToPadding(false);
            listView.setPadding(0,0,0,160);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    private void setup_layers(JSONArray layers){
        try{
            if(layer_items != null) {
                for (int j = 0; j < layer_items.size(); j++) {
                    View layerView = layer_items.get(j);
                    rootLayout.removeView(layerView);
                }
                layer_items = new ArrayList<View>();
            }
            if(layers != null) {
                for(int i = 0; i<layers.length(); i++){
                    JSONObject layer = (JSONObject)layers.getJSONObject(i);
                    if(layer.has("type")){
                        View view = JasonComponentFactory.build(null, layer, null, JasonViewActivity.this);
                        JasonComponentFactory.build(view, layer, null, JasonViewActivity.this);
                        stylize_layer(view, layer);
                        rootLayout.addView(view);
                        layer_items.add(view);
                    }
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private void stylize_layer(View view, JSONObject component){
        try{
            JSONObject style = JasonHelper.style(component, this);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();

            if(style.has("top")){
                int top = (int) JasonHelper.pixels(JasonViewActivity.this, style.getString("top"), "vertical");
                params.topMargin = top;
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            if(style.has("left")){
                int left = (int) JasonHelper.pixels(JasonViewActivity.this, style.getString("left"), "horizontal");
                params.leftMargin = left;
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }
            if(style.has("right")){
                int right = (int) JasonHelper.pixels(JasonViewActivity.this, style.getString("right"), "horizontal");
                params.rightMargin = right;
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
            if(style.has("bottom")){
                int bottom = (int) JasonHelper.pixels(JasonViewActivity.this, style.getString("bottom"), "vertical");
                params.bottomMargin = bottom;
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            }
            view.setLayoutParams(params);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    // Menu
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            menu = toolbar.getMenu();
            if (model.rendered != null) {
                JSONObject header = model.rendered.getJSONObject("header");

                header_height = toolbar.getHeight();
                setup_title(header);

                if(header.has("search")){
                    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                    final JSONObject search = header.getJSONObject("search");
                    if(searchView == null) {
                        searchView = new SearchView(this);
                        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));


                        toolbar.addView(searchView);
                    } else {
                        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                    }

                    // styling

                    // color
                    int c;
                    if(search.has("style") && search.getJSONObject("style").has("color")){
                        c = JasonHelper.parse_color(search.getJSONObject("style").getString("color"));
                    } else if(header.has("style") && header.getJSONObject("style").has("color")){
                        c = JasonHelper.parse_color(header.getJSONObject("style").getString("color"));
                    } else {
                        c = -1;
                    }
                    if(c > 0) {
                        ImageView searchButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_button);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            searchButton.setImageTintList(ColorStateList.valueOf(JasonHelper.parse_color(header.getJSONObject("style").getString("color"))));
                        }
                    }

                    // background
                    if(search.has("style") && search.getJSONObject("style").has("background")){
                        int bc = JasonHelper.parse_color(search.getJSONObject("style").getString("background"));
                        searchView.setBackgroundColor(bc);
                    }

                    // placeholder
                    if(search.has("placeholder")){
                        searchView.setQueryHint(search.getString("placeholder"));
                    }


                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
                        @Override
                        public boolean onQueryTextSubmit(String s) {
                            // name
                            if(search.has("name")){
                                try {
                                    JSONObject kv = new JSONObject();
                                    kv.put(search.getString("name"), s);
                                    model.var = JasonHelper.merge(model.var, kv);
                                    if(search.has("action")){
                                        call(search.getJSONObject("action").toString(), new JSONObject().toString(), "{}", JasonViewActivity.this);
                                    }
                                } catch (Exception e){
                                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String s) {
                            if(search.has("action")){
                                return false;
                            } else {
                                if(listView != null){
                                    ItemAdapter adapter = (ItemAdapter)listView.getAdapter();
                                    adapter.filter(s);
                                }
                                return true;
                            }
                        }
                    });
                }


                if (header.has("menu")) {
                    JSONObject json = header.getJSONObject("menu");

                    final MenuItem item = menu.add("Menu");
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                    // We're going to create a button.
                    json.put("type", "button");

                    // if it's an image button, both url and image should work
                    if(json.has("image")) {
                        json.put("url", json.getString("image"));
                    }

                    // let's override the style so the menu button size has a sane dimension
                    JSONObject style;
                    if(json.has("style")) {
                        style = json.getJSONObject("style");
                    } else {
                        style = new JSONObject();
                    }

                    // For image, limit the height so it doesn't look too big
                    if(json.has("url")){
                        style.put("height", JasonHelper.pixels(this, "8", "vertical"));
                    }

                    json.put("style", style);

                    // Now creating the menuButton and itemview
                    FrameLayout itemView;
                    View menuButton;
                    if(item.getActionView() == null){
                        // Create itemView if it doesn't exist yet
                        itemView = new FrameLayout(this);
                        menuButton = JasonComponentFactory.build(null, json, null, JasonViewActivity.this);
                        JasonComponentFactory.build(menuButton, json, null, JasonViewActivity.this);
                        itemView.addView(menuButton);
                        item.setActionView(itemView);
                    } else {
                        // Reuse the itemView if it already exists
                        itemView = (FrameLayout)item.getActionView();
                        menuButton = itemView.getChildAt(0);
                        JasonComponentFactory.build(menuButton, json, null, JasonViewActivity.this);
                    }

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    menuButton.setLayoutParams(lp);

                    // Set padding for the menu button
                    int padding = (int)JasonHelper.pixels(this, "15", "vertical");
                    itemView.setPadding(padding, padding, padding, padding);


                    if(json.has("badge")){
                        String badge_text = "";
                        JSONObject badge = json.getJSONObject("badge");
                        if(badge.has("text")) {
                            badge_text = badge.getString("text");
                        }
                        JSONObject badge_style = badge.getJSONObject("style");

                        int color = JasonHelper.parse_color("#ffffff");
                        int background = JasonHelper.parse_color("#ff0000");

                        if(badge_style.has("color")) color = JasonHelper.parse_color(badge_style.getString("color"));
                        if(badge_style.has("background")) background = JasonHelper.parse_color(badge_style.getString("background"));

                        MaterialBadgeTextView v = new MaterialBadgeTextView(this);
                        v.setBackgroundColor(background);
                        v.setTextColor(color);
                        v.setText(badge_text);

                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        //layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
                        int left = (int)JasonHelper.pixels(this, String.valueOf(20), "horizontal");
                        int top = (int)JasonHelper.pixels(this, String.valueOf(-10), "vertical");
                        if(badge_style.has("left")){
                            left = (int)JasonHelper.pixels(this, badge_style.getString("left"), "horizontal");
                        }
                        if(badge_style.has("top")) {
                            top = (int)JasonHelper.pixels(this, String.valueOf(Integer.parseInt(badge_style.getString("top"))), "vertical");
                        }
                        layoutParams.setMargins(left,top,0,0);
                        itemView.addView(v);
                        v.setLayoutParams(layoutParams);
                        itemView.setClipChildren(false);
                        itemView.setClipToPadding(false);

                    }


                    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            try {
                                JSONObject header = model.rendered.getJSONObject("header");
                                if (header.has("menu")) {
                                    if (header.getJSONObject("menu").has("action")) {
                                        call(header.getJSONObject("menu").getJSONObject("action").toString(), new JSONObject().toString(), "{}", JasonViewActivity.this);
                                    } else if (header.getJSONObject("menu").has("href")) {
                                        JSONObject action = new JSONObject().put("type", "$href").put("options", header.getJSONObject("menu").getJSONObject("href"));
                                        call(action.toString(), new JSONObject().toString(), "{}", JasonViewActivity.this);
                                    }
                                }
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                            return true;
                        }
                    });
                }

            }
        }catch(Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    void setup_title(JSONObject header) {
        try {
            toolbar.setTitle("");
            if (header.has("title")) {
                Object title = header.get("title");
                if (title instanceof String) {
                    toolbar.setTitle(header.getString("title"));
                    if(logoView != null){
                        toolbar.removeView(logoView);
                        logoView = null;
                    }
                } else if (title instanceof JSONObject) {
                    String type = ((JSONObject) title).getString("type");
                    if (type.equalsIgnoreCase("image")) {
                        String url = ((JSONObject) title).getString("url");
                        JSONObject c = new JSONObject();
                        c.put("url", url);
                        int height = header_height;
                        int width = Toolbar.LayoutParams.WRAP_CONTENT;
                        if (((JSONObject) title).has("style")) {
                            JSONObject style = ((JSONObject) title).getJSONObject("style");
                            if (style.has("height")) {
                                try {
                                    height = (int) JasonHelper.pixels(this, style.getString("height"), "vertical");
                                } catch (Exception e) {
                                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                }
                            }
                            if (style.has("width")) {
                                try {
                                    width = (int) JasonHelper.pixels(this, style.getString("width"), "horizontal");
                                } catch (Exception e) {
                                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                }
                            }
                        }
                        if (logoView == null) {
                            logoView = new ImageView(JasonViewActivity.this);
                            toolbar.addView(logoView);
                        }
                        Toolbar.LayoutParams params = new Toolbar.LayoutParams(width, height);
                        params.gravity = Gravity.CENTER_HORIZONTAL;
                        logoView.setLayoutParams(params);
                        Glide.with(this)
                                .load(JasonImageComponent.resolve_url(c, JasonViewActivity.this))
                                .into((ImageView) logoView);
                    } else if(type.equalsIgnoreCase("label")){
                        String text = ((JSONObject) title).getString("text");
                        toolbar.setTitle(text);
                        if(logoView != null){
                            toolbar.removeView(logoView);
                            logoView = null;
                        }
                    } else {
                        if(logoView != null){
                            toolbar.removeView(logoView);
                            logoView = null;
                        }
                    }
                }
            } else {
                if(logoView != null){
                    toolbar.removeView(logoView);
                    logoView = null;
                }
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        try {
            for (int i = 0; i < toolbar.getChildCount(); ++i) {
                View child = toolbar.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextSize(20);
                    break;
                }
            }
            if(header.has("style")) {
                String f = header.getJSONObject("style").getString("font:android");
                Typeface font = JasonHelper.get_font(f, this);
                for (int i = 0; i < toolbar.getChildCount(); ++i) {
                    View child = toolbar.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTypeface(font);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    /******************
     * Event listeners
     ******************/

    /**
     * Enables components, or anyone with access to this activity, to listen for item touch events
     * on listView. If the same listener is passed more than once, only the first listener is added.
     * @param listener
     */
    public void addListViewOnItemTouchListener(RecyclerView.OnItemTouchListener listener) {
        if(!listViewOnItemTouchListeners.contains(listener)) {
            listViewOnItemTouchListeners.add(listener);
            listView.addOnItemTouchListener(listener);
        }
    }


    /**
     * Removes all item touch listeners attached to this activity
     * Called when the activity
     */
    public void removeListViewOnItemTouchListeners() {
        for (RecyclerView.OnItemTouchListener listener: listViewOnItemTouchListeners) {
            listView.removeOnItemTouchListener(listener);
            listViewOnItemTouchListeners.remove(listener);
        }
    }
}
