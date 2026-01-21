/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.easyapp.cameraengine.core.gpuimage;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import com.google.easyapp.cameraengine.core.gpuimage.util.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.google.easyapp.cameraengine.core.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
public class GPUImageFilterGroup extends GPUImageFilter {

    protected final FloatBuffer mGLCubeBuffer;
    protected final FloatBuffer mGLTextureBuffer;
    protected List<GPUImageFilter> mFilters;
    protected List<GPUImageFilter> mMergedFilters;
    // endregion
    protected int[] mFrameBuffers;
    protected int[] mFrameBufferTextures;

    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    public GPUImageFilterGroup() {
        this(null);
    }

    /**
     * Instantiates a new GPUImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    public GPUImageFilterGroup(List<GPUImageFilter> filters) {
        mFilters = filters;
        if (mFilters == null) {
            mFilters = new ArrayList<GPUImageFilter>();
        } else {
            updateMergedFilters();
        }

        mGLCubeBuffer = ByteBuffer.allocateDirect(GPUImageRenderer.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(GPUImageRenderer.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
    }

    public void addFilter(GPUImageFilter aFilter) {
        if (aFilter == null) {
            return;
        }
        mFilters.add(aFilter);
        updateMergedFilters();
    }

    public GPUImageFilter removeLastFilter() {
        GPUImageFilter filter = null;
        if (mFilters.size() > 1) {
            filter = mFilters.remove(mFilters.size() - 1);
            updateMergedFilters();
        }
        return filter;
    }

//    public void addFilter(int index, GPUImageFilter aFilter) {
//        if (aFilter == null) {
//            return;
//        }
//        mFilters.add(index, aFilter);
//        updateMergedFilters();
//    }

    /*
     * (non-Javadoc)
     * @see com.ulsee.easylib.gpuimage.GPUImageFilter#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();
        for (GPUImageFilter filter : mFilters) {
            filter.init();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.ulsee.easylib.gpuimage.GPUImageFilter#onDestroy()
     */
    @Override
    public void onDestroy() {
        destroyFramebuffers();
        for (GPUImageFilter filter : mFilters) {
            filter.destroy();
        }
        super.onDestroy();
    }

    protected void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            OpenGlUtils.deleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * com.ulsee.easylib.gpuimage.GPUImageFilter#onOutputSizeChanged(int,
     * int)
     */
    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        if (mFrameBuffers != null) {
            destroyFramebuffers();
        }

        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            mFilters.get(i).onOutputSizeChanged(width, height);
        }

        if (mMergedFilters != null && mMergedFilters.size() > 0) {
            size = mMergedFilters.size();
            mFrameBuffers = new int[size - 1];
            mFrameBufferTextures = new int[size - 1];

            for (int i = 0; i < size - 1; i++) {
                GLES20.glGenFramebuffers(1, mFrameBuffers, i);
                GLES20.glGenTextures(1, mFrameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

                mMergedFilters.get(i).setTargetFrameBuffer(mFrameBuffers[i]);
            }

            mMergedFilters.get(size - 1).setTargetFrameBuffer(mTargetFrameBuffer);
        }
    }


    @SuppressLint("WrongCall")
    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return;
        }
        if (mMergedFilters != null) {
            int size = mMergedFilters.size();
            int previousTexture = textureId;
            for (int i = 0; i < size; i++) {
                GPUImageFilter filter = mMergedFilters.get(i);
                if (i == 0) {
                    filter.onDraw(previousTexture, cubeBuffer, textureBuffer);
                } else {
                    filter.onDraw(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
                }
                if (i < mFrameBufferTextures.length)
                    previousTexture = mFrameBufferTextures[i];
            }
        }

    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<GPUImageFilter> getFilters() {
        return mFilters;
    }

    private List<GPUImageFilter> getMergedFilters() {
        return mMergedFilters;
    }

    public void updateMergedFilters() {
        if (mFilters == null) {
            return;
        }

        if (mMergedFilters == null) {
            mMergedFilters = new ArrayList<GPUImageFilter>();
        } else {
            mMergedFilters.clear();
        }

        List<GPUImageFilter> filters;
        for (GPUImageFilter filter : mFilters) {
            if (filter instanceof GPUImageFilterGroup) {
                ((GPUImageFilterGroup) filter).updateMergedFilters();
                filters = ((GPUImageFilterGroup) filter).getMergedFilters();
                if (filters == null || filters.isEmpty())
                    continue;
                mMergedFilters.addAll(filters);
                continue;
            }
            mMergedFilters.add(filter);

        }
    }

    @Override
    public void setTargetFrameBuffer(int targetFrameBuffer) {
        super.setTargetFrameBuffer(targetFrameBuffer);
        if (mMergedFilters != null && mMergedFilters.size() > 0) {
            mMergedFilters.get(mMergedFilters.size() - 1).setTargetFrameBuffer(targetFrameBuffer);
        }
    }

    @Override
    public void setRendererConfig(RendererConfig rendererConfig) {
        super.setRendererConfig(rendererConfig);
        for (GPUImageFilter filter : mFilters) {
            filter.setRendererConfig(rendererConfig);
        }
    }

    public boolean isChild(GPUImageFilter filter) {
        boolean result = false;
        for (GPUImageFilter filter1 : mFilters) {
            if ((filter1 == filter)
                    || (filter1 instanceof GPUImageFilterGroup && ((GPUImageFilterGroup) filter1).isChild(filter))) {
                result = true;
                break;
            }
        }
        return result;
    }

    public void resetLastTexture() {
        for (GPUImageFilter filter : mFilters) {
            if (filter instanceof GPUImageFilterGroup) {
                ((GPUImageFilterGroup) filter).resetLastTexture();
            }
        }
    }
}
