package com.google.easyapp.cameraengine.core.gpuimage;

public interface IGLView {
    void requestRender();

    void queueEvent(Runnable runnable);

    void setRenderer(GPUImageRenderer mRenderer);
}
