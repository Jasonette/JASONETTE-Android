package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.commonsware.cwac.cam2.AbstractCameraActivity;
import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.VideoRecorderActivity;
import com.commonsware.cwac.cam2.ZoomStyle;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JasonMediaAction {

    /**********************************
     *
     * Play
     *
     **********************************/

    public void play(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        try {
            if(action.has("options")){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if(action.getJSONObject("options").has("url")){
                    intent.setDataAndType(Uri.parse(action.getJSONObject("options").getString("url")), "video/mp4");
                }
                if(action.getJSONObject("options").has("muted")){
                    AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    } else {
                        am.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    }
                }
                JSONObject callback = new JSONObject();
                callback.put("class", "JasonMediaAction");
                callback.put("method", "finishplay");
                JasonHelper.dispatchIntent(action, data, event, context, intent, callback);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    // Util for play
    public void finishplay(Intent intent, final JSONObject options) {
        try {
            JSONObject action = options.getJSONObject("action");
            JSONObject event = options.getJSONObject("event");
            Context context = (Context) options.get("context");

            // revert mute
            if(action.getJSONObject("options").has("muted")){
                AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                } else {
                    am.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
            }

            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    /**********************************
     *
     * Picker + Camera
     *
     **********************************/


    public void picker(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {

        // Image picker intent
        try {
            String type = "image";
            if(action.has("options")){
                if(action.getJSONObject("options").has("type")){
                    type = action.getJSONObject("options").getString("type");
                }
            }

            Intent intent;
            if(type.equalsIgnoreCase("video")){
                // video
                intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            } else {
                // image
                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            }

            // dispatchIntent method
            // 1. triggers an external Intent
            // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns

            // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
            JSONObject callback = new JSONObject();
            callback.put("class", "JasonMediaAction");
            callback.put("method", "process");

            JasonHelper.dispatchIntent(action, data, event, context, intent, callback);
        } catch (SecurityException e){
            JasonHelper.permission_exception("$media.picker", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }
    public void camera(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {

        // Image picker intent

        try {

            AbstractCameraActivity.Quality q = AbstractCameraActivity.Quality.LOW;
            String type = "photo";
            Boolean edit = false;

            if(action.has("options")) {
                JSONObject options = action.getJSONObject("options");

                // type
                if (options.has("type")) {
                    type = options.getString("type");
                }

                // quality
                if(type.equalsIgnoreCase("video")) {
                    // video
                    // high by default
                    q = AbstractCameraActivity.Quality.HIGH;
                } else {
                    // photo
                    // high by default
                    q = AbstractCameraActivity.Quality.HIGH;
                }
                if (options.has("quality")) {
                    String quality = options.getString("quality");
                    if (quality.equalsIgnoreCase("low")) {
                        q = AbstractCameraActivity.Quality.LOW;
                    } else if (quality.equalsIgnoreCase("medium")) {
                        q = AbstractCameraActivity.Quality.HIGH;
                    }
                }

                // edit
                if (options.has("edit")) {
                    edit = true;
                }
            }

            Intent intent;
            if(type.equalsIgnoreCase("video")) {
                // video
                VideoRecorderActivity.IntentBuilder builder =new VideoRecorderActivity.IntentBuilder(context)
                        .to(createFile("video", context))
                        .zoomStyle(ZoomStyle.SEEKBAR)
                        .updateMediaStore()
                        .quality(q);

                intent = builder.build();

            } else {
                // photo
                CameraActivity.IntentBuilder builder = new CameraActivity.IntentBuilder(context)
                        .to(createFile("image", context))
                        .zoomStyle(ZoomStyle.SEEKBAR)
                        .updateMediaStore()
                        .quality(q);

                if(!edit){
                    builder.skipConfirm();
                }

                intent = builder.build();

            }

            // dispatchIntent method
            // 1. triggers an external Intent
            // 2. attaches a callback with all the payload so that we can pick it up where we left off when the intent returns

            // the callback needs to specify the class name and the method name we wish to trigger after the intent returns
            JSONObject callback = new JSONObject();
            callback.put("class", "JasonMediaAction");
            callback.put("method", "process");
            JasonHelper.dispatchIntent(action, data, event, context, intent, callback);
        } catch (SecurityException e){
            JasonHelper.permission_exception("$media.camera", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    // util
    public void process(Intent intent, final JSONObject options) {
        try {
            JSONObject action = options.getJSONObject("action");
            JSONObject data = options.getJSONObject("data");
            JSONObject event = options.getJSONObject("event");
            Context context = (Context)options.get("context");

            Uri uri = intent.getData();

            // handling image
            String type = "image";
            if(action.has("options")) {
                if (action.getJSONObject("options").has("type")) {
                    type = action.getJSONObject("options").getString("type");
                }
            }
            if(type.equalsIgnoreCase("video")){
                // video
                try {
                    JSONObject ret = new JSONObject();
                    ret.put("file_url", uri.toString());
                    ret.put("content_type", "video/mp4");
                    JasonHelper.next("success", action, ret, event, context);
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            } else {
                // image
                InputStream stream =  context.getContentResolver().openInputStream(uri);
                byte[] byteArray = JasonHelper.readBytes(stream);
                String encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("data:image/jpeg;base64,");
                stringBuilder.append(encoded);
                String data_uri = stringBuilder.toString();

                try {
                    JSONObject ret = new JSONObject();
                    ret.put("data", encoded);
                    ret.put("data_uri", data_uri);
                    ret.put("content_type", "image/jpeg");
                    JasonHelper.next("success", action, ret, event, context);
                } catch (Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            }

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    private File createFile(String type, Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File f;
        if(type.equalsIgnoreCase("image")) {
            f = File.createTempFile( fileName, ".jpg", storageDir );
        } else if(type.equalsIgnoreCase("video")){
            f = File.createTempFile( fileName, ".mp4", storageDir );
        } else {
            f = File.createTempFile( fileName, ".txt", storageDir );
        }
        return f;
    }

}
