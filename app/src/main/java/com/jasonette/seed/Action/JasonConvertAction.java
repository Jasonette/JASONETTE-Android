package com.jasonette.seed.Action;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Core.JasonParser;
import com.jasonette.seed.Helper.JasonHelper;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.json.JSONObject;

public class JasonConvertAction {
    public void csv(final JSONObject action, final JSONObject data, final Context context){
        try{
            final JSONObject options = action.getJSONObject("options");
            String result = "[]";
            if(options.has("data")) {
                String csv_data = options.getString("data");
                if (!csv_data.isEmpty()) {
                    String js = JasonHelper.read_file("csv", context);
                    V8 runtime = V8.createV8Runtime();
                    runtime.executeVoidScript(js);
                    V8Object csv = runtime.getObject("csv");
                    V8Array parameters = new V8Array(runtime).push(csv_data.toString());
                    V8Array val = csv.executeArrayFunction("run", parameters);
                    parameters.release();
                    csv.release();

                    parameters = new V8Array(runtime).push(val);
                    V8Object json = runtime.getObject("JSON");
                    result = json.executeStringFunction("stringify", parameters);
                    parameters.release();
                    val.release();
                    json.release();

                    runtime.release();
                }
            }
            JasonHelper.next("success", action, result, context);
        } catch (Exception e){
            try {
                JSONObject error = new JSONObject();
                error.put("data", e.toString());
                JasonHelper.next("error", action, error, context);
            } catch (Exception err){
                Log.d("Error", err.toString());
            }
        }
    }
}
