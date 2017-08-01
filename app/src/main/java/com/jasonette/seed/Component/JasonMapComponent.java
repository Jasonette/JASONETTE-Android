package com.jasonette.seed.Component;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
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
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

import static com.jasonette.seed.Action.JasonGeoAction.COORDS_STRING_FORMAT;


public class JasonMapComponent extends JasonComponent {

    static int EQUATOR_LENGTH = 40075004;

    private static final String DEFAULT_PIN_ACTION_PROP = "default_action";

    private static final String MAP_PIN_TITLE_PROP = "title";
    private static final String MAP_PIN_COORD_PROP = "coord";
    private static final String MAP_PIN_DESCRIPTION_PROP = "description";

    public static final String JS_FALSE = "false";

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                MapsInitializer.initialize(context);
                GoogleMapOptions options = new GoogleMapOptions();

                LatLng latlng = new LatLng(0, 0);
                if(component.has("region")) {
                    latlng = getCoordinates(component.getJSONObject("region"));
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
                mapview.getMapAsync(new MapReadyHandler(component, mapview, context));
                return mapview;
            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        } else {
            try {
                JasonComponent.build(view, component, parent, context);

                JasonHelper.style(component, context);

                JasonComponent.addListener(view, context);
                view.requestLayout();

                ((MapView)view).onResume(); // Trigger onResume
                return view;
            } catch (Exception e){
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
        return new View(context);
    }

    private static LatLng getCoordinates(JSONObject position) {
        // Calculate latitude and longitude
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            String[] r = position.getString("coord").split(",");
            if (r.length == 2) {
                latitude = Double.parseDouble(r[0]);
                longitude = Double.parseDouble(r[1]);
            }
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
        return new LatLng(latitude, longitude);
    }

    static class MapReadyHandler implements OnMapReadyCallback {

        private JSONObject component;
        private MapView view;
        private Context context;

        MapReadyHandler(JSONObject component, MapView view, Context context) {
            this.component = component;
            this.view = view;
            this.context = context;
        }

        private double getZoomForMetersWide(final double meters, final double width, final double lat) {
            // Converts metes wide to a zoom level for a google map, got this nice formula from
            // http://stackoverflow.com/a/21034310/1034194
            final double latAdjust = Math.cos(Math.PI * lat / 180.0);

            final double arg = EQUATOR_LENGTH * width * latAdjust / (meters * 256.0);

            return Math.log(arg) / Math.log(2.0);
        }

        @Override
        public void onMapReady(final GoogleMap map) {
            try {
                // Add pins to the map
                if (component.has("pins")) {
                    JSONArray pins = component.getJSONArray("pins");
                    for (int i = 0; i < pins.length(); i++) {
                        JSONObject pin = pins.getJSONObject(i);
                        MarkerOptions options = new MarkerOptions();
                        options.position(getCoordinates(pin));
                        if (pin.has("title")) {
                            options.title(pin.getString("title"));
                        }
                        if (pin.has("description")) {
                            options.snippet(pin.getString("description"));
                        }
                        Marker marker = map.addMarker(options);
                        if (pin.has("style")) {
                            JSONObject style = pin.getJSONObject("style");
                            if (style.has("selected") && style.getBoolean("selected")) {
                                marker.showInfoWindow();
                            }
                        }
                        if (pin.has(ACTION_PROP)) {
                            marker.setTag(pin);
                        }
                    }
                }

                // Move the camera to the zoom level that shows at least the desired region
                if(component.has("region")) {
                    JSONObject region = component.getJSONObject("region");
                    if(region.has("width") && region.has("height")) {
                        double width = region.getDouble("width");
                        double height = region.getDouble("height");
                        JasonViewActivity activity = ((JasonViewActivity) context);
                        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                        double viewWidth = view.getLayoutParams().width / metrics.density;
                        double viewHeight = view.getLayoutParams().height / metrics.density;
                        double meters = width;
                        if(height > width && viewHeight > viewWidth) {
                            // Widen the zoom in order to see the requested height
                            meters = height;
                        }
                        double lat = map.getCameraPosition().target.latitude;
                        float zoom = (float)getZoomForMetersWide(meters, viewWidth, lat);
                        map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
                    }
                }

                // Attach listener for pin 'clicks'
                map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                    @Override
                    public void onInfoWindowClick(Marker marker) {
                        try {
                            if (marker.getTag() == null) {
                                return;
                            }
                            JSONObject pinJSONObject = (JSONObject) marker.getTag();
                            if (pinJSONObject.has(ACTION_PROP)) {
                                JSONObject pinData = new JSONObject().put(MAP_PIN_TITLE_PROP, marker.getTitle());
                                LatLng pos = marker.getPosition();
                                pinData.put(MAP_PIN_COORD_PROP, String.format(COORDS_STRING_FORMAT
                                        , pos.latitude, pos.longitude));
                                pinData.put(MAP_PIN_DESCRIPTION_PROP, marker.getSnippet());
                                Intent intent = new Intent(INTENT_ACTION_CALL);
                                intent.putExtra(ACTION_PROP, pinJSONObject.get(ACTION_PROP).toString());
                                intent.putExtra(DATA_PROP, pinData.toString());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                            else if (pinJSONObject.has(HREF_PROP)) {
                                Intent intent = new Intent(INTENT_ACTION_CALL);
                                JSONObject href = new JSONObject();
                                href.put(TYPE_PROP, "$href");
                                href.put(OPTIONS_PROP, pinJSONObject.get(HREF_PROP).toString());
                                intent.putExtra(ACTION_PROP, pinJSONObject.get(ACTION_PROP).toString());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                        } catch (JSONException e) {
                            Timber.e(e);
                        }
                    }
                });

            } catch (Exception e) {
                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
            }
        }
    }

    // Intercept touch events on the RecyclerView to make sure they don't interfere with map moves
    static RecyclerView.SimpleOnItemTouchListener touchListener = new RecyclerView.SimpleOnItemTouchListener() {
        // Intercept touch events on the recycler view, and if they are over a mapview, make sure
        // to let the mapview handle them
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            if((rv.findChildViewUnder(e.getX(), e.getY())) instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout)rv.findChildViewUnder(e.getX(),e.getY());
                if (layout != null) {
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        // Weed out non-map views ASAP
                        if (child.getClass().equals(MapView.class)) {
                            int left = layout.getLeft() + child.getLeft();
                            int right = layout.getLeft() + child.getRight();
                            int top = layout.getTop() + child.getTop();
                            int bottom = layout.getTop() + child.getBottom();
                            if (e.getX() > left && e.getX() < right && e.getY() > top && e.getY() < bottom) {
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
            }
            return false;
        }
    };
}
