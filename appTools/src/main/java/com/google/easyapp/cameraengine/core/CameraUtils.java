package com.google.easyapp.cameraengine.core;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CameraUtils {
    private static final String TAG = "CameraUtil";

    public static int getActivityDisplayOrientation(Activity activity) {
        int degrees = 0;
        if (null != activity) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }
        }
        return degrees;
    }

    public static int getImageOrient(int cameraOrientation, int degrees, int cameraId) {
        int result;
        if (cameraId == CameraDefine.CAMERA_ID_BACK) {
            result = (360 - cameraOrientation + degrees + 360) % 360;
        } else {
            result = (360 - cameraOrientation - degrees + 360) % 360;
        }
        return result;
    }

    public static Bitmap getPreviewBitmap(byte[] bytes, int format, int width, int height) {

        YuvImage yuv = new YuvImage(bytes, format, width, height, null);

        ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, jpgStream);

        byte[] jpgByte = jpgStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpgByte, 0, jpgByte.length);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Size findBestPreviewSizeValue(Size wantedPreviewSize, CameraCharacteristics characteristics) {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        List<Size> previewSizes = convertSizeList(map.getOutputSizes(SurfaceTexture.class));
        return findBestPreviewSizeValue(wantedPreviewSize, previewSizes);
    }

    public static Size findBestPreviewSizeValue(Size wantedPreviewSize, Camera.Parameters parameters) {
        List<Size> previewSizes = convertSizeList(parameters.getSupportedPreviewSizes());
        return findBestPreviewSizeValue(wantedPreviewSize, previewSizes);
    }

    public static Size findBestPreviewSizeValue(Size wantedPreviewSize, List<Size> previewSizes) {
        int index = 0;
        for (int i = 0; i < previewSizes.size(); i++) {
            Size previewSize = previewSizes.get(i);
            index = i;
            if (previewSize.width == wantedPreviewSize.width
                    && previewSize.height == wantedPreviewSize.height) {
                break;
            }
        }
        return previewSizes.get(index);
    }

    public static String findBestFocusModeValue(String focusMode, Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (null != focusMode && supportedFocusModes.contains(focusMode)) {
            parameters.setFocusMode(focusMode);
        } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        focusMode = parameters.getFocusMode();
        return focusMode;
    }

    public static Point findMaxFpsRangeValue(Camera.Parameters parameters) {
        List<int[]> supportedPreviewFpsRanges = parameters.getSupportedPreviewFpsRange();
        int index = 0;
        for (int i = 1; i < supportedPreviewFpsRanges.size(); i++) {
            int[] fpsRange = supportedPreviewFpsRanges.get(i);
            int[] maxFpsRange = supportedPreviewFpsRanges.get(index);
            // 优先上限，其次再取最高下限
            if (fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] > maxFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                index = i;
                continue;
            } else if (fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] == maxFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                    && fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] > maxFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]) {
                index = i;
                continue;
            }
        }
        return new Point(supportedPreviewFpsRanges.get(index)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                supportedPreviewFpsRanges.get(index)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
    }

    public static int findBestExposureCompensation(int exposureCompensation, Camera.Parameters parameters) {
        int minExposureCompensation = parameters.getMinExposureCompensation();
        int maxExposureCompensation = parameters.getMaxExposureCompensation();
        int exposureCompensationStep = 1;//parameters.getExposureCompensationStep();
        int bestExposureCompensation = minExposureCompensation;
        int minDistance = Math.abs(minExposureCompensation - exposureCompensation);
        for(int i = 1; i < (maxExposureCompensation - minExposureCompensation); i++) {
            int ec = (int)(minExposureCompensation + i * exposureCompensationStep);
            int distance = Math.abs(ec - exposureCompensation);
            if(distance < minDistance) {
                bestExposureCompensation = ec;
                minDistance = distance;
            }
        }
        return bestExposureCompensation;
    }

    public static List<Size> convertSizeList(List<Camera.Size> cameraSizeList) {
        List<Size> sizeList = null;
        if (null != cameraSizeList) {
            sizeList = new ArrayList<>();
            for (Camera.Size cameraSize : cameraSizeList) {
                sizeList.add(new Size(cameraSize.width, cameraSize.height));
            }
        }
        return sizeList;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static List<Size> convertSizeList(android.util.Size cameraSizeList[]) {
        List<Size> sizeList = null;
        if (null != cameraSizeList) {
            sizeList = new ArrayList<>();
            for (android.util.Size cameraSize : cameraSizeList) {
                sizeList.add(new Size(cameraSize.getWidth(), cameraSize.getHeight()));
            }
        }
        return sizeList;
    }
}