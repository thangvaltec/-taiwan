package mcv.testfacepass.utils;


import android.app.Activity;
import android.content.Intent;

import com.google.easyapp.cameraengine.core.Size;
import com.google.easyapp.ui.camera.TakePicActivity;

public class GoogleWidgetHelper {

    public static void startCameraActivity(Activity activity, int requestCode, int cameraRotateAdjust, boolean cameraPreviewFlipX) {
        Intent intent = new Intent(activity, TakePicActivity.class);
        intent.putExtra(TakePicActivity.KEY_CAMERA_ROTATE_ADJUST, cameraRotateAdjust);
        intent.putExtra(TakePicActivity.KEY_CAMERA_PREVIEW_FLIPX, cameraPreviewFlipX);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startCameraActivity(Activity activity, int requestCode, int cameraId, Size previewSize, int cameraRotateAdjust, boolean cameraPreviewFlipX) {
        Intent intent = new Intent(activity, TakePicActivity.class);
        intent.putExtra(TakePicActivity.KEY_CAMERA_ID, cameraId);
        intent.putExtra(TakePicActivity.KEY_CAMERA_PREVIEW_SIZE, previewSize);
        intent.putExtra(TakePicActivity.KEY_CAMERA_ROTATE_ADJUST, cameraRotateAdjust);
        intent.putExtra(TakePicActivity.KEY_CAMERA_PREVIEW_FLIPX, cameraPreviewFlipX);
        activity.startActivityForResult(intent, requestCode);
    }
}
