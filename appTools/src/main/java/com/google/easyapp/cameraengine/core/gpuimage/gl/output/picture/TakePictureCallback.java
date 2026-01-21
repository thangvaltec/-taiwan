package com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture;

import android.graphics.Bitmap;


public interface TakePictureCallback {
    void onTakingPicture(final Bitmap bitmap);
}