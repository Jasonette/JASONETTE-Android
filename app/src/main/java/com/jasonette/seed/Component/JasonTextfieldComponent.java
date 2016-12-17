package com.jasonette.seed.Component;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Core.JasonViewActivity;
import org.json.JSONObject;

public class JasonTextfieldComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            return new EditText(context);
        } else {
            try {
                view = JasonComponent.build(view, component, parent, context);

                JSONObject style = JasonHelper.style(component, context);
                String type = component.getString("type");

                if (style.has("color")) {
                    int color = JasonHelper.parse_color(style.getString("color"));
                    ((TextView)view).setTextColor(color);
                }
                if (style.has("background")) {
                    int color = JasonHelper.parse_color(style.getString("background"));
                    ((TextView)view).setBackgroundColor(color);
                }



                if (style.has("align")) {
                    String align = style.getString("align");
                    if (align.equalsIgnoreCase("center")) {
                        ((EditText)view).setGravity(Gravity.CENTER_HORIZONTAL);
                    } else if (align.equalsIgnoreCase("right")) {
                        ((EditText)view).setGravity(Gravity.RIGHT);
                    } else {
                        ((EditText)view).setGravity(Gravity.LEFT);
                    }
                }

                if (style.has("size")) {
                    ((EditText)view).setTextSize(Float.parseFloat(style.getString("size")));
                }


                ((EditText)view).setSingleLine();
                ((EditText)view).setLines(1);


                ((EditText)view).setEllipsize(TextUtils.TruncateAt.END);



                // padding
                int padding_left = (int)JasonHelper.pixels(context, "10", "horizontal");
                int padding_right = (int)JasonHelper.pixels(context, "10", "horizontal");
                int padding_top = (int)JasonHelper.pixels(context, "10", "horizontal");
                int padding_bottom = (int)JasonHelper.pixels(context, "10", "horizontal");
                if (style.has("padding")) {
                    padding_left = (int)JasonHelper.pixels(context, style.getString("padding"), "horizontal");
                    padding_right = padding_left;
                    padding_top = padding_left;
                    padding_bottom = padding_left;
                }

                // overwrite if more specific values exist
                if (style.has("padding_left")) {
                    padding_left = (int)JasonHelper.pixels(context, style.getString("padding_left"), "horizontal");
                }
                if (style.has("padding_right")) {
                    padding_right = (int)JasonHelper.pixels(context, style.getString("padding_right"), "horizontal");
                }
                if (style.has("padding_top")) {
                    padding_top = (int)JasonHelper.pixels(context, style.getString("padding_top"), "vertical");
                }
                if (style.has("padding_bottom")) {
                    padding_bottom = (int)JasonHelper.pixels(context, style.getString("padding_bottom"), "vertical");
                }

                view.setPadding(padding_left, padding_top, padding_right, padding_bottom);


                // placeholder
                if(component.has("placeholder")){
                    ((EditText)view).setHint(component.getString("placeholder"));
                }

                // default value
                if(component.has("value")){
                    ((EditText)view).setText(component.getString("value"));
                }

                // Data binding
                if(component.has("name")){
                    ((EditText)view).addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            try {
                                ((JasonViewActivity) context).model.var.put(component.getString("name"), s.toString());
                                if(component.has("on")){
                                    JSONObject events = component.getJSONObject("on");
                                    if(events.has("change")){
                                        Intent intent = new Intent("call");
                                        intent.putExtra("action", events.getJSONObject("change").toString());
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                    }
                                }
                            } catch (Exception e){

                            }
                        }
                    });
                }
                ((EditText)view).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            try {
                                if(component.has("name")) {
                                    ((JasonViewActivity) context).model.var.put(component.getString("name"), v.getText().toString());
                                }
                            } catch (Exception e){

                            }
                        }

                        return false;
                    }
                });

                // The order is important => Must set the secure mode first and then set the font because Android sets the typeface to monospace by default when password mode
                if(style.has("secure")){
                    ((EditText)view).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ((EditText)view).setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                if (style.has("font:android")){
                    String f = style.getString("font:android");
                    if(f.equalsIgnoreCase("bold")){
                        ((TextView) view).setTypeface(Typeface.DEFAULT_BOLD);
                    } else if(f.equalsIgnoreCase("sans")){
                        ((TextView) view).setTypeface(Typeface.SANS_SERIF);
                    } else if(f.equalsIgnoreCase("serif")){
                        ((TextView) view).setTypeface(Typeface.SERIF);
                    } else if(f.equalsIgnoreCase("monospace")){
                        ((TextView) view).setTypeface(Typeface.MONOSPACE);
                    } else if(f.equalsIgnoreCase("default")){
                        ((TextView) view).setTypeface(Typeface.DEFAULT);
                    } else {
                        try {
                            Typeface font_type = Typeface.createFromAsset(context.getAssets(), "fonts/" + style.getString("font:android") + ".ttf");
                            ((TextView) view).setTypeface(font_type);
                        } catch (Exception e) {
                        }
                    }
                } else if (style.has("font")){
                    if(style.getString("font").toLowerCase().contains("bold")) {
                        if (style.getString("font").toLowerCase().contains("italic")) {
                            ((TextView) view).setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
                        } else {
                            ((TextView) view).setTypeface(Typeface.DEFAULT_BOLD);
                        }
                    } else {
                        if (style.getString("font").toLowerCase().contains("italic")) {
                            ((TextView) view).setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                        } else {
                            ((TextView) view).setTypeface(Typeface.DEFAULT);
                        }
                    }
                }

                view.requestLayout();
                return view;
            } catch (Exception e){
                Log.d("Error", e.toString());
                return new View(context);
            }
        }
    }
}
