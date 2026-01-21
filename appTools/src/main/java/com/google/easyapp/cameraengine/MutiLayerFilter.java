package com.google.easyapp.cameraengine;

import com.google.easyapp.cameraengine.core.gpuimage.GPUImageFilter;
import com.google.easyapp.cameraengine.core.gpuimage.RendererConfig;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class MutiLayerFilter extends GPUImageFilter {

    private List<GPUImageFilter> gpuImageFilters;

    public MutiLayerFilter(GPUImageFilter bgFilter) {
        gpuImageFilters = new ArrayList<>();
        gpuImageFilters.add(bgFilter);
    }

    public MutiLayerFilter() {
        this(new GPUImageFilter());
    }

    public void addFilter(GPUImageFilter drawTriangFilter) {
        gpuImageFilters.add(drawTriangFilter);
    }

    public GPUImageFilter getFilterByIndex(int index) {
        return gpuImageFilters.get(index);
    }
    @Override
    public void onInit() {
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.onInit();
        }
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.onOutputSizeChanged(width, height);
        }
    }

    @Override
    public void onDestroy() {
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.onDestroy();
        }
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.onDraw(textureId, cubeBuffer, textureBuffer);
        }
    }

    @Override
    public void setTargetFrameBuffer(int targetFrameBuffer) {
        super.setTargetFrameBuffer(targetFrameBuffer);
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.setTargetFrameBuffer(targetFrameBuffer);
        }
    }

    @Override
    public void setRendererConfig(RendererConfig rendererConfig) {
        super.setRendererConfig(rendererConfig);
        for (GPUImageFilter gpuImageFilter : gpuImageFilters) {
            gpuImageFilter.setRendererConfig(rendererConfig);
        }
    }


    public int getFilterSize() {
        return gpuImageFilters.size();
    }
}
