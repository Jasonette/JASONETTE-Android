package com.jasonette.seed.Component;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class JasonComponentFactory {
    Map<String, Integer> signature_to_type = new HashMap<String,Integer>();
    public static View build(View prototype, final JSONObject component, final JSONObject parent, final Context context) {
        try{
            String type;
            type = component.getString("type");

            View view;
            if(type.equalsIgnoreCase("label")){
                view = JasonLabelComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("image")) {
                view = JasonImageComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("button")) {
                view = JasonButtonComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("space")) {
                view = JasonSpaceComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("textfield")) {
                view = JasonTextfieldComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("textarea")) {
                view = JasonTextareaComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("html")) {
                view = JasonHtmlComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("map")) {
                view = JasonMapComponent.build(prototype, component, parent, context);
            } else if(type.equalsIgnoreCase("slider")) {
                view = JasonSliderComponent.build(prototype, component, parent, context);
            } else {
                // Non-existent component warning
                JSONObject error_component = new JSONObject(component.toString());
                error_component.put("type", "label");
                error_component.put("text", "$"+component.getString("type")+"\n(not implemented yet)");
                view = JasonLabelComponent.build(prototype, error_component, parent, context);
                ((TextView)view).setGravity(Gravity.CENTER);
            }
            return view;

        }
        catch (Exception e){
            Log.d("Error", e.toString());
        }

        return new View(context);
    }
}
