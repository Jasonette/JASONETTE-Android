package com.jasonette.seed.Component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import org.json.JSONObject;

public class JasonSpaceComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, Context context) {
        if(view == null) {
            return new View(context);
        } else {
            try {
                JasonComponent.build(view, component, parent, context);
                view.requestLayout();
                return view;
            } catch (Exception e) {
                Log.d("Error", e.toString());
                return new View(context);
            }
        }
    }
}
