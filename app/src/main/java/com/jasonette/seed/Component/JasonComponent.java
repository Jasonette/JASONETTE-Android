package com.jasonette.seed.Component;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Section.JasonLayout;

import org.json.JSONObject;

public class JasonComponent {

    public static final String INTENT_ACTION_CALL = "call";
    public static final String ACTION_PROP = "action";
    public static final String DATA_PROP = "data";
    public static final String HREF_PROP = "href";
    public static final String TYPE_PROP = "type";
    public static final String OPTIONS_PROP = "options";

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context root_context) {
        float width = 0;
        float height = 0;
        int corner_radius = 0;

        view.setTag(component);
        JSONObject style = JasonHelper.style(component, root_context);

        try{
            if(parent == null) {
                // Layer type
                width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                if (style.has("height")) {
                    try {
                        height = (int) JasonHelper.pixels(root_context, style.getString("height"), "vertical");
                        if (style.has("ratio")) {
                            Float ratio = JasonHelper.ratio(style.getString("ratio"));
                            width = height * ratio;
                        }
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
                if (style.has("width")) {
                    try {
                        width = (int) JasonHelper.pixels(root_context, style.getString("width"), "horizontal");
                        if (style.has("ratio")) {
                            Float ratio = JasonHelper.ratio(style.getString("ratio"));
                            height = width / ratio;
                        }
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }

                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int)width, (int)height);
                view.setLayoutParams(layoutParams);
            } else {
                // Section item type
                LinearLayout.LayoutParams layoutParams = JasonLayout.autolayout(null, parent, component, root_context);
                view.setLayoutParams(layoutParams);
            }

            int bgcolor;
            if (style.has("background")) {
                int color = JasonHelper.parse_color(style.getString("background"));
                bgcolor = color;
                view.setBackgroundColor(color);
            } else {
                bgcolor = JasonHelper.parse_color("rgba(0,0,0,0)");
            }
            if(style.has("opacity"))
            {
                try {
                    float opacity = (float) style.getDouble("opacity");
                    view.setAlpha(opacity);
                }
                catch (Exception ex) {
                }
            }


            // padding
            int padding_left = (int)JasonHelper.pixels(root_context, "0", "horizontal");
            int padding_right = (int)JasonHelper.pixels(root_context, "0", "horizontal");
            int padding_top = (int)JasonHelper.pixels(root_context, "0", "horizontal");
            int padding_bottom = (int)JasonHelper.pixels(root_context, "0", "horizontal");
            if (style.has("padding")) {
                padding_left = (int)JasonHelper.pixels(root_context, style.getString("padding"), "horizontal");
                padding_right = padding_left;
                padding_top = padding_left;
                padding_bottom = padding_left;
            }

            // overwrite if more specific values exist
            if (style.has("padding_left")) {
                padding_left = (int)JasonHelper.pixels(root_context, style.getString("padding_left"), "horizontal");
            }
            if (style.has("padding_right")) {
                padding_right = (int)JasonHelper.pixels(root_context, style.getString("padding_right"), "horizontal");
            }
            if (style.has("padding_top")) {
                padding_top = (int)JasonHelper.pixels(root_context, style.getString("padding_top"), "vertical");
            }
            if (style.has("padding_bottom")) {
                padding_bottom = (int)JasonHelper.pixels(root_context, style.getString("padding_bottom"), "vertical");
            }

            if (style.has("corner_radius")) {
                float corner = JasonHelper.pixels(root_context, style.getString("corner_radius"), "horizontal");
                int color = ContextCompat.getColor(root_context, android.R.color.transparent);
                GradientDrawable cornerShape = new GradientDrawable();
                cornerShape.setShape(GradientDrawable.RECTANGLE);
                if (style.has("background")) {
                    color = JasonHelper.parse_color(style.getString("background"));
                }
                cornerShape.setColor(color);
                cornerShape.setCornerRadius(corner);

                // border + corner_radius handling
                if (style.has("border_width")){
                    int border_width = (int)JasonHelper.pixels(root_context, style.getString("border_width"), "horizontal");
                    if(border_width > 0){
                        int border_color;
                        if (style.has("border_color")){
                            border_color = JasonHelper.parse_color(style.getString("border_color"));
                        } else {
                            border_color = JasonHelper.parse_color("#000000");
                        }
                        cornerShape.setStroke(border_width, border_color);
                    }
                }
                cornerShape.invalidateSelf();
                view.setBackground(cornerShape);
            } else {
                // border handling (no corner radius)
                if (style.has("border_width")){
                    int border_width = (int)JasonHelper.pixels(root_context, style.getString("border_width"), "horizontal");
                    if(border_width > 0){
                        int border_color;
                        if (style.has("border_color")){
                            border_color = JasonHelper.parse_color(style.getString("border_color"));
                        } else {
                            border_color = JasonHelper.parse_color("#000000");
                        }
                        GradientDrawable cornerShape = new GradientDrawable();
                        cornerShape.setStroke(border_width, border_color);
                        cornerShape.setColor(bgcolor);
                        cornerShape.invalidateSelf();
                        view.setBackground(cornerShape);
                    }
                }

            }

            view.setPadding(padding_left, padding_top, padding_right, padding_bottom);
            return view;

        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return new View(root_context);
        }
    }
    public static void addListener(final View view, final Context root_context){
        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                JSONObject component = (JSONObject)v.getTag();
                try {
                    if (component.has("action")) {
                        JSONObject action = component.getJSONObject("action");
                        ((JasonViewActivity) root_context).call(action.toString(), new JSONObject().toString(), "{}", v.getContext());
                    } else if (component.has("href")) {
                        JSONObject href = component.getJSONObject("href");
                        JSONObject action = new JSONObject().put("type", "$href").put("options", href);
                        ((JasonViewActivity) root_context).call(action.toString(), new JSONObject().toString(), "{}",  v.getContext());
                    } else {
                        // NONE Explicitly stated.
                        // Need to bubble up all the way to the root viewholder.
                        View cursor = view;
                        while(cursor.getParent() != null) {
                            JSONObject item = (JSONObject)(((View)cursor.getParent()).getTag());
                            if (item!=null && (item.has("action") || item.has("href"))) {
                                ((View)cursor.getParent()).performClick();
                                break;
                            } else {
                                cursor = (View) cursor.getParent();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        };
        view.setOnClickListener(clickListener);
    }
}
