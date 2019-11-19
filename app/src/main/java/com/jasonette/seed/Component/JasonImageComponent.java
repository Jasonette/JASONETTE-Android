package com.jasonette.seed.Component;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

import java.net.URI;
import java.util.Iterator;


public class JasonImageComponent {
    private static LazyHeaders.Builder prepare(JSONObject component, Context context){
        try {
            // Constructing URL
            GlideUrl url;
            LazyHeaders.Builder builder = new LazyHeaders.Builder();

            // Add session if included
            SharedPreferences pref = context.getSharedPreferences("session", 0);
            JSONObject session = null;
            URI uri_for_session = new URI(component.getString("url").toLowerCase());
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

            if(component.has("header")){
                Iterator<?> keys = component.getJSONObject("header").keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = component.getJSONObject("header").getString(key);
                    builder.addHeader(key, val);
                }
            }
            return builder;

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return null;
        }
    }

    public static Object resolve_url(JSONObject component, Context context){
        try {
            String url = component.getString("url");
            if(url.contains("file://")) {
                return "file:///android_asset/file/" + url.substring(7);
            } else if(url.startsWith("data:image")) {
                return url;
            } else {
                LazyHeaders.Builder builder = JasonImageComponent.prepare(component, context);
                return new GlideUrl(url, builder.build());
            }
        } catch (Exception e){
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            return "";
        }
    }

    private static void gif(JSONObject component, View view, Context context){
        Object new_url = JasonImageComponent.resolve_url(component, context);
        Glide
                .with(context)
                .asGif()
                .load(new_url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into((ImageView)view);
    }
    private static void rounded(JSONObject component, View view, final float corner_radius_float, final Context context){
        Object new_url = JasonImageComponent.resolve_url(component, context);
        try {
            Glide
                    .with(context)
                    .asBitmap()
                    .load(new_url)
                    .fitCenter()
                    .into(new BitmapImageViewTarget((ImageView)view) {
                        @Override
                        protected void setResource(Bitmap res) {
                            RoundedBitmapDrawable bitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(context.getResources(), res);
                            bitmapDrawable.setCornerRadius(corner_radius_float);
                            view.setImageDrawable(bitmapDrawable);
                        }
                    });
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
    private static void normal(JSONObject component, final View view, Context context) {
        Object new_url = JasonImageComponent.resolve_url(component, context);
        if(new_url.getClass().toString().equalsIgnoreCase("string") && ((String)new_url).startsWith("data:image")){
            String n = (String)new_url;
            String base64;
            if(n.startsWith("data:image/jpeg")){
                base64 = n.substring("data:image/jpeg;base64,".length());
            } else if(n.startsWith("data:image/png")){
                base64 = n.substring("data:image/png;base64,".length());
            } else if(n.startsWith("data:image/gif")){
                base64 = n.substring("data:image/gif;base64,".length());
            } else {
                base64 = "";    // exception
            }
            byte[] bs = Base64.decode(base64, Base64.NO_WRAP);

            Glide.with(context).load(bs)
                    .into(new Target<Drawable>() {
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
                        public void getSize(@NonNull SizeReadyCallback cb) {

                        }

                        @Override
                        public void removeCallback(@NonNull SizeReadyCallback cb) {

                        }

                        @Override
                        public void setRequest(@Nullable Request request) {

                        }

                        @Nullable
                        @Override
                        public Request getRequest() {
                            return null;
                        }

                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> glideAnimation) {
                            ((ImageView)view).setImageDrawable(resource);
                        }
                    });
        } else {
            Glide
                    .with(context)
                    .load(new_url)
                    .into((ImageView) view);
        }

    }
    private static void tinted(JSONObject component, View view, final Context context){
        try {
            Object new_url = JasonImageComponent.resolve_url(component, context);
            final JSONObject style = component.getJSONObject("style");
            Glide
                    .with(context)
                    .asBitmap()
                    .load(new_url)
                    .fitCenter()
                    .into(new BitmapImageViewTarget((ImageView)view) {
                        @Override
                        protected void setResource(Bitmap res) {
                            BitmapDrawable d = new BitmapDrawable(context.getResources(), res);
                            try {
                                Drawable wrapper = DrawableCompat.wrap(d);
                                DrawableCompat.setTint(wrapper, JasonHelper.parse_color(style.getString("color")));
                                view.setImageDrawable(wrapper);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                                view.setImageDrawable(d);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                ImageView imageview;
                imageview = new ImageView(context);
                imageview.setAdjustViewBounds(true);
                return imageview;
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                return new View(context);
            }
        } else {
            JasonComponent.build(view, component, parent, context);

            int width = 0;
            int height = 0;
            float corner_radius = 0;
            try {
                String type;
                final JSONObject style = JasonHelper.style(component, context);
                if (style.has("corner_radius")) {
                    corner_radius = JasonHelper.pixels(context, style.getString("corner_radius"), "horizontal");
                }
                type = component.getString("type");

                if (component.has("url")) {
                    if(component.getString("url").contains("file://")){
                        if (corner_radius == 0) {
                            try {
                                if (component.getString("url").matches(".*\\.gif")) {
                                    JasonImageComponent.gif(component, view, context);
                                } else {
                                    if(style.has("color")){
                                        JasonImageComponent.tinted(component, view, context);
                                    } else {
                                        JasonImageComponent.normal(component, view, context);
                                    }
                                }
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        } else {
                            final float corner_radius_float = (float)corner_radius;
                            try {
                                JasonImageComponent.rounded(component, view, corner_radius_float, context);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        }

                        JasonComponent.addListener(view, context);
                        view.requestLayout();
                        return view;

                    } else {
                        if (corner_radius == 0) {
                            try {
                                if (component.getString("url").matches(".*\\.gif")) {
                                    JasonImageComponent.gif(component, view, context);
                                } else {
                                    if(style.has("color")){
                                        JasonImageComponent.tinted(component, view, context);
                                    } else {
                                        JasonImageComponent.normal(component, view, context);
                                    }
                                }
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        } else {
                            final float corner_radius_float = (float)corner_radius;
                            try {
                                JasonImageComponent.rounded(component, view, corner_radius_float, context);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                        }

                        JasonComponent.addListener(view, context);
                        view.requestLayout();
                        return view;

                    }

                } else {
                    return new View(context);
                }
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
            return new View(context);
        }

    }
}
