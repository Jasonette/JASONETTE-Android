package com.jasonette.seed.Action;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.telephony.SmsManager;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;


public class JasonSmsAction {

    public static int REQUEST_CODE = 9339;

    /**
     * {
     *     "type": "$sms.send"
     * }
     */
    public void send(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject options = action.getJSONObject("options");
                    if (Build.VERSION.SDK_INT >= 23) {
                        if(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.SEND_SMS}, 10);
                        }
                    }

                    String number = options.get("number").toString();
                    String text = options.get("text").toString();

                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(number, null, text, null, null);
                } catch (Exception e){
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }
        });
        try {
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
