package com.jasonette.seed.Action;

import android.content.Context;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;


public class JasonVisionAction {

    /**
     * {
     *     "type": "$vision.scan"
     * }
     *
     * Scans code specified in
     *   https://developer.apple.com/documentation/avfoundation/avmetadataobjecttype?language=objc for iOS
     *   https://developers.google.com/vision/android/barcodes-overview for Android
     */

    public void scan(final JSONObject action, final JSONObject data, final JSONObject event, final Context context) {
        ((JasonViewActivity) context).cameraManager.is_open = true;
        JasonHelper.next("success", action, data, event, context);
    }
}
