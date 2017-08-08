package com.jasonette.seed.Action;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

public class JasonGeoAction {

    public static final String COORDS_STRING_FORMAT = "%f,%f";

    public void get(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        try {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    try {

                        locationManager.removeUpdates(this);

                        JSONObject ret = new JSONObject();
                        String val = String.format(COORDS_STRING_FORMAT, location.getLatitude(), location.getLongitude());
                        ret.put("coord", val);
                        ret.put("value", val);
                        JasonHelper.next("success", action, ret, event, context);
                    } catch (Exception e) {
                        Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
        } catch (SecurityException e){
            JasonHelper.permission_exception("$geo.get", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }
}
