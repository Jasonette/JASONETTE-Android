package com.jasonette.seed.Component;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaderFactory;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;

import java.net.URI;
import java.util.Iterator;

public class JasonMapComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                MapsInitializer.initialize(context);
                GoogleMapOptions options = new GoogleMapOptions();
                options.camera(new CameraPosition(new LatLng(0, 0), 0, 0, 0));
                JSONObject style = component.getJSONObject("style");
                if (style.has("type")) {
                    switch ((String) style.get("type")) {
                        case "satellite":
                            options.mapType(GoogleMap.MAP_TYPE_SATELLITE);
                            break;
                        case "hybrid":
                            options.mapType(GoogleMap.MAP_TYPE_HYBRID);
                            break;
                        case "terrain":
                            options.mapType(GoogleMap.MAP_TYPE_TERRAIN);
                            break;
                        default:
                            options.mapType(GoogleMap.MAP_TYPE_NORMAL);
                    }
                }
                MapView mapview = new MapView(context, options);
                return mapview;
            } catch (Exception err) {
                Log.d("Error", err.toString());
            }
        } else {
            try {
                JasonComponent.build(view, component, parent, context);

                JasonComponent.addListener(view, context);
                view.requestLayout();
                return view;
            } catch (Exception err){
                Log.d("Error", err.toString());
            }
        }
        return new View(context);
    }
}