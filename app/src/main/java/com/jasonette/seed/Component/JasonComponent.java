package com.jasonette.seed.Component;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.R;
import com.jasonette.seed.Section.JasonLayout;

import org.json.JSONObject;

public class JasonComponent {
    public static View build(View view, final JSONObject component, final JSONObject parent, final Context root_context) {
        int width = 0;
        int height = 0;
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
                    } catch (Exception e) {
                    }
                }
                if (style.has("width")) {
                    try {
                        width = (int) JasonHelper.pixels(root_context, style.getString("width"), "horizontal");
                    } catch (Exception e) {
                    }
                }

                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
                view.setLayoutParams(layoutParams);
            } else {
                // Section item type
                LinearLayout.LayoutParams layoutParams = JasonLayout.autolayout(parent, component, root_context);
                view.setLayoutParams(layoutParams);
            }

            if (style.has("background")) {
                int color = JasonHelper.parse_color(style.getString("background"));
                view.setBackgroundColor(color);
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

            if (style.has("corner_radius")) {
                float corner = (float)style.getDouble("corner_radius");
                int color = ContextCompat.getColor(root_context, android.R.color.transparent);
                GradientDrawable cornerShape = new GradientDrawable();
                cornerShape.setShape(GradientDrawable.RECTANGLE);
                if (style.has("background")) {
                    color = JasonHelper.parse_color(style.getString("background"));
                }
                cornerShape.setColor(color);
                cornerShape.setCornerRadius(corner);
                cornerShape.invalidateSelf();
                view.setBackground(cornerShape);
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

            view.setPadding(padding_left, padding_top, padding_right, padding_bottom);
            return view;

        } catch (Exception e){
            Log.d("Error", e.toString());
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
                    Log.d("Error", e.toString());
                }
            }
        };
        view.setOnClickListener(clickListener);
    }
}
