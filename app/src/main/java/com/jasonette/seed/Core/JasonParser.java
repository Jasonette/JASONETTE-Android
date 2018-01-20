package com.jasonette.seed.Core;

import android.content.Context;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class JasonParser {
    static JSONObject res;
    private static Context context;

    private static JasonParser instance = null;

    private JasonParser(){
        this.listener = null;
    }

    public interface JasonParserListener {
        public void onFinished(JSONObject json);
    }
    private JasonParserListener listener;
    private V8 juice;
    public void setParserListener(JasonParserListener listener){
        this.listener = listener;
    }


    private class Task {
        public String data_type;
        public JSONObject data;
        public Object template;
        public Task(String data_type, JSONObject data, Object template) {
            this.data_type = data_type;
            this.data = data;
            this.template = template;
        }
    }
    private LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>();
    private void processTask(Task task) {
        try {
            Object template = task.template;
            JSONObject data = task.data;
            String data_type = task.data_type;

            // thread handling - acquire handle
            juice.getLocker().acquire();
            Console console = new Console();
            V8Object v8Console = new V8Object(juice);
            juice.add("console", v8Console);
            v8Console.registerJavaMethod(console, "log", "log", new Class<?>[] { String.class });
            v8Console.registerJavaMethod(console, "error", "error", new Class<?>[] { String.class });
            v8Console.registerJavaMethod(console, "trace", "trace", new Class<?>[] {});

            String templateJson = template.toString();
            String dataJson = data.toString();
            String val = "{}";

            V8Object parser = juice.getObject("ST");
            // Get global variables (excluding natively injected variables which will never be used in the template)
            String globals = juice.executeStringScript("JSON.stringify(Object.keys(this).filter(function(key){return ['ST', 'to_json', 'setImmediate', 'clearImmediate', 'console'].indexOf(key) === -1;}));");
            if(data_type.equalsIgnoreCase("json")) {
                V8Array parameters = new V8Array(juice);
                parameters.push(templateJson);
                parameters.push(dataJson);
                parameters.push(globals);
                parameters.push(true);
                val = parser.executeStringFunction("transform", parameters);
                parameters.release();
            } else {
                String raw_data = data.getString("$jason");
                V8Array parameters = new V8Array(juice).push("html");
                parameters.push(templateJson);
                parameters.push(raw_data);
                parameters.push(globals);
                parameters.push(true);
                val = juice.executeStringFunction("to_json", parameters);
                parameters.release();
            }
            parser.release();
            v8Console.release();

            if (val.equalsIgnoreCase("null")) {
                res = new JSONObject();
            } else {
                res = new JSONObject(val);
            }

            listener.onFinished(res);

        } catch (Exception e){
            Timber.w(e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

        // thread handling - release handle
        juice.getLocker().release();

    }



    public static JasonParser getInstance(Context context){
        if(instance == null)
        {
            instance = new JasonParser();
            try {
                String js = JasonHelper.read_file("st", context);
                String xhtmljs = JasonHelper.read_file("xhtml", context);
                instance.juice = V8.createV8Runtime();
                instance.juice.executeVoidScript(js);
                instance.juice.executeVoidScript(xhtmljs);
                instance.juice.getLocker().release();
            } catch (Exception e){
                Timber.w(e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
        return instance;
    }

    public static void inject(String js) {
        instance.juice.executeVoidScript(js);
    }
    public static void reset() {
        try {
            String js = JasonHelper.read_file("st", context);
            String xhtmljs = JasonHelper.read_file("xhtml", context);
            instance.juice = V8.createV8Runtime();
            instance.juice.executeVoidScript(js);
            instance.juice.executeVoidScript(xhtmljs);
            instance.juice.getLocker().release();
        } catch (Exception e){
            Timber.w(e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    private Thread thread;
    public void parse(final String data_type, final JSONObject data, final Object template, final Context context){
        try{
            taskQueue.put(new Task(data_type, data, template));
            if(thread == null) {
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                if (taskQueue.size() > 0) {
                                    processTask(taskQueue.take());
                                }
                                if (taskQueue.size() == 0) {
                                    thread = null;
                                    break;
                                }
                            } catch (Exception e) {
                                Timber.w(e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        }
                    }
                });
                thread.start();
            } else {
            }
        } catch (Exception e){
            Timber.w(e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}


/**
 * Override for console to print javascript debug output in the Android Studio console
 */
class Console {
    public void log(final String message) {
        Timber.d(message);
    }
    public void error(final String message) {
        Timber.e(message);
    }
    public void trace() {
        Timber.e("Unable to reproduce JS stacktrace");
    }
}
