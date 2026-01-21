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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Looper;

import com.google.easyapp.cameraengine.core.gpuimage.gl.EglCore;
import com.google.easyapp.cameraengine.core.gpuimage.util.YuvMode;
import com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture.ResultFrameCallback;
import com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture.ResultReader;
import com.google.easyapp.cameraengine.core.gpuimage.util.TextureRotationUtil;
import com.google.easyapp.utils.L;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.google.easyapp.cameraengine.core.gpuimage.OpenGlUtils.NO_TEXTURE;
import static com.google.easyapp.cameraengine.core.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

@TargetApi(11)
public class GPUImageRenderer implements Renderer {
    public static final int NO_IMAGE = -1;
    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };
    private static final String TAG = "GPUImageRenderer";
    public static boolean isOffsetScreenProcessing = false;
    public final Object mSurfaceChangedWaiter = new Object();
    private Runnable mUpdateTextureOnDrawRunnable = null;
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;
    private GPUImageFilter mFilter;
    private GPUVideoFilter mVideoFilter = new GPUVideoFilter();
    private int mGLTextureId = NO_IMAGE;
    private int mGLTextureLumianceId;
    private int mGLTextureLumianceAlphaId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;
    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private ByteBuffer mGLDataBuffer;
    private int mImageWidth;
    private int mImageHeight;
    private int mOutputWidth;
    private int mOutputHeight;
    private int mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    private OnOpenGLInitSucceedListener onOpenGLInitSucceedListener;
    private GPUImage.ScaleType mScaleType = GPUImage.ScaleType.CENTER_CROP;
    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;
    private float[] cube;
    private float[] textureCords;
    private float mScale = 1.0f;
    private List<OnBitmapRectAdjustListener> onBitmapRectAdjustListenerList;
    private RendererConfig rendererConfig = new RendererConfig();
    private EglCore eglCore;
    private ResultReader resultReader;
    private ResultFrameCallback resultFrameCallback;
    private boolean needCaptureOne = false;

    private float fps = 0f;
    private long firstDrawMills;
    private int count = 0;

    private float distCubeHorizontal = 0f;
    private float distCubeVertical = 0f;
    private float distTextureHorizontal = 0f;
    private float distTextureVertical = 0f;

    private YuvMode yuvMode;

    public GPUImageRenderer(GPUImageFilter filter, YuvMode yuvMode) {
        mFilter = filter;
        this.yuvMode = yuvMode;
        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();
        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(0, false);
        onBitmapRectAdjustListenerList = new ArrayList<>();
    }

    public GPUImageRenderer(GPUImageFilter filter) {
        this(filter, YuvMode.YUV_NV21);
        mFilter = filter;
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        L.d(TAG, "onSurfaceCreated 0");
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        if(null != yuvMode) {
            mGLTextureId = OpenGlUtils.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mGLTextureId);
            if (onOpenGLInitSucceedListener != null) {
                onOpenGLInitSucceedListener.onInitSucceed(mSurfaceTexture);
            }
            mVideoFilter.setYuvMode(yuvMode);
            mVideoFilter.init();
        }

        mFilter.init();
        L.d(TAG, "onSurfaceCreated 1");
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        L.d(TAG, "onSurfaceChanged 0");
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        this.mFilter.onOutputSizeChanged(width, height);
        eglCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
        resultReader = new ResultReader(width, height, new Handler(Looper.getMainLooper()), eglCore);
        resultReader.setResultFrameCallback(resultFrameCallback);

        adjustImageScaling();
        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
        L.d(TAG, "onSurfaceChanged 1");
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        calcFPS();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Runnable updateTextureOnDrawRunnable = mUpdateTextureOnDrawRunnable;
        if(null != updateTextureOnDrawRunnable) {
            updateTextureOnDrawRunnable.run();
            mUpdateTextureOnDrawRunnable = null;
        }
        runAll(mRunOnDraw);

        int textureId = mGLTextureId;
        if(null != yuvMode) {
            textureId = mVideoFilter.onDrawToTexture(mGLTextureLumianceId, mGLTextureLumianceAlphaId);
        }
        mFilter.onDraw(textureId, mGLCubeBuffer, mGLTextureBuffer);

        // 截屏操作
        if(needCaptureOne) {
            needCaptureOne = false;
            resultReader.startCapture();
            mFilter.onDraw(textureId, mGLCubeBuffer, mGLTextureBuffer);
            resultReader.stopCapture();
        }

        runAll(mRunOnDrawEnd);
    }

    public void destroy() {
        if(null != mVideoFilter) {
            mVideoFilter.destroy();
        }
        if(null != mFilter) {
            mFilter.destroy();
        }
        OpenGlUtils.deleteTexture(mGLTextureId);
        OpenGlUtils.deleteTexture(mGLTextureLumianceId);
        OpenGlUtils.deleteTexture(mGLTextureLumianceAlphaId);
        if(null != resultReader) {
            resultReader.release();
        }
        if(null != eglCore) {
            eglCore.release();
        }
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mBackgroundRed = red;
        mBackgroundGreen = green;
        mBackgroundBlue = blue;

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                rendererConfig.mBackgroundBlue = mBackgroundBlue;
                rendererConfig.mBackgroundGreen = mBackgroundGreen;
                rendererConfig.mBackgroundRed = mBackgroundRed;
            }
        });
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public void onPreviewFrame(byte[] data, int width, int height, int rotation, boolean flip) {
        if(null != yuvMode) {
            onPreviewFrameNv21(data, width, height, rotation, flip);
        } else {
            onPreviewFrameRgbx(data, width, height, rotation, flip);
        }
    }

    public void onPreviewFrameNv21(final byte[] data, final int width, final int height, final int rotation, final boolean flip) {
        Runnable updateTextureOnDrawRunnable = mUpdateTextureOnDrawRunnable;
        if(null == updateTextureOnDrawRunnable) {
            updateTextureOnDrawRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mSurfaceTexture != null) {
                            mSurfaceTexture.updateTexImage();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 生成纹理
                    if(mImageWidth != width || mImageHeight != height) {
                        OpenGlUtils.deleteTexture(mGLTextureLumianceId);
                        OpenGlUtils.deleteTexture(mGLTextureLumianceAlphaId);
                        mGLTextureLumianceId = NO_TEXTURE;
                        mGLTextureLumianceAlphaId = NO_TEXTURE;
                        if(null != mGLDataBuffer) {
                            mGLDataBuffer.clear();
                            mGLDataBuffer = null;
                        }
                        mVideoFilter.onOutputSizeChanged(width, height);
                    }

                    if (mGLDataBuffer == null) {
                        mGLDataBuffer = ByteBuffer.allocateDirect(data.length)
                                .order(ByteOrder.nativeOrder());
                    }

                    mGLDataBuffer.clear();
                    mGLDataBuffer.put(data);
                    mGLDataBuffer.position(0);
                    mGLTextureLumianceId = OpenGlUtils.loadTextureLumiance(mGLDataBuffer, width, height, mGLTextureLumianceId);
                    mGLDataBuffer.position(width * height);
                    mGLTextureLumianceAlphaId = OpenGlUtils.loadTextureLumianceAlpha(mGLDataBuffer, width, height, mGLTextureLumianceAlphaId);

                    if (mImageWidth != width || mImageHeight != height || mRotation != rotation || mFlipHorizontal != flip) {
                        mImageWidth = width;
                        mImageHeight = height;
                        mRotation = rotation;
                        mFlipHorizontal = flip;
                        adjustImageScaling();
                    }
                }
            };
            mUpdateTextureOnDrawRunnable = updateTextureOnDrawRunnable;
        }
    }

    public void onPreviewFrameRgbx(final byte[] data, final int width, final int height, final int rotation, final boolean flip) {
        Runnable updateTextureOnDrawRunnable = mUpdateTextureOnDrawRunnable;
        if(null == updateTextureOnDrawRunnable) {
            updateTextureOnDrawRunnable = new Runnable() {
                @Override
                public void run() {
                    // 生成纹理
                    if(mImageWidth != width || mImageHeight != height) {
                        OpenGlUtils.deleteTexture(mGLTextureId);
                        mGLTextureId = NO_TEXTURE;
                        if(null != mGLDataBuffer) {
                            mGLDataBuffer.clear();
                            mGLDataBuffer = null;
                        }
                    }

                    if (mGLDataBuffer == null) {
                        mGLDataBuffer = ByteBuffer.allocateDirect(data.length)
                                .order(ByteOrder.nativeOrder());
                    }

                    mGLDataBuffer.clear();
                    mGLDataBuffer.put(data);
                    mGLDataBuffer.position(0);
                    mGLTextureId = OpenGlUtils.loadTexture(mGLDataBuffer, width, height, mGLTextureId);
                    L.d(TAG,"mGLTextureId:"+mGLTextureId);
                    if (mImageWidth != width || mImageHeight != height || mRotation != rotation || mFlipHorizontal != flip) {
                        mImageWidth = width;
                        mImageHeight = height;
                        mRotation = rotation;
                        mFlipHorizontal = flip;
                        mFlipVertical = true;
                        adjustImageScaling();
                    }
                }
            };
            mUpdateTextureOnDrawRunnable = updateTextureOnDrawRunnable;
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                if (mFilter != null) {
                    mFilter.onDestroy();
                }
                mFilter = filter;
                mFilter.init();
                mFilter.setRendererConfig(rendererConfig);
                mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    public void deleteImage() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                OpenGlUtils.deleteTexture(mGLTextureId);
                mGLTextureId = NO_IMAGE;
            }
        });
    }

    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        runOnDraw(new Runnable() {

            @Override
            public void run() {
//                Bitmap resizedBitmap = null;
//                if (bitmap.getWidth() % 2 == 1) {
//                    resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(),
//                            Bitmap.Config.ARGB_8888);
//                    Canvas can = new Canvas(resizedBitmap);
//                    can.drawARGB(0x00, 0x00, 0x00, 0x00);
//                    can.drawBitmap(bitmap, 0, 0, null);
//                }
                mGLTextureId = OpenGlUtils.loadTexture(bitmap, mGLTextureId, recycle);
//                if (resizedBitmap != null) {
//                    resizedBitmap.recycle();
//                }
                mImageWidth = bitmap.getWidth();
                mImageHeight = bitmap.getHeight();
                adjustImageScaling();
            }
        });
    }

    public void setScaleType(final GPUImage.ScaleType scaleType) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mScaleType = scaleType;
                adjustImageScaling();
            }
        });
    }

    protected int getFrameWidth() {
        return mOutputWidth;
    }

    protected int getFrameHeight() {
        return mOutputHeight;
    }

    public void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (isTranspose()) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;
        if (ratioWidth == 0 || ratioHeight == 0) return;
        cube = CUBE;
        textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        if (mScaleType == GPUImage.ScaleType.CENTER_CROP) {
            distTextureHorizontal = (1 - 1 / ratioWidth) / 2;
            distTextureVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distTextureHorizontal), addDistance(textureCords[1], distTextureVertical),
                    addDistance(textureCords[2], distTextureHorizontal), addDistance(textureCords[3], distTextureVertical),
                    addDistance(textureCords[4], distTextureHorizontal), addDistance(textureCords[5], distTextureVertical),
                    addDistance(textureCords[6], distTextureHorizontal), addDistance(textureCords[7], distTextureVertical),
            };
        } else {
            distTextureHorizontal = 0;
            distTextureVertical = 0;
            if(isTranspose()) {
                float tmp = ratioHeight;
                ratioHeight = ratioWidth;
                ratioWidth = tmp;
            }
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        // region ulsee
         for (int i = 0; i < cube.length; i++) {
            cube[i] *= mScale;
        }
        // endregion

        distCubeHorizontal = cube[6];
        distCubeVertical = cube[7];

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
        Rect bitmapRect = getBitmapRect();
        if (onBitmapRectAdjustListenerList != null && !onBitmapRectAdjustListenerList.isEmpty()) {
            for (int i = 0; i < onBitmapRectAdjustListenerList.size(); ++i) {
                OnBitmapRectAdjustListener onBitmapRectAdjustListener = onBitmapRectAdjustListenerList.get(i);
                onBitmapRectAdjustListener.onBitmapRectAdjust(bitmapRect);
            }
        }
        rendererConfig.width = bitmapRect.width();
        rendererConfig.height = bitmapRect.height();
    }


    public float[] getCube() {
        return cube;
    }

    public float[] getTextureCords() {
        return textureCords;
    }

    public void setTextureCords(float[] textureCord) {
        textureCords = textureCord;

        mGLTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    public void setCubeCords(float[] cubes) {
        cube = cubes;
        mGLCubeBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
    }

    /***
     * 获取Bitmap区域
     * CENTER_CROP方式暂时未处理
     * @return
     */
    public Rect getBitmapRect() {
        Rect rect = new Rect();
        if (mScaleType == GPUImage.ScaleType.CENTER_INSIDE) {
            float absW = Math.abs(mOutputWidth / 2 * cube[2]);
            float absH = Math.abs(mOutputHeight / 2 * cube[3]);
            rect.left = (int) (mOutputWidth / 2 - absW);
            rect.top = (int) (mOutputHeight / 2 - absH);
            rect.right = (int) (mOutputWidth / 2 + absW);
            rect.bottom = (int) (mOutputHeight / 2 + absH);
        } else if (mScaleType == GPUImage.ScaleType.CENTER_CROP) {
            rect.left = (int) (0f - mOutputWidth * distTextureHorizontal);
            rect.top = (int) (0 - mOutputWidth * distTextureVertical);
            rect.right = (int) (mOutputWidth + mOutputWidth * distTextureHorizontal);
            rect.bottom = (int) (mOutputHeight + mOutputWidth * distTextureVertical);
        }
        return rect;
    }
    // endregion

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void setRotation(int rotation,
                            final boolean flipHorizontal) {
        mFlipHorizontal = flipHorizontal;
        setRotation(rotation);
    }

    public void setRotation(final int rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    public float getScale() {
        return this.mScale;
    }

    // region ulsee
    public void setScale(float scale) {
        this.mScale = scale;
    }


    public void addOnBitmapRectAdjustListener(OnBitmapRectAdjustListener onBitmapRectAdjustListener) {
        this.onBitmapRectAdjustListenerList.add(onBitmapRectAdjustListener);
    }

    public void removeOnBitmapRectAdjustListener(OnBitmapRectAdjustListener onBitmapRectAdjustListener) {
        this.onBitmapRectAdjustListenerList.remove(onBitmapRectAdjustListener);
    }
    public void setOnOpenGLInitSucceedListener(OnOpenGLInitSucceedListener onOpenGLInitSucceedListener) {
        this.onOpenGLInitSucceedListener = onOpenGLInitSucceedListener;
    }

    public void capturePicture(ResultFrameCallback resultFrameCallback) {
        this.resultFrameCallback = resultFrameCallback;
        if(resultReader != null) {
            resultReader.setResultFrameCallback(resultFrameCallback);
        }
        needCaptureOne = true;
    }

    public void getPictureByFliter(Bitmap bitmap, GPUImageFilter filter, ResultFrameCallback resultFrameCallback) {
        ResultReader resultReaderForGetBitmap = new ResultReader(bitmap.getWidth(), bitmap.getHeight(), new Handler(Looper.getMainLooper()), eglCore);
        resultReaderForGetBitmap.setResultFrameCallback(resultFrameCallback);
        GPUImageRenderer renderer = new GPUImageRenderer(filter);
        renderer.setRotation(0, isFlippedHorizontally());
        renderer.setScaleType(mScaleType);
        renderer.setScale(getScale());
        renderer.setImageBitmap(bitmap, false);
        resultReaderForGetBitmap.requestRenderer(renderer);
        filter.destroy();
        renderer.deleteImage();
        resultReaderForGetBitmap.release();
    }

    private void calcFPS() {
        if (count == 10) {
            count = 0;
            long curMills = System.currentTimeMillis();
            long duration = curMills - firstDrawMills;
            float durationPreFrame = 1.0f * duration / 10;
            if(Float.compare(durationPreFrame, 0f) > 0) {
                fps = 1000 / durationPreFrame;
                //L.d("zhangc", String.format("onDrawFrame fps=%.2f", fps));
            }
        }
        if (count == 0) {
            firstDrawMills = System.currentTimeMillis();
        }
        ++count;
    }

    public PointF getTextureDistance() {
        return new PointF(distTextureHorizontal, distTextureVertical);
    }

    public PointF getCubeDistance() {
        return new PointF(distCubeHorizontal, distCubeVertical);
    }

    public boolean isTranspose() {
        return (mRotation == 270 || mRotation == 90);
    }

    /**
     * 即将作废
     * 如果使用PictureModel请使用new GPUImageRenderer(filter, true)
     */
    @Deprecated
    public void setPictureModel(boolean pictureModel) {
        this.yuvMode = pictureModel ? null : YuvMode.YUV_NV21;
    }

    public void setYuvMode(YuvMode yuvMode) {
        this.yuvMode = yuvMode;
    }

    public interface OnBitmapRectAdjustListener {
        void onBitmapRectAdjust(Rect rect);
    }

    public interface OnOpenGLInitSucceedListener {
        void onInitSucceed(SurfaceTexture surfaceTexture);
    }
    // endregion
}
