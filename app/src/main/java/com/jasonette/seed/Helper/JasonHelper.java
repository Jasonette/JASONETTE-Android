package com.jasonette.seed.Helper;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.TextView;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

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
        } catch (Exception e){
            Timber.w(e);
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
        } catch (Exception e) {
            Timber.w(e);
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
            Timber.w(e);
            return new JSONObject();
        }
    }

    public static void next(String type, JSONObject action, Object data, final JSONObject event, Context context) {
        try {
            if (action.has(type)) {
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
            Timber.e(e);
        }
    }

    /**
     * Parse a string as either a JSON Object or Array.
     *
     * @param json
     * @return a JSONObject or JSONArray based on the Json string,
     * return an emptry JSONObject if json param is null or on parsing supplied json string.
     */
    public static Object objectify(String json) {
        try {
            if (json == null) {
                return new JSONObject();
            }
            if (json.trim().startsWith("[")) {
                // JSONArray
                return new JSONArray(json);
            } else if (json.trim().startsWith("{")) {
                return new JSONObject(json);
            } else {
                return new JSONObject();
            }
        } catch (Exception e) {
            Timber.w(e, "error objectifying: %s", json);
            return new JSONObject();
        }
    }


    public static ArrayList<JSONObject> toArrayList(JSONArray jsonArray) {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getJSONObject(i));
            }
        } catch (Exception e) {
            Timber.w(e);
        }
        return list;
    }

    public static float ratio(String ratio) {
        String regex = "^[ ]*([0-9]+)[ ]*[:/][ ]*([0-9]+)[ ]*$";
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(ratio);
        if (m.matches()) {
            Float w = Float.parseFloat(m.group(1));
            Float h = Float.parseFloat(m.group(2));
            return w/h;
        } else {
            return Float.parseFloat(ratio);
        }
    }

    public static float pixels(Context context, String size, String direction) {
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
            if (direction.equalsIgnoreCase("vertical")) {
                int full = displayMetrics.heightPixels;
                percent_height = full * percentage / 100;
                s = percent_height;
            } else {
                int full = displayMetrics.widthPixels;
                percent_width = full * percentage / 100;
                s = percent_width;
            }

            if (sign.equalsIgnoreCase("+")) {
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
            if (m.matches()) {
                Float percentage = Float.parseFloat(m.group(1));
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
                if (direction.equalsIgnoreCase("vertical")) {
                    int full = displayMetrics.heightPixels;
                    s = full * percentage / 100;
                } else {
                    int full = displayMetrics.widthPixels;
                    s = full * percentage / 100;
                }
                return s;
            } else {
                s = Float.parseFloat(size);
                return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, s, context.getResources().getDisplayMetrics());
            }
        }
    }

    public static int parse_color(String color_string) {
        Pattern rgb = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Pattern rgba = Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9.]+) *\\)");
        Matcher rgba_m = rgba.matcher(color_string);
        Matcher rgb_m = rgb.matcher(color_string);
        if (rgba_m.matches()) {
            float a = Float.valueOf(rgba_m.group(4));
            int alpha = (int) Math.round(a * 255);
            String hex = Integer.toHexString(alpha).toUpperCase();
            if (hex.length() == 1) hex = "0" + hex;
            hex = "0000" + hex;
            return Color.argb(Integer.parseInt(hex, 16), Integer.valueOf(rgba_m.group(1)), Integer.valueOf(rgba_m.group(2)), Integer.valueOf(rgba_m.group(3)));
        } else if (rgb_m.matches()) {
            return Color.rgb(Integer.valueOf(rgb_m.group(1)), Integer.valueOf(rgb_m.group(2)), Integer.valueOf(rgb_m.group(3)));
        } else {
            // Otherwise assume hex code
            return Color.parseColor(color_string);
        }
    }

    public static Typeface get_font(String font, Context context) {
        Typeface font_type = Typeface.createFromAsset(context.getAssets(), "fonts/" + font + ".ttf");
        return font_type;
    }

    public static String read_file_scheme(String filename, Context context) throws IOException {
        filename = filename.replace("file://", "file/");
        return read_file(filename, context);
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

    public static Object read_json(String fn, Context context) {// throws IOException {

        // we're expecting a filename that looks like "file://..."
        String filename = fn.replace("file://", "file/");

        String jr = null;
        Object ret;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jr = new String(buffer, "UTF-8");


            if(jr.trim().startsWith("[")) {
                // array
                ret = new JSONArray(jr);
            } else if(jr.trim().startsWith("{")){
                // object
                ret = new JSONObject(jr);
            } else {
                // string
                ret = jr;
            }
        } catch (Exception e) {
            Timber.w(e);
            return new JSONObject();
        }
        return ret;

    }

    public static void permission_exception(String actionName, Context context) {
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
            Timber.w(e);
        }
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }


    // dispatchIntent method
    // 1. triggers an external Intent
    // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns
    // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
    public static void dispatchIntent(String name, JSONObject action, JSONObject data, JSONObject event, Context context, Intent intent, JSONObject handler) {
        // Generate unique identifier for return value
        // This will be used to name the handlers
        int requestCode;
        try {
            requestCode = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            requestCode = -1;
        }

        try {
            // handler looks like this:
            /*
                  {
                    "class": [class name],
                    "method": [method name],
                    "options": {
                        [options to preserve]
                    }
                  }
             */

            JSONObject options = new JSONObject();
            options.put("action", action);
            options.put("data", data);
            options.put("event", event);
            options.put("context", context);
            handler.put("options", options);

            ((Launcher) ((JasonViewActivity) context).getApplicationContext()).once(name, handler);
        } catch (Exception e) {
            Timber.w(e);
        }

        if (intent != null) {
            // Start the activity
            ((JasonViewActivity) context).startActivityForResult(intent, requestCode);
        } else {
            // if intent is null,
            // it means we are manually going to deal with opening a new Intent
        }

    }

    public static void dispatchIntent(JSONObject action, JSONObject data, JSONObject event, Context context, Intent intent, JSONObject handler) {
        dispatchIntent(String.valueOf((int) (System.currentTimeMillis() % 10000)), action, data, event, context, intent, handler);
    }

    public static void callback(JSONObject callback, String result, Context context) {
        ((Launcher) context.getApplicationContext()).callback(callback, result, (JasonViewActivity) context);
    }

    public static JSONObject preserve(JSONObject callback, JSONObject action, JSONObject data, JSONObject event, Context context) {
        try {
            JSONObject callback_options = new JSONObject();
            callback_options.put("action", action);
            callback_options.put("data", data);
            callback_options.put("event", event);
            callback_options.put("context", context);
            callback.put("options", callback_options);
            return callback;
        } catch (Exception e) {
            Timber.e(e, "wasn't able to preserve stack for action: %s", action);
            return callback;
        }
    }

    public static void setTextViewFont(TextView view, JSONObject style, Context context) {
        try {
            if (style.has("font:android")) {
                String f = style.getString("font:android");
                if (f.equalsIgnoreCase("bold")) {
                    view.setTypeface(Typeface.DEFAULT_BOLD);
                } else if (f.equalsIgnoreCase("sans")) {
                    view.setTypeface(Typeface.SANS_SERIF);
                } else if (f.equalsIgnoreCase("serif")) {
                    view.setTypeface(Typeface.SERIF);
                } else if (f.equalsIgnoreCase("monospace")) {
                    view.setTypeface(Typeface.MONOSPACE);
                } else if (f.equalsIgnoreCase("default")) {
                    view.setTypeface(Typeface.DEFAULT);
                } else {
                    try {
                        Typeface font_type = Typeface.createFromAsset(context.getAssets(), "fonts/" + style.getString("font:android") + ".ttf");
                        view.setTypeface(font_type);
                    } catch (Exception e) {
                    }
                }
            } else if (style.has("font")) {
                if (style.getString("font").toLowerCase().contains("bold")) {
                    if (style.getString("font").toLowerCase().contains("italic")) {
                        view.setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
                    } else {
                        view.setTypeface(Typeface.DEFAULT_BOLD);
                    }
                } else {
                    if (style.getString("font").toLowerCase().contains("italic")) {
                        view.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    } else {
                        view.setTypeface(Typeface.DEFAULT);
                    }
                }
            }
        } catch (JSONException e) {}
    }
}
