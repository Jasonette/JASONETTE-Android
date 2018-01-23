package com.jasonette.seed.Action;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.apache.commons.lang.reflect.MethodUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Stores the parameters so that MethodInvocation class can invoke call(..) which invokesr
 * JasonHelper.next(..) with those parameters.
 */
class InvocationCallback {
    InvocationCallback(final String type, final JSONObject action, final JSONObject event,
                       final Context context) {
        this.type = type;
        this.action = action;
        this.event = event;
        this.context = context;
    }

    public void call(final JSONObject data) {
        JasonHelper.next(type, action, data, event, context);
    }

    private final String type;
    private final JSONObject action;
    private final JSONObject event;
    private final Context context;
}

/*
 * The runnable calls 'method' on the instance 'object' with the parameters 'params' unwrapped.
 * The method should always return a JSONObject. The object returned by the function should
 * have the following attributes:
 *   - "result": True/False. If this field is true, success callback will be invoked, else error
 *               callback.
 *   - "data"  : Optional. The data being passed to the downstream processes.
 *
 * In case of any exceptions in the invocation, error callback will be called.
 */
class MethodInvocation implements Runnable {
    private final Method method;
    private final Object[] params;
    private final Object object;
    private final InvocationCallback success;
    private final InvocationCallback error;

    MethodInvocation(final Object object, final Method method, Object[] params,
                     final InvocationCallback success, final InvocationCallback error) {
        this.object = object;
        this.method = method;
        this.params = params;
        this.success = success;
        this.error = error;
    }

    @Override
    public void run() {
        final JSONObject res;
        try {
            res = (JSONObject) method.invoke(object, params);
        } catch (Exception e) {
            Log.e("JasonExternalAction", "Method invocation failed.", e);
            error.call(new JSONObject());
            return;
        }

        if (res.optBoolean("result", false)) {
            final JSONObject data = res.optJSONObject("data");
            success.call(data == null? new JSONObject() : data);
        } else {
            final JSONObject data = res.optJSONObject("data");
            error.call(data == null? new JSONObject() : data);
        }
    }
}

/*
 * The action can be invoked like:
 * {
 *   "method": "my.package.Class.public_method",
 *   "params": [
 *      "I can pass any JSON object here",  // This will be translated to String.
 *      1.2,
 *      ["hello", "world"],   // Translated to List
 *      {"a": "b", "c": "d"}  // Translated to Map.
 *   ],
 *   "block": false,   // Call blocks. Default false.
 *   "cacheObject": true,  // The instance of my.package.Class will be cached and no new instances
 *                         // will be created for every invocation.
 *  "success": { ...  },
 *  "error": { ...  }
 * }
 */
public class JasonExternalAction {
    public JasonExternalAction() {
        objectCache = new HashMap<String, Object>();
        final HandlerThread thread = new HandlerThread("ExternalActionThread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public void invoke(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        final JSONObject options;
        final JSONArray params;
        final String method;
        boolean isBlocking, cacheObject;
        final Object object;
        final Method m;
        Object[] translatedParams;

        try {
            options = action.getJSONObject("options");
            method = options.getString("method");
            params = options.getJSONArray("params");
            isBlocking = options.optBoolean("block", false);
            cacheObject = options.optBoolean("cacheObject", true);

            object = getObject(method, cacheObject, context);
            translatedParams = translateParams(params);
            m = getMethod(object, method, translatedParams);
        } catch (Exception e) {
            // Any 'internal' error above need not propagate to the front end.
            Log.e("JasonExternalAction", "Couldn't find method.", e);
            handleError(new Exception("Internal Error."), action, event, context);
            return;
        }

        final InvocationCallback success = new InvocationCallback("success", action, event, context);
        final InvocationCallback error = new InvocationCallback("error", action, event, context);

        Runnable runnable = new MethodInvocation(object, m, translatedParams, success, error);

        if (isBlocking) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    private void handleError(Exception err, JSONObject action, JSONObject event, Context context) {
        try {
            JSONObject error = new JSONObject();
            error.put("data", err.toString());
            JasonHelper.next("error", action, error, event, context);
        } catch(JSONException e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    private Object getObject(final String methodName, boolean cache, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final String className = methodName.substring(0, methodName.lastIndexOf('.'));

        if (cache && objectCache.containsKey(className)) {
            return objectCache.get(className);
        }

        Class<?> cls = Class.forName(className);
        final Object obj = cls.newInstance();

        if (cache) {
            objectCache.put(className, obj);
        }

        try {
            final Method initialize = getMethod(obj, "initialize", new Object[]{context});
            if (initialize != null) {
                initialize.invoke(obj, context);
            }
        } catch (Exception e) {
            Log.w("JasonExternalAction", "Exception while initializing: " + methodName, e);
        }

        return obj;
    }

    /*
     * Returns an instance of Method within object that can be called with given pameters.
     * Boxing/Unboxing is taken care of, too. Throws NoSuchMethodError if no suitable method
     * is found.
     */
    private Method getMethod(final Object object, final String method, Object[] params) {
        final String methodName = method.substring(method.lastIndexOf('.') + 1);
        final Class<?>[] paramClasses = new Class<?>[params.length];

        for (int i = 0; i < params.length; i++) {
            final Object value = params[i];
            paramClasses[i] = value.getClass();
        }

        Method m = MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, paramClasses);
        if (m == null) {
            throw new NoSuchMethodError(method);
        }
        return m;
    }

    /*
     * Converts parameters from JSON world to Java world. JSONArray maps to ArrayList, JSONObject maps to
     * HashMap<String, Object>
     */
    private Object[] translateParams(final JSONArray params) throws JSONException {
        final Object[] result = new Object[params.length()];
        for (int i = 0; i < params.length(); i++) {
            Object value = params.get(i);

            if (value instanceof Integer || value instanceof String || value instanceof Double) {
                // We are good.
            } if (value instanceof JSONArray) {
                JSONArray jsonArr = (JSONArray) value;
                List<Object> res = new ArrayList<>(jsonArr.length());
                for (int j = 0; j < jsonArr.length(); i++) {
                    res.add(jsonArr.get(j));
                }
                value = res;
            } else if (value instanceof  JSONObject) {
                Map<String, Object> map = new HashMap<String, Object>();
                JSONObject jsonObj = (JSONObject) value;
                Iterator<String> iter = jsonObj.keys();
                while(iter.hasNext()) {
                    String key = iter.next();
                    map.put(key, jsonObj.get(key));
                }
                value = map;
            } else {
                throw new IllegalArgumentException("Unsupported parameter type: " + params.get(i));
            }
            result[i] = value;
        }

        return result;
    }

    private Map<String, Object> objectCache;  // Instantiated objects may be cached.
    private Handler handler;  // All method invocations happen in this Handler's thread.
}
