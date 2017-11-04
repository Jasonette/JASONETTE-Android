package com.jasonette.seed.Action;

import android.content.Context;
import android.util.Log;

import com.jasonette.seed.Core.JasonParser;
import com.jasonette.seed.Core.JasonRequire;
import com.jasonette.seed.Helper.JasonHelper;
import com.jasonette.seed.Launcher.Launcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

/**
 * Created by e on 9/14/17.
 */

public class JasonScriptAction {
    public void include(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        try {
            JSONObject options = action.getJSONObject("options");
            if(options.has("items")) {
                JSONArray items = options.getJSONArray("items");
                JSONObject refs = new JSONObject();
                OkHttpClient client = ((Launcher)context.getApplicationContext()).getHttpClient(0);

                JSONArray urlItems = new JSONArray();
                JSONArray inlineItems = new JSONArray();
                for (int i = 0; i < items.length() ; i++) {
                    JSONObject item = (JSONObject) items.get(i);
                    if (item.has("url")) {
                        urlItems.put(item.getString("url"));
                    } else if (item.has("text")) {
                        inlineItems.put(item.getString("text"));
                    }
                }

                if (urlItems.length() > 0) {
                    CountDownLatch latch = new CountDownLatch(urlItems.length());
                    ExecutorService taskExecutor = Executors.newFixedThreadPool(urlItems.length());
                    for (int i = 0; i < urlItems.length() ; i++) {
                        String url = urlItems.getString(i);
                        taskExecutor.submit(new JasonRequire(url, latch, refs, client, context));
                    }
                    try {
                        latch.await();
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }

                // remote inject
                Iterator keys = refs.keys();
                while (keys.hasNext()) {
                    Object key = keys.next();
                    String js = refs.getString((String) key);
                    JasonParser.getInstance(context).inject(js);
                }

                // local inject (inline)
                for (int i = 0; i < inlineItems.length() ; i++) {
                    String js = inlineItems.getString(i);
                    JasonParser.getInstance(context).inject(js);
                }

                JasonHelper.next("success", action, data, event, context);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }


    }
    public void clear(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        JasonParser.getInstance(context).reset();
        JasonHelper.next("success", action, data, event, context);
    }
}
