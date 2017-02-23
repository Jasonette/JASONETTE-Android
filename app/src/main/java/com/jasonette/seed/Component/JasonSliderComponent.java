package com.jasonette.seed.Component;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;

public class JasonSliderComponent {
    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null) {
            return new SeekBar(context);
        } else {
            try {
                view = JasonComponent.build(view, component, parent, context);
                SeekBar seekBar = ((SeekBar) view);
                if(component.has("name")){
                    String val = "0.5";
                    if(((JasonViewActivity) context).model.var.has(component.getString("name"))){
                        val = ((JasonViewActivity) context).model.var.getString(component.getString("name"));
                    } else {
                        // default value
                        if(component.has("value")){
                            val = component.getString("value");
                        }
                    }
                    seekBar.setProgress((int)Math.round(Double.parseDouble(val)*100.0));
                    JasonSliderComponent.addListener(seekBar, context);
                }
                JSONObject style = JasonHelper.style(component, context);
                if (style.has("color")) {
                    int color = JasonHelper.parse_color(style.getString("color"));
                    seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                } else {
                    // maybe it's not necessary
                    seekBar.getProgressDrawable().clearColorFilter();
                    // it's necessary
                    seekBar.getThumb().clearColorFilter();
                }
                view.requestLayout();
                return view;
            } catch (Exception e){
                Log.d("Error", e.toString());
                return new View(context);
            }
        }
    }

    public static void addListener(final SeekBar view, final Context root_context){
        SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                JSONObject component = (JSONObject)seekBar.getTag();
                try {
                    // don't work with int if progress == 0
                    String progress = Double.toString(seekBar.getProgress()/100.0);
                    ((JasonViewActivity) root_context).model.var.put(component.getString("name"), progress);
                    if (component.has("action")) {
                        JSONObject action = component.getJSONObject("action");
                        ((JasonViewActivity) root_context).call(action.toString(), new JSONObject().toString(), "{}", view.getContext());
                    }
                } catch (Exception e) {
                    Log.d("Error", e.toString());
                }
            }
        };
        view.setOnSeekBarChangeListener(seekListener);
    }
}
