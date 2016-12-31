package com.jasonette.seed.Core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jasonette.seed.Component.JasonComponentFactory;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Helper.JasonSettings;
import com.jasonette.seed.R;
import com.jasonette.seed.Section.ItemAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import static com.bumptech.glide.Glide.with;

public class JasonViewActivity extends AppCompatActivity{
    private Toolbar toolbar;
    private RecyclerView listView;
    public String url;
    public JasonModel model;
    private ProgressBar loading;


    private boolean firstResume = true;

    private int header_height;
    private ImageView logoView;
    private ArrayList<JSONObject> section_items;
    private HashMap<Integer, AHBottomNavigationItem> bottomNavigationItems;
    private HashMap<String, Object> modules;
    private SwipeRefreshLayout swipeLayout;
    public LinearLayout sectionLayout;
    public RelativeLayout rootLayout;
    private AHBottomNavigation bottomNavigation;
    private LinearLayout footerInput;
    private View footer_input_textfield;
    ArrayList<View> layer_items;

    Parcelable listState;


    /*************************************************************
     *
     * JASON ACTIVITY LIFECYCLE MANAGEMENT
     *
     ************************************************************/



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


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

                JSONObject options = new JSONObject();
                options.put("silent", true);
                build(options);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        } else {
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


        if (!firstResume) {
            onShow();
        }
        firstResume = false;

        super.onResume();

        if (listState != null) {
            listView.getLayoutManager().onRestoreInstanceState(listState);
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

        // Store RecyclerView state
        listState = listView.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable("listState", listState);

        super.onSaveInstanceState(savedInstanceState);
    }


    /*************************************************************

     ## Event Handlers Rule

     1. Pick only ONE Between $load and $show.
     - It's because currently action call chains are single threaded on Jason.
     - If you include both, only $load will be triggered.
     2. Use $show if you want to keep content constantly in sync without manually refreshing ($show gets triggered whenever the view shows up while you're in the app, either from a parent view or coming back from a child view).
     3. Use $load if you want to trigger ONLY when the view loads (for populating content once)
     - You still may want to implement a way to refresh the view manually. You can:
     - add some component that calls "$network.request" or "$reload" when touched
     - add "$pull" event handler that calls "$network.request" or "$reload" when user makes a pull to refresh action
     4. Use $foreground to handle coming background ($show only gets triggered WHILE you're on the app)

     *************************************************************/
    void onShow(){
        try {
            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject events = head.getJSONObject("actions");
            if(events!=null && !events.has("$load")){
                trigger("$show", new JSONObject(), this);
            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
    void onLoad(){
        trigger("$load", new JSONObject(), this);
        onShow();
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
    public void call(final String action_json, final String data_json, final Context context) {

        try {

            Object action = JasonHelper.objectify(action_json);
            final JSONObject data = (JSONObject)JasonHelper.objectify(data_json);

            model.set("state", (JSONObject)data);

            if (action instanceof JSONArray) {
                // resolve
                JasonParser.getInstance().setParserListener(new JasonParser.JasonParserListener() {
                    @Override
                    public void onFinished(JSONObject reduced_action) {
                        final_call(reduced_action, data, context);
                    }
                });

                JasonParser.getInstance().parse("json", model.state, action, context);

            } else {
                final_call((JSONObject)action, data, context);
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    };
    private void final_call(final JSONObject action, final JSONObject data, final Context context) {

        try {
            // Handle trigger first
            if (action.has("trigger")) {
                trigger(action.getString("trigger"), data, context);
            } else {
                // If not trigger, regular call
                if(action.has("options")){
                    // if action has options, we need to parse out the options first
                    Object options = action.get("options");
                    JasonParser.getInstance().setParserListener(new JasonParser.JasonParserListener() {
                        @Override
                        public void onFinished(JSONObject parsed_options) {
                            try {
                                JSONObject action_with_parsed_options = new JSONObject(action.toString());
                                action_with_parsed_options.put("options", parsed_options);
                                exec(action_with_parsed_options, model.state, context);
                            } catch (Exception e) {
                                Log.d("Error", e.toString());
                            }
                        }
                    });
                    JasonParser.getInstance().parse("json", model.state, options, context);
                } else {
                    // otherwise we can just call immediately
                    exec(action, model.state, context);
                }
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    public void trigger(final String event_name, JSONObject data, Context context){
        try{
            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject events = head.getJSONObject("actions");

            // Look up an action by event_name
            Object action = events.get(event_name);
            call(action.toString(), data.toString(), context);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }

    }

    private void exec(final JSONObject action, final JSONObject data, final Context context){
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
                    Method method = JasonViewActivity.class.getMethod(methodName, JSONObject.class, JSONObject.class, Context.class);
                    method.invoke(this, action, model.state, context);
                } else {
                    className = type.substring(1, type.lastIndexOf('.'));
                    fileName = "com.jasonette.seed.Action.Jason" + className.toUpperCase().charAt(0) + className.substring(1) + "Action";
                    methodName = type.substring(type.lastIndexOf('.') + 1);

                    // Turn on Loading indicator if it's an async action
                    if(JasonSettings.isAsync(className, this)){
                        JasonViewActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    loading.setVisibility(View.VISIBLE);
                                } catch (Exception e) {
                                    Log.d("Error", e.toString());
                                }
                            }
                        });
                    }

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

                    Method method = module.getClass().getMethod(methodName, JSONObject.class, JSONObject.class, Context.class);
                    method.invoke(module, action, model.state, context);

                }


            }
        } catch (Exception e){
            // Action doesn't exist yet
            Log.d("Error", e.toString());
            try {

                JSONObject alert_action = new JSONObject();
                alert_action.put("type", "$util.banner");

                JSONObject options = new JSONObject();
                options.put("title", "Not implemented");
                String type = action.getString("type");
                options.put("description", action.getString("type") + " is not implemented yet.");

                alert_action.put("options", options);


                call(alert_action.toString(), new JSONObject().toString(), JasonViewActivity.this);

            } catch (Exception err){
                Log.d("Error", err.toString());
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

                // Wrap return value with $jason
                JSONObject data;

                // Detect if the result is JSONObject, JSONArray, or String
                if(data_string.trim().startsWith("[")) {
                    // JSONArray
                    JSONArray json = new JSONArray(data_string);
                    data = new JSONObject().put("$jason", new JSONArray(data_string));
                } else if(data_string.trim().startsWith("{")){
                    // JSONObject
                    JSONObject json = new JSONObject(data_string);
                    data = new JSONObject().put("$jason", new JSONObject(data_string));
                } else {
                    // String
                    data = new JSONObject().put("$jason", data_string);
                }

                // call next
                call(action_string, data.toString(), JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        }
    };
    private BroadcastReceiver onError = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action_string = intent.getStringExtra("action");
                String data_string = intent.getStringExtra("data");

                // Wrap return value with $jason
                JSONObject data;
                if(data_string.startsWith("[")) {
                    JSONArray json = new JSONArray(data_string);
                    data = new JSONObject().put("$jason", new JSONArray(data_string));
                } else {
                    JSONObject json = new JSONObject(data_string);
                    data = new JSONObject().put("$jason", new JSONObject(data_string));
                }

                // call next
                call(action_string, data.toString(), JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        }
    };
    private BroadcastReceiver onCall = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action_string = intent.getStringExtra("action");

                // call next
                call(action_string, new JSONObject().toString(), JasonViewActivity.this);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        }
    };





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


    public void render(final JSONObject action, JSONObject data, Context context){
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


                if(options.has("type")){
                    type = options.getString("type");
                }
            }

            JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
            JSONObject templates = head.getJSONObject("templates");

            JSONObject template = templates.getJSONObject(template_name);
            JasonParser.getInstance().setParserListener(new JasonParser.JasonParserListener() {
                @Override
                public void onFinished(JSONObject body) {
                    setup_body(body);
                }
            });

            JasonParser.getInstance().parse(type, data, template, getApplicationContext());

        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
    public void set(final JSONObject action, JSONObject data, Context context){
        try{
            if(action.has("options")){
                JSONObject options = action.getJSONObject("options");
                model.var = JasonHelper.merge(model.var, options);
            }
            JasonHelper.next("success", action, new JSONObject(), context);

        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }

    public void href(final JSONObject action, JSONObject data, Context context){
        try {
            if (action.has("options")) {
                String url = action.getJSONObject("options").getString("url");
                String view_type = "json";
                if (action.getJSONObject("options").has("view")) {
                    view_type = action.getJSONObject("options").getString("view");
                }
                if(!view_type.equalsIgnoreCase("app")) {
                    url = JasonHelper.url(url, context);
                }
                String transition = "push";
                if(action.getJSONObject("options").has("transition")){
                    transition = action.getJSONObject("options").getString("transition");
                }

                // "view": "web"
                if(view_type.equalsIgnoreCase("web") || view_type.equalsIgnoreCase("app")){
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
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

                if(transition.equalsIgnoreCase("replace")){
                    model = new JasonModel(url, null, this);
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
            Log.d("Error", e.toString());
        }
    }

    public void close ( final JSONObject action, JSONObject data, Context context){
       finish();
    }
    public void unlock ( final JSONObject action, JSONObject data, Context context){
        JasonViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    loading.setVisibility(View.GONE);
                    if (swipeLayout != null) {
                        swipeLayout.setRefreshing(false);
                    }
                } catch (Exception e) {
                    Log.d("Error", e.toString());
                }
            }
        });
    }


    public void reload ( final JSONObject action, JSONObject data, Context context){
        if(model != null){
            model.fetch();
            try {
                JasonHelper.next("success", action, new JSONObject(), context);
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }
    }

    public void flush ( final JSONObject action, JSONObject data, Context context){
        // there's no default caching on Android. So don't do anything for now
        try {
            JasonHelper.next("success", action, new JSONObject(), context);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    /*************************************************************
     *
     * JASON VIEW
     *
     ************************************************************/

    public void build(JSONObject options){
        if(model.jason!=null) {
            try {

                // Set up background

                if (model.jason.getJSONObject("$jason").has("body")) {
                    final JSONObject body;
                    body = (JSONObject) model.jason.getJSONObject("$jason").getJSONObject("body");
                    setup_body(body);
                }

                if (model.jason.getJSONObject("$jason").has("head")) {
                    final JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
                    if (head.has("data")) {
                        if (head.has("templates")) {
                            if (head.getJSONObject("templates").has("body")) {
                                model.set("state", new JSONObject());
                                render(new JSONObject(), model.state, this);
                            }
                        }
                    }
                }

                if(options!=null && options.has("silent") && options.getBoolean("silent")){
                    // silent build
                } else {
                    // non-silent build => trigger events
                    onLoad();
                }


            } catch (JSONException e) {
                Log.d("Error", e.toString());
            }
        }

    }

    private void setup_body(final JSONObject body) {
        model.rendered = body;
        invalidateOptionsMenu();

        JasonViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    loading.setVisibility(View.GONE);
                    if(swipeLayout !=null) {
                        swipeLayout.setRefreshing(false);
                    }
                    if (body.has("style")) {
                        JSONObject style = body.getJSONObject("style");
                        if (style.has("background")) {
                            if(style.get("background") instanceof String){
                                String background = style.getString("background");
                                if(background == "camera") {
                                    // TODO
                                } else {
                                    try {
                                        // Set the background to a color, if it is parseable
                                        int bg_color = JasonHelper.parse_color(background);
                                        getWindow().getDecorView().setBackgroundColor(bg_color);
                                    } catch (IllegalArgumentException e) {
                                        // Not a color, get the URL
                                        try {
                                            background = JasonHelper.url(background, JasonViewActivity.this);
                                        } catch (MalformedURLException me) {
                                            Log.d("Error", me.toString());
                                        }
                                        if (background.matches(".*\\.gif")) {
                                            with(JasonViewActivity.this).load(background).asGif().into(new SimpleTarget<GifDrawable>() {
                                                @Override
                                                public void onResourceReady(GifDrawable resource, GlideAnimation<? super GifDrawable> glideAnimation) {
                                                    sectionLayout.setBackground(resource);

                                                }
                                            });
                                        } else {
                                            with(JasonViewActivity.this).load(background).into(new SimpleTarget<GlideDrawable>() {
                                                @Override
                                                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                                    sectionLayout.setBackground(resource);
                                                }
                                            });
                                        }
                                    }
                                }
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
                    } else {
                        setup_sections(null);
                    }


                    final JSONObject head = model.jason.getJSONObject("$jason").getJSONObject("head");
                    if(head.has("actions") && head.getJSONObject("actions").has("$pull")){
                        // Setup refresh listener which triggers new data loading
                        swipeLayout.setEnabled(true);
                        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                try {
                                    JSONObject action = head.getJSONObject("actions").getJSONObject("$pull");
                                    call(action.toString(), new JSONObject().toString(), JasonViewActivity.this);
                                } catch (Exception e) {
                                }
                            }
                        });
                    } else {
                        swipeLayout.setEnabled(false);
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
            Log.d("Error", e.toString());
        }

        try{
            String color = header.getJSONObject("style").getString("color");
            toolbar.setTitleTextColor(JasonHelper.parse_color(color));
        } catch (Exception e){
            Log.d("Error", e.toString());
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
                Log.d("Error", e.toString());
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
            Log.d("Error", e.toString());
        }
    }

    private void setup_input(JSONObject input){
       // Set up a horizontal linearlayout
        // which sticks to the bottom
        if(footerInput != null) {
            ((EditText)footer_input_textfield).setText("");
        } else {
            // build footer.input shell and position it to the bottom
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

            // add left button and add
            try {
                JSONObject style = new JSONObject();
                style.put("height", "25");

                if(input.has("left")) {
                    JSONObject json = input.getJSONObject("left");
                    if(json.has("image")) {
                        json.put("url", JasonHelper.url(json.getString("image"), JasonViewActivity.this));
                    }
                    json.put("type", "button");
                    json.put("style", style);
                    View leftButton = JasonComponentFactory.build(null, json, null, JasonViewActivity.this);
                    JasonComponentFactory.build(leftButton, input.getJSONObject("left"), null, JasonViewActivity.this);
                    footerInput.addView(leftButton);
                }

                input.put("type", "textfield");
                footer_input_textfield= JasonComponentFactory.build(null, input, null, JasonViewActivity.this);
                int padding = (int) JasonHelper.pixels(JasonViewActivity.this, "10", "vertical");
                JasonComponentFactory.build(footer_input_textfield, input, null, JasonViewActivity.this);
                footer_input_textfield.setPadding(padding, padding, padding, padding);

                footerInput.addView(footer_input_textfield);
                LinearLayout.LayoutParams layout_params = (LinearLayout.LayoutParams)footer_input_textfield.getLayoutParams();
                layout_params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                layout_params.weight = 1;
                layout_params.width = 0;
                layout_params.leftMargin = spacing;
                layout_params.rightMargin = spacing;

                if(input.has("right")) {
                    JSONObject json = input.getJSONObject("right");
                    if(!json.has("image") && !json.has("text")){
                        json.put("text", "Send");
                    }
                    if(json.has("image")) {
                        json.put("url", JasonHelper.url(json.getString("image"), JasonViewActivity.this));
                    }
                    json.put("type", "button");
                    json.put("style", style);
                    View rightButton = JasonComponentFactory.build(null, json, null, JasonViewActivity.this);
                    JasonComponentFactory.build(rightButton, input.getJSONObject("right"), null, JasonViewActivity.this);
                    rightButton.setPadding(0,0,0,0);
                    footerInput.addView(rightButton);
                }

                footerInput.requestLayout();

                listView.setClipToPadding(false);
                listView.setPadding(0,0,0,height);
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
            // add textfield
            // ad right button ad add
        }
    }

    private void setup_tabs(JSONObject tabs){
        try {
            JSONObject style = tabs.getJSONObject("style");
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


            if(bottomNavigation.getItemsCount() == items.length()){
                // if the same number as the previous state, try to fill in the items instead of re-instantiating them all

                for (int i = 0; i < items.length(); i++) {
                    final JSONObject item = items.getJSONObject(i);
                    if(item.has("image")) {
                        final int index = i;
                        Glide
                                .with(this)
                                .load(JasonHelper.url(item.getString("image"), JasonViewActivity.this))
                                .asBitmap()
                                .into(new SimpleTarget<Bitmap>(100, 100) {
                                    @Override
                                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                                        String text = "";
                                        try {
                                            if (item.has("text")) {
                                                text = item.getString("text");
                                            }
                                        } catch (Exception e) {
                                            Log.d("Error", e.toString());
                                        }
                                        AHBottomNavigationItem item = bottomNavigation.getItem(index);
                                        bottomNavigationItems.put(Integer.valueOf(index), item);
                                        Drawable drawable = new BitmapDrawable(getResources(), resource);
                                        item.setDrawable(drawable);
                                        item.setTitle(text);
                                    }
                                });

                    }
                }
            } else {

                bottomNavigationItems = new HashMap<Integer, AHBottomNavigationItem>();
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject item = items.getJSONObject(i);
                    final int index = i;
                    if(item.has("image")) {
                        with(this)
                                .load(JasonHelper.url(item.getString("image"), JasonViewActivity.this))
                                .asBitmap()
                                .into(new SimpleTarget<Bitmap>(100, 100) {
                                    @Override
                                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                                        String text = "";
                                        try {
                                            if (item.has("text")) {
                                                text = item.getString("text");
                                            }
                                        } catch (Exception e) {
                                            Log.d("Error", e.toString());
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

                    }
                }

            }
            bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
                @Override
                public boolean onTabSelected(int position, boolean wasSelected) {
                    try {
                        JSONObject item = items.getJSONObject(position);
                        if(item.has("url")) {
                            String url = JasonHelper.url(item.getString("image"), JasonViewActivity.this);
                            JSONObject action = new JSONObject();
                            JSONObject options = new JSONObject();
                            options.put("url", url);
                            options.put("transition", "replace");
                            action.put("options", options);
                            href(action, new JSONObject(), JasonViewActivity.this);
                        }
                    } catch (Exception e) {
                        Log.d("Error", e.toString());
                    }
                    return true;
                }
            });

            listView.setClipToPadding(false);
            listView.setPadding(0,0,0,160);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }


    private void setup_layers(JSONArray layers){
        try{
            if(layer_items != null) {
                for (int j = 0; j < layer_items.size(); j++) {
                    View layerView = layer_items.get(j);
                    rootLayout.removeView(layerView);
                }
            }
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
        } catch (Exception e) {
            Log.d("Error", e.toString());
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
            Log.d("Error", e.toString());
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

                if (header.has("menu")) {
                    JSONObject json = header.getJSONObject("menu");

                    final MenuItem item = menu.add("Menu");
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                    // We're going to create a button.
                    json.put("type", "button");

                    // if it's an image button, both url and image should work
                    if(json.has("image")) {
                        json.put("url", JasonHelper.url(json.getString("image"), JasonViewActivity.this));
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

                    // Set padding for the menu button
                    int padding = (int)JasonHelper.pixels(this, "10", "vertical");
                    itemView.setPadding(padding, 0, padding, 0);


                    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            try {
                                JSONObject header = model.rendered.getJSONObject("header");
                                if (header.has("menu")) {
                                    if (header.getJSONObject("menu").has("action")) {
                                        call(header.getJSONObject("menu").getJSONObject("action").toString(), new JSONObject().toString(), JasonViewActivity.this);
                                    } else if (header.getJSONObject("menu").has("href")) {
                                        JSONObject action = new JSONObject().put("type", "$href").put("options", header.getJSONObject("menu").getJSONObject("href"));
                                        call(action.toString(), new JSONObject().toString(), JasonViewActivity.this);
                                    }
                                }
                            } catch (Exception e) {
                                Log.d("Error", e.toString());
                            }
                            return true;
                        }
                    });
                }

            }
        }catch(Exception e){
            Log.d("Error", e.toString());
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
                        String url = JasonHelper.url(((JSONObject) title).getString("url"), JasonViewActivity.this);
                        int height = header_height;
                        int width = Toolbar.LayoutParams.WRAP_CONTENT;
                        if (((JSONObject) title).has("style")) {
                            JSONObject style = ((JSONObject) title).getJSONObject("style");
                            if (style.has("height")) {
                                try {
                                    height = (int) JasonHelper.pixels(this, style.getString("height"), "vertical");
                                } catch (Exception e) {
                                    Log.d("Error", e.toString());
                                }
                            }
                            if (style.has("width")) {
                                try {
                                    width = (int) JasonHelper.pixels(this, style.getString("width"), "horizontal");
                                } catch (Exception e) {
                                    Log.d("Error", e.toString());
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
                                .load(url)
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
            Log.d("Error", e.toString());
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
            Log.d("Error", e.toString());
        }
    }

}
