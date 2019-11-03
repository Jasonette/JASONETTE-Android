package com.jasonette.seed.Service.vision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.jasonette.seed.Core.JasonViewActivity;

import org.json.JSONObject;


/**
 * Created by realitix on 06/07/17.
 */

public class JasonVisionService {
    public static int FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static int BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    private Activity temp_context;
    private SurfaceHolder temp_holder;
    private int temp_side;

    private BarcodeDetector detector;
    private CameraSource cameraSource;
    private SurfaceView view;
    private int side;
    public boolean is_open;

    public JasonVisionService(Activity context) {
        initView(context);
    }

    public void setSide(final int side) {
        this.side = side;
    }

    public SurfaceView getView() {
        return view;
    }

    private void initView(final Activity context) {
        if (view != null) {
            return;
        }
        view = new SurfaceView(context);
        final SurfaceHolder holder = view.getHolder();

        /*
         * Barcode detection
         *
         * When a code is recognized, this service:
         *
         * [1] triggers the event "$vision.onscan" with the following payload:
         *
         * {
         *   "$jason": {
         *     "type": "org.iso.QRCode",
         *     "content": "hello world"
         *   }
         * }
         *
         * the "type" attribute is different for iOS and Android. In case of Android it returns a number code specified at:
         *  https://developers.google.com/android/reference/com/google/android/gms/vision/barcode/Barcode.html#constants
         *
         * [2] Then immediately stops scanning.
         * [3] To start scanning again, you need to call $vision.scan again
         *
         */

        detector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();
        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (is_open) {
                    final SparseArray<Barcode> detected_items = detections.getDetectedItems();
                    if (detected_items.size() != 0) {
                        for (int i = 0; i < detected_items.size(); i++) {
                            int key = detected_items.keyAt(i);
                            final Barcode obj = detected_items.get(key);
                            is_open = false;
                            try {
                                JSONObject payload = new JSONObject();
                                /*
                                JSONArray corners = new JSONArray();
                                for (int j = 0; j < obj.cornerPoints.length; j++) {
                                    Point p = obj.cornerPoints[j];
                                    JSONObject point = new JSONObject();
                                    point.put("top", p.y);
                                    point.put("left", p.x);
                                    corners.put(point);
                                }
                                payload.put("corners", corners);
                                */
                                payload.put("content", obj.rawValue);
                                payload.put("type", obj.format);

                                JSONObject response = new JSONObject();
                                response.put("$jason", payload);

                                ((JasonViewActivity)context).simple_trigger("$vision.onscan", response, context);
                            } catch (Exception e) {
                                Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
                            }
                            return;
                        }
                    }
                }
            }
        });
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                startCamera(context, surfaceHolder, side);
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                stopCamera();
            }
        });
    }

    private void startCamera(final Activity context, SurfaceHolder holder, final int side) {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    temp_context = context;
                    temp_holder = holder;
                    temp_side = side;
                    ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CAMERA}, 50);
                } else {
                    openCamera(context, holder, side);
                }
            } else {
                openCamera(context, holder, side);
            }


        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }

    public void startVision(Activity context) {
        openCamera(context, temp_holder, temp_side);
        temp_context = null;
        temp_holder = null;
        temp_side = -1;
    }

    void openCamera(Activity context, SurfaceHolder holder, final int side) {
        try {
            if (cameraSource != null) {
                cameraSource.stop();
            }
            cameraSource = new CameraSource
                    .Builder(context, detector)
                    .setFacing(side)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraSource.start(holder);

            ((JasonViewActivity)context).simple_trigger("$vision.ready", new JSONObject(), context);

        } catch (Exception e) {
            Log.d("Warning", e.getStackTrace()[0].getMethodName() + " : " + e.toString());
        }
    }


    public void stopCamera() {
        cameraSource.stop();
    }

    private int getVerticalCameraDisplayOrientation(Activity context, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }
}
