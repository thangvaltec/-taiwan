package com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture;

import android.graphics.Bitmap;


public interface ResultFrameCallback {
    void onResultFrame(Bitmap bitmap);
}
