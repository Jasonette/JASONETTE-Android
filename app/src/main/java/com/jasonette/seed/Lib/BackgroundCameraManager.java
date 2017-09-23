package com.jasonette.seed.Lib;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;

import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by realitix on 06/07/17.
 */

public class BackgroundCameraManager {
    public static int FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static int BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    private Camera camera;
    private SurfaceView view;
    private int side;

    public BackgroundCameraManager(Activity context) {
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
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void stopCamera() {
        camera.stopPreview();
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
