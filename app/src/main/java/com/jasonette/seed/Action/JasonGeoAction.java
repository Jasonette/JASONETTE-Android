package com.jasonette.seed.Action;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

public class JasonGeoAction {

    public static final String COORDS_STRING_FORMAT = "%f,%f";

    public void get(final JSONObject action, JSONObject data, final JSONObject event, final Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 51);
                } else {
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
                }
            } else {
                getLocationManagerInfo(action, event, context);
            }
        } catch (SecurityException e){
            JasonHelper.permission_exception("$geo.get", context);
        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    private void getLocationManagerInfo(final JSONObject action, final JSONObject event, final Context context){
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
    }
}
