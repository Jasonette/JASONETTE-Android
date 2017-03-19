package com.jasonette.seed.Helper;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;

import com.jasonette.seed.Core.JasonViewActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JasonHelper {
    public static JSONObject style(JSONObject component, Context root_context) {
        JSONObject style = new JSONObject();
        try {
            if (component.has("class")) {
                String style_class_string = component.getString("class");
                String[] style_classes = style_class_string.split("\\s+");
                for(int i = 0 ; i < style_classes.length ; i++){
                    JSONObject astyle = ((JasonViewActivity) root_context).model.jason.getJSONObject("$jason").getJSONObject("head").getJSONObject("styles").getJSONObject(style_classes[i]);
                    Iterator iterator = astyle.keys();
                    String style_key;
                    while (iterator.hasNext()) {
                        style_key = (String) iterator.next();
                        style.put(style_key, astyle.get(style_key));
                    }
                }
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }

        try {
            // iterate through inline style and overwrite
            if (component.has("style")) {
                JSONObject inline_style = component.getJSONObject("style");
                Iterator iterator = inline_style.keys();
                String style_key;
                while (iterator.hasNext()) {
                    style_key = (String) iterator.next();
                    style.put(style_key, inline_style.get(style_key));
                }
            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
        return style;
    }

    public static JSONObject merge(JSONObject old, JSONObject add) {
        try {
            JSONObject stub = new JSONObject(old.toString());
            Iterator<String> keysIterator = add.keys();
            while (keysIterator.hasNext()) {
                String key = (String) keysIterator.next();
                Object val = add.get(key);
                stub.put(key, val);
            }
            return stub;
        } catch (Exception e) {
            Log.d("Error", e.toString());
            return new JSONObject();
        }
    }

    public static void next(String type, JSONObject action, Object data, final JSONObject event, Context context){
        try {
            if(action.has(type)){
                Intent intent = new Intent(type);
                intent.putExtra("action", action.get(type).toString());
                intent.putExtra("data", data.toString());
                intent.putExtra("event", event.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                // Release everything and finish
                Intent intent = new Intent("call");
                JSONObject unlock_action = new JSONObject();
                unlock_action.put("type", "$unlock");

                intent.putExtra("action", unlock_action.toString());
                intent.putExtra("event", event.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    public static Object objectify(String json){
        try {
            if (json.trim().startsWith("[")) {
                // JSONArray
                return new JSONArray(json);
            } else if (json.trim().startsWith("{")) {
                return new JSONObject(json);
            } else {
                return new Object();
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
            return new Object();
        }
    }


    public static ArrayList<JSONObject> toArrayList(JSONArray jsonArray){
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        try {
            for (int i=0; i<jsonArray.length(); i++) {
                list.add(jsonArray.getJSONObject(i));
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
        return list;
    }

    public static float pixels(Context context, String size, String direction){
        String regex_percent_and_pixels = "^([0-9.]+)%[ ]*([+-]?)[ ]*([0-9]+)$";
        Pattern percent_pixels = Pattern.compile(regex_percent_and_pixels);
        Matcher m = percent_pixels.matcher(size);


        if (m.matches()) {
            Float percentage = Float.parseFloat(m.group(1));
            String sign = m.group(2);
            Float pixels = Float.parseFloat(m.group(3));
            pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels, context.getResources().getDisplayMetrics());

            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
            float percent_height;
            float percent_width;
            float s;
            if(direction.equalsIgnoreCase("vertical")){
                int full = displayMetrics.heightPixels;
                percent_height = full*percentage/100;
                s = percent_height;
            } else {
                int full = displayMetrics.widthPixels;
                percent_width = full*percentage/100;
                s = percent_width;
            }

            if(sign.equalsIgnoreCase("+")){
                s = s + pixels;
            } else {
                s = s - pixels;
            }

            return s;

        } else {
            String regex = "(\\d+)%";
            Pattern percent = Pattern.compile(regex);
            m = percent.matcher(size);
            float s;
            if(m.matches()){
                Float percentage = Float.parseFloat(m.group(1));
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
                if(direction.equalsIgnoreCase("vertical")){
                    int full = displayMetrics.heightPixels;
                    s = full*percentage/100;
                } else {
                    int full = displayMetrics.widthPixels;
                    s = full*percentage/100;
                }
                return s;
            } else {
                s = Float.parseFloat(size);
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, s, context.getResources().getDisplayMetrics());
            }
        }
    }
    public static int parse_color(String color_string){
        Pattern rgb = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Pattern rgba = Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9.]+) *\\)");
        Matcher rgba_m = rgba.matcher(color_string);
        Matcher rgb_m = rgb.matcher(color_string);
        if (rgba_m.matches()){
            float a = Float.valueOf(rgba_m.group(4));
            int alpha = (int) Math.round(a * 255);
            String hex = Integer.toHexString(alpha).toUpperCase();
            if (hex.length() == 1) hex = "0" + hex;
            hex = "0000" + hex;
            return Color.argb(Integer.parseInt(hex, 16), Integer.valueOf(rgba_m.group(1)), Integer.valueOf(rgba_m.group(2)), Integer.valueOf(rgba_m.group(3)));
        } else if(rgb_m.matches()){
            return Color.rgb(Integer.valueOf(rgb_m.group(1)), Integer.valueOf(rgb_m.group(2)), Integer.valueOf(rgb_m.group(3)));
        } else {
            // Otherwise assume hex code
            return Color.parseColor(color_string);
        }
    }
    public static Typeface get_font(String font, Context context){
        Typeface font_type = Typeface.createFromAsset(context.getAssets(),"fonts/"+font+".ttf");
        return font_type;
    }
    public static String read_file(String filename, Context context) throws IOException {
        AssetManager assets = context.getAssets();
        final InputStream inputStream = assets.open(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        final StringBuilder stringBuilder = new StringBuilder();
        boolean done = false;
        while (!done) {
            final String line = reader.readLine();
            done = (line == null);
            if (line != null) {
                stringBuilder.append("\n");
                stringBuilder.append(line);
            }
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }
    public static JSONObject read_json(String fn, Context context) throws IOException {

        // we're expecting a filename that looks like "file://..."
        String filename = fn.replace("file://", "file/");

        String jr = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jr = new String(buffer, "UTF-8");
            return new JSONObject(jr);
        } catch (Exception e) {
            Log.d("Error", e.toString());
            return new JSONObject();
        }

    }
    public static void permission_exception(String actionName, Context context){
        try {
            Intent intent = new Intent("call");
            JSONObject alert_action = new JSONObject();
            alert_action.put("type", "$util.alert");
            JSONObject options = new JSONObject();
            options.put("title", "Turn on Permissions");
            options.put("description", actionName + " requires additional permissions. Go to AndroidManifest.xml file and turn on the permission");
            alert_action.put("options", options);
            intent.putExtra("action", alert_action.toString());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }
}
