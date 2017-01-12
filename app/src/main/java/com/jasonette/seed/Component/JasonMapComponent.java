package com.jasonette.seed.Component;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jasonette.seed.Core.JasonViewActivity;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONArray;
import org.json.JSONObject;


public class JasonMapComponent {
    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                MapsInitializer.initialize(context);
                GoogleMapOptions options = new GoogleMapOptions();

                LatLng latlng = new LatLng(0, 0);
                if(component.has("region")) {
                    latlng = GetCoordinates(component.getJSONObject("region"));
                }
                options.camera(new CameraPosition(latlng, 16, 0, 0));
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
                } else {
                    options.mapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                MapView mapview = new MapView(context, options);
                mapview.onCreate(null); // Trigger onCreate
                ((JasonViewActivity)context).addListViewOnItemTouchListener(touchListener);
                // Add pins when the map is ready
                mapview.getMapAsync(new MapReadyHandler(component));
                return mapview;
            } catch (Exception err) {
                Log.d("Error", err.toString());
            }
        } else {
            try {
                JasonComponent.build(view, component, parent, context);

                JasonHelper.style(component, context);

                JasonComponent.addListener(view, context);
                view.requestLayout();
                ((MapView)view).onResume(); // Trigger onResume
                return view;
            } catch (Exception err){
                Log.d("Error", err.toString());
            }
        }
        return new View(context);
    }

    public static LatLng GetCoordinates(JSONObject position) {
        // Calculate latitude and longitude
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            String[] r = position.getString("coord").split(",");
            if (r.length == 2) {
                latitude = Double.parseDouble(r[0]);
                longitude = Double.parseDouble(r[1]);
            }
        } catch (Exception err) {
            Log.d("Error", err.toString());
        }
        return new LatLng(latitude, longitude);
    }

    static class MapReadyHandler implements OnMapReadyCallback {
        private JSONObject component;

        MapReadyHandler(JSONObject component) {
            this.component = component;
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            try {
                // Add pins to the map
                if (component.has("pins")) {
                    JSONArray pins = component.getJSONArray("pins");
                    for (int i = 0; i < pins.length(); i++) {
                        JSONObject pin = pins.getJSONObject(i);
                        MarkerOptions options = new MarkerOptions();
                        options.position(GetCoordinates(pin));
                        if (pin.has("title")) {
                            options.title(pin.getString("title"));
                        }
                        if (pin.has("description")) {
                            options.snippet(pin.getString("description"));
                        }
                        Marker marker = googleMap.addMarker(options);
                        if (pin.has("style")) {
                            JSONObject style = pin.getJSONObject("style");
                            if (style.has("selected") && style.getBoolean("selected")) {
                                marker.showInfoWindow();
                            }
                        }
                    }
                }
            } catch (Exception err) {
                Log.d("Error", err.toString());
            }
        }
    }

    static RecyclerView.SimpleOnItemTouchListener touchListener = new RecyclerView.SimpleOnItemTouchListener() {
        // Intercept touch events on the recycler view, and if they are over a mapview, make sure
        // to let the mapview handle them
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            LinearLayout layout = (LinearLayout)rv.findChildViewUnder(e.getX(),e.getY());
            if(layout != null) {
                for(int i=0; i<layout.getChildCount(); i++) {
                    View child= layout.getChildAt(i);
                    // Weed out non-map views ASAP
                    if (child.getClass().equals(MapView.class)) {
                        int left = layout.getLeft() + child.getLeft();
                        int right = layout.getLeft() + child.getRight();
                        int top = layout.getTop() + child.getTop();
                        int bottom = layout.getTop() + child.getBottom();
                        if(e.getX() > left && e.getX() < right && e.getY() > top && e.getY() < bottom) {
                            switch (e.getActionMasked()) {
                                // Pressed on map: stop listview from scrolling
                                case MotionEvent.ACTION_DOWN:
                                    rv.requestDisallowInterceptTouchEvent(true);
                                    break;

                                // Released on map or cancelled: listview can be normal again
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_CANCEL:
                                    rv.requestDisallowInterceptTouchEvent(false);
                                    break;
                            }
                        }
                    }
                }
            }
            return false;
        }
    };
}