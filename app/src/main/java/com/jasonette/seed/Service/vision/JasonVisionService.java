package com.jasonette.seed.Service.vision;

import android.app.Activity;
import android.hardware.Camera;
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

import java.io.IOException;

/**
 * Created by realitix on 06/07/17.
 */

public class JasonVisionService {
    public static int FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static int BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    private BarcodeDetector detector;
    private CameraSource cameraSource;
    private Camera camera;
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
        is_open = false;
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
                .setBarcodeFormats(Barcode.QR_CODE)
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

                            }
                            return;
                        }
                    }
                }
            }
        });
        cameraSource = new CameraSource
                .Builder(context, detector)
                .build();
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
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == side) {
                cameraId = i;
                break;
            }
        }

        if (cameraId == -1) {
            Log.w("Camera", "Camera not found");
            return;
        }

        try {
            camera = Camera.open(cameraId);
            camera.setDisplayOrientation(getVerticalCameraDisplayOrientation(context, cameraId));
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
            cameraSource.start(holder);

            ((JasonViewActivity)context).simple_trigger("$vision.ready", new JSONObject(), context);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopCamera() {
        camera.stopPreview();
        cameraSource.stop();
        camera.release();
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


