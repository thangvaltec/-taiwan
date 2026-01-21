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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;

public class GPUImageView extends FrameLayout {

    public Size mForceSize = null;
    private GPUImageGLSurfaceView mGLSurfaceView;
    private GPUImage mGPUImage;
    private GPUImageFilter mFilter;
    private float mRatio = 0.0f;

    public GPUImageView(Context context) {
        super(context);
        init(context, null);
    }

    public GPUImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        mGLSurfaceView = new GPUImageGLSurfaceView(context, attrs);
        addView(mGLSurfaceView);
        mGPUImage = new GPUImage(getContext());
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(new MyConfigChooser());
//        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        if(null != mGLSurfaceView.getHolder()) {
            mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        }
        mGPUImage.setGLSurfaceView(mGLSurfaceView);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGPUImage.destroy();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio != 0.0f) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            int newHeight;
            int newWidth;
            if (width / mRatio < height) {
                newWidth = width;
                newHeight = Math.round(width / mRatio);
            } else {
                newHeight = height;
                newWidth = Math.round(height * mRatio);
            }

            int newWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY);
            int newHeightSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY);
            super.onMeasure(newWidthSpec, newHeightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * Retrieve the GPUImage instance used by this view.
     *
     * @return used GPUImage instance
     */
    public GPUImage getGPUImage() {
        return mGPUImage;
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mGPUImage.setBackgroundColor(red, green, blue);
    }

    // TODO Should be an xml attribute. But then GPUImage can not be distributed as .jar anymore.
    public void setRatio(float ratio) {
        mRatio = ratio;
        mGLSurfaceView.requestLayout();
        mGPUImage.deleteImage();
    }

    /**
     * Set the scale type of GPUImage.
     *
     * @param scaleType the new ScaleType
     */
    public void setScaleType(GPUImage.ScaleType scaleType) {
        mGPUImage.setScaleType(scaleType);
    }

    /**
     * Get the current applied filter.
     *
     * @return the current filter
     */
    public GPUImageFilter getFilter() {
        return mFilter;
    }

    /**
     * Set the filter to be applied on the image.
     *
     * @param filter Filter that should be applied on the image.
     */
    public void setFilter(GPUImageFilter filter) {
        mFilter = filter;
        mGPUImage.setFilter(filter);
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public void setImage(final Bitmap bitmap) {
        mGPUImage.setImage(bitmap);
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public void setImage(final Uri uri) {
        mGPUImage.setImage(uri);
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public void setImage(final File file) {
        mGPUImage.setImage(file);
    }

    public void requestRender() {
        mGLSurfaceView.requestRender();
    }


    public Bitmap getBitmapWithFilterApplied() {
        return getBitmapWithFilterApplied(null);
    }

    public Bitmap getBitmapWithFilterApplied(GPUImageFilter filter) {
        return mGPUImage.getBitmapWithFilterApplied(filter);
    }

    /**
     * Retrieve current image with filter applied and given size as Bitmap.
     *
     * @param width  requested Bitmap width
     * @param height requested Bitmap height
     * @return Bitmap of picture with given size
     * @throws InterruptedException
     */
    public Bitmap capture(final int width, final int height) throws InterruptedException {
        // This method needs to run on a background thread because it will take a longer time
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Do not call this method from the UI thread!");
        }

        mForceSize = new Size(width, height);

        final Semaphore waiter = new Semaphore(0);

        // Layout with new size
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                waiter.release();
            }
        });
//        uiPost(new Runnable() {
//            @Override
//            public void run() {
//                // Show loading
//                addView(new LoadingView(getContext()));
//
//                mGLSurfaceView.requestLayout();
//            }
//        });
        waiter.acquire();

        // Run one render pass
        mGPUImage.runOnGLThread(new Runnable() {
            @Override
            public void run() {
                waiter.release();
            }
        });
        requestRender();
        waiter.acquire();
        Bitmap bitmap = capture();


        mForceSize = null;
        post(new Runnable() {
            @Override
            public void run() {
                mGLSurfaceView.requestLayout();
            }
        });
        requestRender();

        postDelayed(new Runnable() {
            @Override
            public void run() {
                // Remove loading view
                removeViewAt(1);
            }
        }, 300);

        return bitmap;
    }

    /**
     * Capture the current image with the size as it is displayed and retrieve it as Bitmap.
     *
     * @return current output as Bitmap
     * @throws InterruptedException
     */
    public Bitmap capture() throws InterruptedException {
        final Semaphore waiter = new Semaphore(0);

        final int width = mGLSurfaceView.getMeasuredWidth();
        final int height = mGLSurfaceView.getMeasuredHeight();

        // Take picture on OpenGL thread
        final int[] pixelMirroredArray = new int[width * height];
        mGPUImage.runOnGLThread(new Runnable() {
            @Override
            public void run() {
                final IntBuffer pixelBuffer = IntBuffer.allocate(width * height);
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
                int[] pixelArray = pixelBuffer.array();

                // Convert upside down mirror-reversed image to right-side up normal image.
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        pixelMirroredArray[(height - i - 1) * width + j] = pixelArray[i * width + j];
                    }
                }
                waiter.release();
            }
        });
        requestRender();
        waiter.acquire();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixelMirroredArray));
        return bitmap;
    }

    /**
     * capture by rect
     *
     * @param rect
     * @return
     * @throws InterruptedException
     */
    public Bitmap capture(Rect rect) throws InterruptedException {
        final Semaphore waiter = new Semaphore(0);

        final int x = rect.left;
        final int y = rect.top;
        final int w = rect.width();
        final int h = rect.height();

        // Take picture on OpenGL thread
        final int[] pixelMirroredArray = new int[w * h];
        mGPUImage.runOnGLThread(new Runnable() {
            @Override
            public void run() {
                final IntBuffer pixelBuffer = IntBuffer.allocate(w * h);
                GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
                int[] pixelArray = pixelBuffer.array();
                int len = pixelArray.length;

                // Convert upside down mirror-reversed image to right-side up normal image.
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        pixelMirroredArray[(h - i - 1) * w + j] = pixelArray[i * w + j];
                    }
                }
                waiter.release();
            }
        });
        requestRender();
        waiter.acquire();

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixelMirroredArray));
        return bitmap;
    }

    /**
     * capture by cube rect
     *
     * @param x
     * @param y
     * @param w
     * @param h
     * @return
     * @throws InterruptedException
     */
    public Bitmap capture(float x, float y, float w, float h) throws InterruptedException {
        final int width = mGLSurfaceView.getMeasuredWidth();
        final int height = mGLSurfaceView.getMeasuredHeight();
        final int x1 = (int) (width * x);
        final int y1 = (int) (height * y);
        final int w1 = (int) (width * w);
        final int h1 = (int) (height * h);
        Rect rect = new Rect(x1, y1, w1, h1);
        return capture(rect);
    }

    /**
     * Pauses the GLSurfaceView.
     */
    public void onPause() {
        mGLSurfaceView.onPause();
    }

    /**
     * Resumes the GLSurfaceView.
     */
    public void onResume() {
        mGLSurfaceView.onResume();
    }


    public interface OnPictureSavedListener {
        void onPictureSaved(Uri uri);
    }

    public static class Size {
        public int width;
        public int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private class GPUImageGLSurfaceView extends GLTextureView implements IGLView {
        public GPUImageGLSurfaceView(Context context) {
            super(context);
        }

        public GPUImageGLSurfaceView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (mForceSize != null) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(mForceSize.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(mForceSize.height, MeasureSpec.EXACTLY));
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        public SurfaceHolder getHolder() {
            return null;
        }

        @Override
        public void setRenderer(GPUImageRenderer mRenderer) {
            super.setRenderer(mRenderer);
        }
    }

}
