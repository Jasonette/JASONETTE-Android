package com.jasonette.seed.Action;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

import java.io.File;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;

public class JasonAudioAction {
    private MediaPlayer player;
    public void play(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        try {
            if (action.has("options")) {
                JSONObject options = action.getJSONObject("options");
                if(options.has("url")){
                    if(player == null) {
                        player = new MediaPlayer();
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

                        String url = options.getString("url");
                        player.reset();
                        player.setDataSource(url);
                        player.prepare();
                    }

                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    ((JasonViewActivity)context).setVolumeControlStream(AudioManager.STREAM_MUSIC);

                    if(player.isPlaying()){
                        player.pause();
                    } else {
                        player.start();
                    }
                }
            }
            JasonHelper.next("success", action, new JSONObject(), event, context);
        } catch (SecurityException e){
            JasonHelper.permission_exception("$audio.play", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    public void pause(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if(player!=null){
            player.pause();
        }
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void stop(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if(player!=null){
            player.stop();
            player.release();
            player = null;
        }
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }
    public void duration(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if (player != null) {
            try {
                int duration = (int) (player.getDuration() / 1000);
                JSONObject ret = new JSONObject();
                ret.put("value", String.valueOf(duration));
                JasonHelper.next("success", action, ret, event, context);
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                try {
                    JSONObject err = new JSONObject();
                    err.put("message", "invalid position");
                    JasonHelper.next("error", action, err, event, context);
                } catch (Exception e2){
                    Log.d("Warning", e2.getStackTrace()[0].getMethodName() + " : " + e2.toString());
                }
            }
        } else {
            try {
                JSONObject err = new JSONObject();
                err.put("message", "player doesn't exist");
                JasonHelper.next("error", action, err, event, context);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }
    public void position(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if (player != null) {
            try {
                int duration = player.getDuration();
                int position = player.getCurrentPosition();
                float ratio = position/duration;
                JSONObject ret = new JSONObject();
                ret.put("value", String.valueOf(ratio));
                JasonHelper.next("success", action, ret, event, context);
            } catch (Exception e) {
                try {
                    JSONObject err = new JSONObject();
                    err.put("message", "invalid position or duration");
                    JasonHelper.next("error", action, err, event, context);
                } catch (Exception e2){
                    Log.d("Warning", e2.getStackTrace()[0].getMethodName() + " : " + e2.toString());
                }
            }
        } else {
            try {
                JSONObject err = new JSONObject();
                err.put("message", "player doesn't exist");
                JasonHelper.next("error", action, err, event, context);
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }
    public void seek(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        if(player!=null) {
            try {
                if (action.has("options")) {
                    JSONObject options = action.getJSONObject("options");
                    if(options.has("position")){
                        float position = Float.parseFloat(options.getString("position"));
                        int duration = player.getDuration();
                        player.seekTo((int)position * duration);
                    }
                }
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
        JasonHelper.next("success", action, new JSONObject(), event, context);
    }

    public void record(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        try {
            int color = JasonHelper.parse_color("rgba(0,0,0,0.8)");
            if (action.has("options")) {
                JSONObject options = action.getJSONObject("options");
                if (options.has("color")) {
                    color = JasonHelper.parse_color(options.getString("color"));
                }
            }
            AndroidAudioConverter.load(context, new ILoadCallback() {
                @Override
                public void onSuccess() {
                }
                @Override
                public void onFailure(Exception e) {
                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                }
            });

            String filePath = Environment.getExternalStorageDirectory() + "/recorded_audio.wav";
            int requestCode = (int)(System.currentTimeMillis() % 10000);
            AndroidAudioRecorder.with((JasonViewActivity)context)
                    .setFilePath(filePath)
                    .setColor(color)
                    .setRequestCode(requestCode)

                    .setSource(AudioSource.MIC)

                    .setChannel(AudioChannel.STEREO)
                    .setSampleRate(AudioSampleRate.HZ_48000)
                    .setAutoStart(true)
                    .setKeepDisplayOn(true)

                    .record();

            JSONObject callback = new JSONObject();
            callback.put("class", "JasonAudioAction");
            callback.put("method", "process");
            JasonHelper.dispatchIntent(String.valueOf(requestCode), action, data, event, context, null, callback);
        } catch (SecurityException e){
            JasonHelper.permission_exception("$audio.record", context);
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    // util
    public void process(Intent intent, final JSONObject options) {
        convert(options);
    }

    private void convert(final JSONObject options){
        File file = new File(Environment.getExternalStorageDirectory() + "/recorded_audio.wav");
        try {
            final JSONObject action = options.getJSONObject("action");
            final JSONObject data = options.getJSONObject("data");
            final JSONObject event = options.getJSONObject("event");
            final Context context = (Context) options.get("context");

            IConvertCallback callback = new IConvertCallback() {
                @Override
                public void onSuccess(File convertedFile) {
                    try {
                        JSONObject ret = new JSONObject();
                        String filePath = "file://" + convertedFile.getAbsolutePath();
                        ret.put("file_url", filePath);
                        ret.put("url", filePath);
                        ret.put("content_type", "audio/m4a");
                        JasonHelper.next("success", action, ret, event, context);
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
                @Override
                public void onFailure(Exception error) {
                    try {
                        JSONObject err = new JSONObject();
                        err.put("message", error.toString());
                        JasonHelper.next("error", action, err, event, context);
                    } catch (Exception e){
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }
            };
            AndroidAudioConverter.with(context)
                    .setFile(file)
                    .setFormat(AudioFormat.M4A)
                    .setCallback(callback)
                    .convert();
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
