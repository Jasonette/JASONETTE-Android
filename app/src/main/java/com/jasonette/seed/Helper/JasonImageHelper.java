package com.jasonette.seed.Helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Iterator;


public class JasonImageHelper {
    public interface JasonImageDownloadListener {
        public void onLoaded(byte[] data, Uri uri);
    }

    private JasonImageDownloadListener listener;
    private String url;
    private Context context;
    private byte[] data;

    public JasonImageHelper(String url, Context context) {
        // set null or default listener or accept as argument to constructor
        this.listener = null;
        this.url = url;
        this.context = context;
    }
    public JasonImageHelper(byte[] data, Context context){
        this.listener = null;
        this.data = data;
        this.context = context;
    }
    public void setListener(JasonImageDownloadListener listener) {
        this.listener = listener;
    }

    public void load(){
        try {
            File file =  new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            out.write(this.data);
            out.close();
            Uri bitmapUri = Uri.fromFile(file);
            this.listener.onLoaded(this.data, bitmapUri);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }

    }

    public void fetch(){

        try {
            // Constructing URL
            GlideUrl url;
            LazyHeaders.Builder builder = new LazyHeaders.Builder();

            // Add session if included
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            JSONObject session = null;
            URI uri_for_session = new URI(this.url.toLowerCase());
            String session_domain = uri_for_session.getHost();
            if(pref.contains(session_domain)){
                String str = pref.getString(session_domain, null);
                session = new JSONObject(str);
            }
            // Attach Header from Session
            if(session != null && session.has("header")) {
                Iterator<?> keys = session.getJSONObject("header").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = session.getJSONObject("header").getString(key);
                    builder.addHeader(key, val);
                }
            }

            url = new GlideUrl(this.url, builder.build());
            if (this.url.matches("\\.gif")) {
                /*
                Glide
                        .with(context)
                        .load(url)
                        .asGif()
                        .into((ImageView)view);
                        */
            } else {
                int width = 512;
                int height = 384;

                Glide
                        .with(context)
                        .asBitmap()
                        .load(url)
                        .into(new CustomTarget<Bitmap>(100,100) {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onStop() {

                            }

                            @Override
                            public void onDestroy() {

                            }

                            @Override
                            public void onLoadStarted(@Nullable Drawable placeholder) {

                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {

                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }

                            @Override
                            public void onResourceReady(Bitmap bitmap, Transition anim) {
                                Uri bitmapUri = null;
                                try {
                                    File file =  new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
                                    FileOutputStream out = new FileOutputStream(file);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                    out.close();
                                    bitmapUri = Uri.fromFile(file);

                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    byte[] byteArray = stream.toByteArray();

                                    listener.onLoaded(byteArray, bitmapUri);
                                } catch (Exception e) {
                                    Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                }

                            }
                        });

            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
