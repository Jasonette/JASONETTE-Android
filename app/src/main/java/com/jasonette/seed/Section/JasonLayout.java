package com.jasonette.seed.Section;

import android.content.Context;
import android.widget.LinearLayout;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;

public class JasonLayout {
    public static LinearLayout.LayoutParams autolayout(JSONObject parent, JSONObject item, Context root_context) {

        int width = 0;
        int height = 0;
        int weight = 0;

        try {
            JSONObject style = JasonHelper.style(item, root_context);

            String item_type = item.getString("type");

            if (parent == null){
                // parent == null means: it's at the root level. Which can be:
                //  1. a layer item
                //  2. the root level of a section item
                if (style.has("width")) {
                    try {
                        width = (int) JasonHelper.pixels(root_context, style.getString("width"), "horizontal");
                    } catch (Exception e) { }
                } else {
                    width = LinearLayout.LayoutParams.MATCH_PARENT;
                }
                if (style.has("height")) {
                    try {
                        height = (int) JasonHelper.pixels(root_context, style.getString("height"), "vertical");
                    } catch (Exception e) {
                    }
                } else {
                    height = LinearLayout.LayoutParams.WRAP_CONTENT;
                }


            } else if (parent.getString("type").equalsIgnoreCase("vertical")) {

                if (style.has("height")) {
                    try {
                        height = (int) JasonHelper.pixels(root_context, style.getString("height"), "vertical");
                    } catch (Exception e) { }
                } else {
                    if(item_type.equalsIgnoreCase("vertical") || item_type.equalsIgnoreCase("horizontal") || item_type.equalsIgnoreCase("space")){
                        // layouts should have flexible height inside a vertical layout
                        height = 0;
                        weight = 1;
                    } else {
                        // components should stay as their intrinsic size
                        height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    }
                }

                if (style.has("width")) {
                    try {
                        width = (int) JasonHelper.pixels(root_context, style.getString("width"), "horizontal");
                    } catch (Exception e) { }
                } else {
                    // in case of vertical layout, all its children, regardless of whether they are layout or components,
                    // should have the width match parent
                    // (Except for images, which will be handled inside JasonImageComponent)
                    width = LinearLayout.LayoutParams.MATCH_PARENT;
                }



            } else if (parent.getString("type").equalsIgnoreCase("horizontal")) {
                if (style.has("width")) {
                    try {
                        width = (int) JasonHelper.pixels(root_context, style.getString("width"), "horizontal");
                    } catch (Exception e) {
                    }
                } else {
                    // in a horizontal layout, the child components shouldn't fight with width.
                    // All must be flexible width unless otherwise specified.
                    width = 0;
                    weight = 1;
                }
                if (style.has("height")) {
                    try {
                        height = (int) JasonHelper.pixels(root_context, style.getString("height"), "vertical");
                    } catch (Exception e) {
                    }
                } else {
                    height = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
            }

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
            if (weight > 0) {
                layoutParams.weight = weight;
            }


            return layoutParams;

        } catch (Exception e){
            return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }
}
