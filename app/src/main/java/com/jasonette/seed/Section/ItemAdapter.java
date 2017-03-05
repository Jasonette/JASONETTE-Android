package com.jasonette.seed.Section;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.jasonette.seed.Component.JasonComponentFactory;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Core.JasonViewActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/********************************************************
 *
 * Here's the hierarchy:
 *
 *  - ViewHolder
 *      - ContentView
 *          - Layout
 *              - Component
 *
 ********************************************************/



public class ItemAdapter extends RecyclerView.Adapter <ItemAdapter.ViewHolder>{
    public static final int DATA = 0;


    Context context;
    Context root_context;
    ArrayList<JSONObject> items;
    ArrayList<JSONObject> cloned_items;
    Map<String, Integer> signature_to_type = new HashMap<String,Integer>();
    Map<Integer, String> type_to_signature = new HashMap<Integer, String>();
    ViewHolderFactory factory = new ViewHolderFactory();


    /********************************************************
     *
     * Root level RecyclerView/ViewHolder logic
     *
     ********************************************************/

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ArrayList<View> subviews;
        String type;
        public ViewHolder(View itemView){
            super(itemView);
            this.subviews = new ArrayList<View>();
            itemView.setOnClickListener(this);
            this.type = "item";
        }
        public View getView(){
            return this.itemView;
        }
        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            JSONObject item = (JSONObject)view.getTag();
            try {
                if (item.has("action")) {
                    JSONObject action = item.getJSONObject("action");
                    ((JasonViewActivity)root_context).call(action.toString(), new JSONObject().toString(), "{}", view.getContext());
                } else if (item.has("href")){
                    JSONObject href = item.getJSONObject("href");
                    JSONObject action = new JSONObject().put("type", "$href").put("options", href);
                    ((JasonViewActivity)root_context).call(action.toString(), new JSONObject().toString(), "{}", view.getContext());
                }
            } catch (Exception e){ }
        }

    }


    public ItemAdapter(Context root_context, Context context, ArrayList<JSONObject> items) {
        this.items = items;
        this.cloned_items = new ArrayList<JSONObject>();
        this.cloned_items.addAll(items);
        this.context = context;
        this.root_context = root_context;
    }

    public void filter(String text) {
        this.items.clear();
        if(text.isEmpty()){
            this.items.addAll(this.cloned_items);
        } else{
            text = text.toLowerCase();
            for(JSONObject item: this.cloned_items){
                if(item.toString().toLowerCase().contains(text)){
                    this.items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    // For determining the view type.
    // 1. Generate a signature using the JSON markup and assign it to signature_to_type.
    // 2. If the signature already exists, return the type.
    public int getItemViewType(int position) {

        JSONObject item = this.items.get(position);

        // if the key starts with "horizontal_section",
        // we deal with it in a special manner.
        // Assuming that all items for a horizontal section will have the same prototype,
        // we can generate the signature from just one of its items.

        String stringified_item;
        if(item.has("horizontal_section")){
            try {
                JSONArray horizontal_section_items = item.getJSONArray("horizontal_section");
                // assuming that the section would contain at least one item,
                // we will take the first item from the section and generate the signature
                JSONObject first_item = horizontal_section_items.getJSONObject(0);
                stringified_item = "[" + first_item.toString() + "]";
            } catch (Exception e) {
                stringified_item = item.toString();
            }
        } else {
            stringified_item = item.toString();
        }


        // Simplistic way of transforming an item JSON into a generic string, by replacing out all non-structural values
        // - replace out text and url
        String regex = "\"(url|text)\"[ ]*:[ ]*\"([^\"]+)\"";
        String signature = stringified_item.replaceAll(regex, "\"jason\":\"jason\"");
        // - replace out 'title' and 'description'
        regex = "\"(title|description)\"[ ]*:[ ]*\"([^\"]+)\"";
        signature = signature.replaceAll(regex, "\"jason\":\"jason\"");

        if(signature_to_type.containsKey(signature)){
            // if the signature exists, get the type using the signature
            return signature_to_type.get(signature);
        } else {
            // If it's a new signature, set the mapping between jason and type, both ways

            // Increment the index (new type) first.
            int index = signature_to_type.size();

            // 1. jason => type: assign that index as the type for the signature
            signature_to_type.put(signature, index);

            // 2. type => jason: assign the stringified item so it can be used later
            //  Need to use the original instance instead of the stubbed out "signature" since some components requre url or text attributes to instantiate (create)
            type_to_signature.put(index, stringified_item);
            //type_to_signature.put(index, signature);

            // Return the new index;
            return index;
        }

    }

    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        String signatureString = type_to_signature.get(new Integer(viewType));
        ItemAdapter.ViewHolder viewHolder;

        if (signatureString.startsWith("[")) {
            // Horizontal Section => Build a ViewHolder with a horizontally scrolling RecyclerView

            // 1. Create RecyclerView
            RecyclerView horizontalListView = new RecyclerView(parent.getContext());
            horizontalListView.setLayoutManager(new LinearLayoutManager(horizontalListView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            horizontalListView.setNestedScrollingEnabled(false);

            // 2. Create Adapter
            ItemAdapter horizontal_adapter = new ItemAdapter(context, horizontalListView.getContext(), new ArrayList<JSONObject>());

            // 3. Connect RecyclerView with Adapter
            horizontalListView.setAdapter(horizontal_adapter);

            // 4. Instantiate a new ViewHolder with the RecyclerView
            viewHolder = new ViewHolder(horizontalListView);

        } else {
            // Vertcial Section => Regular ViewHolder

            JSONObject json;
            try {
                json = new JSONObject(signatureString);
            } catch (JSONException e) {
                json = new JSONObject();
            }
            viewHolder = factory.build(null, json);
        }

        return viewHolder;

    }

    @Override
    public void onBindViewHolder(ItemAdapter.ViewHolder viewHolder, int position) {
        JSONObject json = this.items.get(position);
        if(json.has("horizontal_section")) {
            // Horizontal Section
            // In this case, the viewHolder is a Recyclerview.

            // We fetch the recyclerview from the viewholder (the viewholder's itemView is the recyclerview)
            ItemAdapter horizontalListAdapter = ((ItemAdapter) ((RecyclerView)viewHolder.itemView).getAdapter());

            // Transform JasonArray into ArrayList
            try {
                horizontalListAdapter.items = JasonHelper.toArrayList(((JSONArray)json.getJSONArray("horizontal_section")));
            } catch (Exception e) { }

            // Update viewholder
            horizontalListAdapter.notifyDataSetChanged();
            viewHolder.itemView.invalidate();
        } else {
            // Vertical section
            // Build ViewHolder via ViewHolderFactory
            factory.build(viewHolder, json);
        }
    }

    @Override
    public int getItemCount(){
        return this.items.size();
    }

    /********************************************************
     *
     * ViewHolderFactory => Creates ViewHolders
     *
     ********************************************************/

    public class ViewHolderFactory {

        // "subviews" =>
        //
        //      store the DOM tree under viewHolder, so that it can be accessed easily inside onBindViewHolder, for example:
        //      viewHolder.subviews = [Image1, TextView1, Image2, TextView2, TextView3, TextView4];
        //      for(int i = 0 ; i < viewHolder.subviews.size() ; i++){
        //          View el = viewHolder.subviews.get(i);
        //          if(el instancof Button){
        //              ..
        //          } ..
        //      }

        private ArrayList<View> subviews;
        private Boolean exists;
        private int index;

        public ItemAdapter.ViewHolder build(ViewHolder prototype, JSONObject json) {

            LinearLayout layout;


            if (prototype != null) {
                // Fill
                this.exists = true;
            } else {
                this.exists = false;
            }

            if (this.exists) {
                // Fill

                // Get the subviews
                this.subviews = prototype.subviews;
                this.index = 0;

                // Build content view with the existing prototype layout
                layout = (LinearLayout) prototype.getView();
                buildContentView(layout, json);

                layout.setTag(json);

                // return the existing prototype layout
                return prototype;
            } else {
                // Create

                // Initialize subviews
                this.subviews = new ArrayList<View>();

                // Build content view with a new layout
                layout = buildContentView(new LinearLayout(context), json);

                // Create a new viewholder with the new layout
                ItemAdapter.ViewHolder viewHolder = new ItemAdapter.ViewHolder(layout);

                // Assign subviews
                viewHolder.subviews = this.subviews;

                return viewHolder;
            }

        }

        // ContentView is the top level view of a cell.
        // It's always a layout.
        // If the JSON supplies a component, ContentView creates a layout wrapper around it
        private LinearLayout buildContentView(LinearLayout layout, JSONObject json) {
            try {
                if (json.has("type")) {
                    String type = json.getString("type");
                    if (type.equalsIgnoreCase("vertical") || type.equalsIgnoreCase("horizontal")) {
                        layout = buildLayout(layout, json, null, 0);
                        layout.setClickable(true);
                    } else {
                        // 1. Create components array
                        JSONArray components = new JSONArray();


                        // 2. Create a vertical layout and set its components
                        JSONObject wrapper = new JSONObject();
                        wrapper.put("type", "vertical");

                        // When wrapping, we set the padding on the wrapper to 0, since it will be taken care of on the component
                        JSONObject style = new JSONObject();
                        style.put("padding", type.equalsIgnoreCase("html") ? 1 : 0);
                        wrapper.put("style", style);

                        // Instead, we set the component's padding to 10
                        JSONObject componentStyle;
                        if(json.has("style")) {
                            componentStyle = json.getJSONObject("style");
                            if(!componentStyle.has("padding")){
                                componentStyle.put("padding", "10");
                            }
                        } else {
                            componentStyle = new JSONObject();
                            componentStyle.put("padding", "10");
                        }
                        json.put("style", componentStyle);

                        // Setup components array
                        components.put(json);
                        wrapper.put("components", components);

                        // Setup href and actions
                        if (json.has("href")) {
                            wrapper.put("href", json.getJSONObject("href"));
                        }
                        if (json.has("action")) {
                            wrapper.put("action", json.getJSONObject("action"));
                        }

                        // 3. Start running the layout logic
                        buildLayout(layout, wrapper, null, 0);

                        // In case we're at the root level
                        // and the child has a width, we need to set the wrapper's width to wrap its child. (for horizontal scrolling sections)
                        View componentView = layout.getChildAt(0);
                        ViewGroup.LayoutParams componentLayoutParams = (ViewGroup.LayoutParams)componentView.getLayoutParams();
                        if(componentLayoutParams.width > 0){
                            ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams)layout.getLayoutParams();
                            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        }

                    }
                } else {
                    layout = new LinearLayout(context);
                }
            } catch (JSONException e) {
                layout = new LinearLayout(context);
            }

            return layout;

        }


        public LinearLayout buildLayout(LinearLayout layout, JSONObject item, JSONObject parent, int level) {

            if (exists) {
                try {
                    JSONArray components = item.getJSONArray("components");
                    for (int i = 0; i < components.length(); i++) {
                        JSONObject component = components.getJSONObject(i);
                        if (component.getString("type").equalsIgnoreCase("vertical") || component.getString("type").equalsIgnoreCase("horizontal")) {
                            LinearLayout childLayout = (LinearLayout)layout.getChildAt(i);
                            buildLayout(childLayout, component, item, ++level);
                            if (i > 0) {
                                add_spacing(childLayout, item, item.getString("type"));
                            }
                        } else {
                            View child_component = buildComponent(component, item);
                            if (i > 0) {
                                add_spacing(child_component, item, item.getString("type"));
                            }
                        }
                    }
                } catch (JSONException e) {

                }
                return new LinearLayout(context);
            } else {
                try {
                    // Layout styling
                    String type = item.getString("type");
                    JSONObject style = JasonHelper.style(item, root_context);

                    JSONArray components;
                    if (type.equalsIgnoreCase("vertical") || type.equalsIgnoreCase("horizontal")) {
                        components = item.getJSONArray("components");
                    } else {
                        components = new JSONArray();
                    }

                    LinearLayout.LayoutParams layoutParams;
                    if (type.equalsIgnoreCase("vertical")) {
                        // vertical layout
                        layout.setOrientation(LinearLayout.VERTICAL);
                        components = item.getJSONArray("components");
                    } else if (type.equalsIgnoreCase("horizontal")) {
                        // horizontal layout
                        layout.setOrientation(LinearLayout.HORIZONTAL);
                        components = item.getJSONArray("components");

                    }

                    // set width and height
                    layoutParams = JasonLayout.autolayout(parent, item, root_context);

                    layout.setLayoutParams(layoutParams);

                    // Padding
                    // If root level, set the default padding to 10
                    String default_padding;
                    if (level == 0) {
                        default_padding = "10";
                    } else {
                        default_padding = "0";
                    }
                    int padding_left = (int) JasonHelper.pixels(root_context, default_padding, type);
                    int padding_right = (int) JasonHelper.pixels(root_context, default_padding, type);
                    int padding_top = (int) JasonHelper.pixels(root_context, default_padding, type);
                    int padding_bottom = (int) JasonHelper.pixels(root_context, default_padding, type);
                    if (style.has("padding")) {
                        padding_left = (int) JasonHelper.pixels(root_context, style.getString("padding"), type);
                        padding_right = padding_left;
                        padding_top = padding_left;
                        padding_bottom = padding_left;
                    }
                    if (style.has("padding_left")) {
                        padding_left = (int) JasonHelper.pixels(root_context, style.getString("padding_left"), type);
                    }
                    if (style.has("padding_right")) {
                        padding_right = (int) JasonHelper.pixels(context, style.getString("padding_right"), type);
                    }
                    if (style.has("padding_top")) {
                        padding_top = (int) JasonHelper.pixels(root_context, style.getString("padding_top"), type);
                    }
                    if (style.has("padding_bottom")) {
                        padding_bottom = (int) JasonHelper.pixels(root_context, style.getString("padding_bottom"), type);
                    }
                    layout.setPadding(padding_left, padding_top, padding_right, padding_bottom);


                    // spacing
                    for (int i = 0; i < components.length(); i++) {
                        JSONObject component = components.getJSONObject(i);
                        String component_type = component.getString("type");
                        if (component_type.equalsIgnoreCase("vertical") || component_type.equalsIgnoreCase("horizontal")) {
                            // the child is also a layout
                            LinearLayout child_layout = buildLayout(new LinearLayout(context), component, item, ++level);
                            layout.addView(child_layout);
                            if (i > 0) {
                                add_spacing(child_layout, item, type);
                            }
                            // From item1, start adding margin-top (item0 shouldn't have margin-top)
                        } else {
                            View child_component = buildComponent(component, item);
                            // the child is a leaf node
                            layout.addView(child_component);
                            if (i > 0) {
                                add_spacing(child_component, item, type);
                            }
                        }
                    }

                    // align
                    if (style.has("align")) {
                        if (style.getString("align").equalsIgnoreCase("center")) {
                            layout.setGravity(Gravity.CENTER);
                        } else if (style.getString("align").equalsIgnoreCase("right")) {
                            layout.setGravity(Gravity.RIGHT);
                        } else {
                            layout.setGravity(Gravity.LEFT);
                        }
                    }

                    // background
                    if (style.has("background")) {
                        layout.setBackgroundColor(JasonHelper.parse_color(style.getString("background")));
                    }

                    layout.requestLayout();

                } catch (JSONException e) {

                }
                return layout;
            }
        }

        public View buildComponent(JSONObject component, JSONObject parent) {

            View view;

            JSONObject style = JasonHelper.style(component, root_context);

            if (exists) {
                view = (View) this.subviews.get(this.index++);
                JasonComponentFactory.build(view, component, parent, root_context);
                return view;
            } else {
                view = JasonComponentFactory.build(null, component, parent, root_context);
                view.setId(this.subviews.size());
                this.subviews.add(view);
                return view;
            }


        }

        private void add_spacing(View view, JSONObject item, String type) {
            try {
                String spacing = "0";
                JSONObject style = JasonHelper.style(item, root_context);
                if (style.has("spacing")) {
                    spacing = style.getString("spacing");
                } else {
                    spacing = "0";
                }

                if (type.equalsIgnoreCase("vertical")) {
                    int m = (int) JasonHelper.pixels(context, spacing, item.getString("type"));
                    LinearLayout.LayoutParams layoutParams;
                    if(view.getLayoutParams() == null) {
                        layoutParams = new LinearLayout.LayoutParams(0,0);
                    } else {
                        layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
                    }
                    layoutParams.topMargin = m;
                    layoutParams.bottomMargin = 0;
                    view.setLayoutParams(layoutParams);
                } else if (type.equalsIgnoreCase("horizontal")) {
                    int m = (int) JasonHelper.pixels(root_context, spacing, item.getString("type"));
                    LinearLayout.LayoutParams layoutParams;
                    if(view.getLayoutParams() == null) {
                        layoutParams = new LinearLayout.LayoutParams(0,0);
                    } else {
                        layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
                    }
                    layoutParams.leftMargin = m;
                    layoutParams.rightMargin = 0;
                    view.setLayoutParams(layoutParams);
                }
                view.requestLayout();
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
        }

    }

}