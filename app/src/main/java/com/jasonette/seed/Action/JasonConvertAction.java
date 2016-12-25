package com.jasonette.seed.Action;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Core.JasonParser;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSFunction;
import org.liquidplayer.webkit.javascriptcore.JSON;
import org.liquidplayer.webkit.javascriptcore.JSObject;
import org.liquidplayer.webkit.javascriptcore.JSValue;

public class JasonConvertAction {
    public void csv(final JSONObject action, final JSONObject data, final Context context){
        try{
            final JSONObject options = action.getJSONObject("options");
            if(options.has("data")){
                String csv_data = options.getString("data");
                if(!csv_data.isEmpty()) {
                  String js = JasonHelper.read_file("csv", context);
                  JSContext jscontext = new JSContext();
                  jscontext.setExceptionHandler(JasonParser.getInstance());
                  jscontext.evaluateScript(js);
                  JSObject parser = jscontext.property("csv").toObject();
                  JSFunction csv = parser.property("run").toFunction();
                  JSValue val = csv.call(null, csv_data.toString(), JSON.parse(jscontext, data.toString()));

                  JasonHelper.next("success", action, val.toJSON(), context);
                }
            }
        } catch (Exception e){
            Log.d("Error", e.toString());
        }
    }
}
