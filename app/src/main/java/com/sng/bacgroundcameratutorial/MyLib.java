package com.sng.bacgroundcameratutorial;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;
import java.util.Random;

public class MyLib {

    public static Bitmap finalfacebitmap;
    public static SharedPreferences sPref;

    public static SharedPreferences getsPref(Context context)
    {
        if(sPref==null)
            sPref=context.getSharedPreferences("MY_PREF",Context.MODE_PRIVATE);
        return sPref;
    }
    public static void SaveFacePref(int id,Context context)
    {
        SharedPreferences.Editor editor=getsPref(context).edit();
        editor.putInt("id",id);
        editor.commit();
    }
    public static int getFacePref(Context context)
    {
        return getsPref(context).getInt("id",0);
    }
    public static int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }
    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    public static int getRandomNumberInRange() {

        if (15 >= 100) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((100 - 15) + 15) + 15;
    }
    public static void savePinPref(int id,Context context)
    {
        SharedPreferences.Editor editor=getsPref(context).edit();
        editor.putInt("Pin",id);
        editor.commit();
    }
    public static void clearPinPref(int id,Context context)
    {
        SharedPreferences.Editor editor=getsPref(context).edit();
        editor.putInt("Pin",0);
        editor.commit();
    }
    public static int getPinPref(Context context)
    {
        return getsPref(context).getInt("Pin",0);
    }
    public static boolean isServiceRunning(Context context,Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }
}
