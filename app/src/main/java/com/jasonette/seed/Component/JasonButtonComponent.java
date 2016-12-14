package com.jasonette.seed.Component;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import org.json.JSONObject;

public class JasonButtonComponent{
    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(component.has("url")){
            // image button
            view = JasonImageComponent.build(view, component, parent, context);
        } else if(component.has("text")){
            // label button

            view = JasonLabelComponent.build(view, component, parent, context);

            // align center, vertically and horizontally,
            ((TextView)view).setGravity(Gravity.CENTER);

            // add padding by default
            int padding_top = view.getPaddingTop();
            int padding_left = view.getPaddingLeft();
            int padding_right = view.getPaddingRight();
            int padding_bottom = view.getPaddingBottom();
            if (padding_left == 0) padding_left = 15;
            if (padding_top == 0) padding_top = 15;
            if (padding_right == 0) padding_right = 15;
            if (padding_bottom == 0) padding_bottom = 15;
            view.setPadding(padding_left, padding_top, padding_right, padding_bottom);
        } else {
            // shouldn't happen
            if (view == null) {
                return new View(context);
            } else {
                return view;
            }
        }
        JasonComponent.addListener(view, context);

        return view;
    }
}
