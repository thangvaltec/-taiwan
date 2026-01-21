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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;

import com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture.ResultFrameCallback;
import com.google.easyapp.cameraengine.core.gpuimage.util.YuvMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * The main accessor for GPUImage functionality. This class helps to do common
 * tasks through a simple interface.
 */
public class GPUImage {
    private final Context mContext;
    private final GPUImageRenderer mRenderer;
    private IGLView mGlSurfaceView;
    private GPUImageFilter mFilter;
    private Bitmap mCurrentBitmap;
    private ScaleType mScaleType = ScaleType.CENTER_INSIDE;

    public GPUImage(final Context context) {
        if (!supportsOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        mFilter = new GPUImageFilter();
        mRenderer = new GPUImageRenderer(mFilter);
    }

    public void destroy() {
        mRenderer.destroy();
    }

    /**
     * Gets the images for multiple filters on a image. This can be used to
     * quickly get thumbnail images for filters. <br>
     * Whenever a new Bitmap is ready, the listener will be called with the
     * bitmap. The order of the calls to the listener will be the same as the
     * filter order.
     *
     * @param bitmap   the bitmap on which the filters will be applied
     * @param filters  the filters which will be applied on the bitmap
     * @param listener the listener on which the results will be notified
     */
    public static void getBitmapForMultipleFilters(final Bitmap bitmap,
                                                   final List<GPUImageFilter> filters, final ResponseListener<Bitmap> listener) {
        if (filters.isEmpty()) {
            return;
        }
        GPUImageRenderer renderer = new GPUImageRenderer(filters.get(0));
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);

        for (GPUImageFilter filter : filters) {
            renderer.setFilter(filter);
            listener.response(buffer.getBitmap());
            filter.destroy();
        }
        renderer.deleteImage();
        buffer.destroy();
        buffer.destroy();
    }

    /**
     * Checks if OpenGL ES 2.0 is supported on the current device.
     *
     * @param context the context
     * @return true, if successful
     */
    private boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    public Rect getBitmapRect() {
        return mRenderer.getBitmapRect();
    }

    /**
     * Sets the GLSurfaceView which will display the preview.
     *
     * @param view the GLSurfaceView
     */
    public void setGLSurfaceView(final IGLView view) {
        mGlSurfaceView = view;
        mGlSurfaceView.setRenderer(mRenderer);
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mRenderer.setBackgroundColor(red, green, blue);
    }

    public void addOnBitmapRectAdjustListener(GPUImageRenderer.OnBitmapRectAdjustListener onBitmapRectAdjustListener) {
        mRenderer.addOnBitmapRectAdjustListener(onBitmapRectAdjustListener);
    }

    public void removeOnBitmapRectAdjustListener(GPUImageRenderer.OnBitmapRectAdjustListener onBitmapRectAdjustListener) {

        mRenderer.removeOnBitmapRectAdjustListener(onBitmapRectAdjustListener);

    }

    public void setOnOpenGLInitSucceedListener(GPUImageRenderer.OnOpenGLInitSucceedListener onOpenGLInitSucceedListener) {
        mRenderer.setOnOpenGLInitSucceedListener(onOpenGLInitSucceedListener);

    }

    /**
     * Request the preview to be rendered again.
     */
    public void requestRender() {
        if (mGlSurfaceView != null) {
            mGlSurfaceView.requestRender();
        }
    }

    public void onPreviewFrameNv21(final byte[] data, final int width, final int height, final int rotation, final boolean flip) {
        mRenderer.onPreviewFrameNv21(data, width, height, rotation, flip);
    }

    public void onPreviewFrameRgbx(final byte[] data, final int width, final int height, final int rotation, final boolean flip) {
        mRenderer.onPreviewFrameRgbx(data, width, height, rotation, flip);
    }

    public void onPreviewFrame(byte[] data, int width, int height, int rotation, boolean flip) {
        mRenderer.onPreviewFrame(data, width, height, rotation, flip);
    }

    /**
     * Sets the filter which should be applied to the image which was (or will
     * be) set by setImage(...).
     *
     * @param filter the new filter
     */
    public void setFilter(final GPUImageFilter filter) {
        mFilter = filter;
        mRenderer.setFilter(mFilter);
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public void setImage(final Bitmap bitmap) {
        mCurrentBitmap = bitmap;
        mRenderer.setImageBitmap(bitmap, false);
        requestRender();
    }

    /**
     * This sets the scale type of GPUImage. This has to be run before setting the image.
     * If image is set and scale type changed, image needs to be reset.
     *
     * @param scaleType The new ScaleType
     */
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        mRenderer.setScaleType(scaleType);
        mRenderer.deleteImage();
        mCurrentBitmap = null;
        requestRender();
    }
    public void setYuvMode(YuvMode yuvMode) {
        if (mRenderer!=null){
            mRenderer.setYuvMode(yuvMode);
        }
    }
    /**
     * Deletes the current image.
     */
    public void deleteImage() {
        mRenderer.deleteImage();
        mCurrentBitmap = null;

        // region ulsee 注释requestRender的调用
//        requestRender();
        // endregion
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public void setImage(final Uri uri) {
        new LoadImageUriTask(this, uri).execute();
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public void setImage(final File file) {
        new LoadImageFileTask(this, file).execute();
    }


    /**
     * Gets the current displayed image with applied filter as a Bitmap.
     *
     * @return the current image with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(GPUImageFilter filter) {
        return getBitmapWithFilterApplied(mCurrentBitmap, filter);
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap) {
        return getBitmapWithFilterApplied(bitmap, null);
    }

    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap, GPUImageFilter filter) {
        return getBitmapWithFilterApplied(bitmap, filter, bitmap.getWidth(), bitmap.getHeight(), null);
    }

    public Bitmap getBitmapWithFilterApplied(GPUImageFilter filter, int outputWidth, int outputHeight, Rect rect) {
        return getBitmapWithFilterApplied(mCurrentBitmap, filter, outputWidth, outputHeight, rect);
    }

    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap, GPUImageFilter filter, int outputWidth, int outputHeight, Rect rect) {
        if (mGlSurfaceView != null) {
            if (filter == null || filter == mFilter ||
                    (mFilter instanceof GPUImageFilterGroup && ((GPUImageFilterGroup) mFilter).isChild(filter))) {
                final Semaphore waiter = new Semaphore(0);
                mGlSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        // destroy原因猜测：由于离屏渲染是一个新的EGLContext，所以filter中相关的数据需要重新初始化
                        mFilter.destroy();
                        waiter.release();
                    }
                });

                try {
                    waiter.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (filter == null)
                    filter = mFilter;
                filter.setTargetFrameBuffer(0);
            }
        }

        GPUImageRenderer.isOffsetScreenProcessing = true;
        GPUImageRenderer renderer = new GPUImageRenderer(filter);
//        renderer.setOffScreen(true);
        renderer.setRotation(0, mRenderer.isFlippedHorizontally());
        renderer.setScaleType(mScaleType);
        renderer.setScale(mRenderer.getScale());
        PixelBuffer buffer = new PixelBuffer(outputWidth, outputHeight);
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, false);
        Bitmap result = buffer.getBitmap(rect);
        filter.destroy();
        renderer.deleteImage();
        buffer.destroy();
        GPUImageRenderer.isOffsetScreenProcessing = false;

        if (filter == mFilter ||
                (mFilter instanceof GPUImageFilterGroup && ((GPUImageFilterGroup) mFilter).isChild(filter))) {
            mRenderer.setFilter(mFilter);
        }

//        Canvas canvas = new Canvas(result);
//        Paint paint = new Paint();
//        paint.setColor(Color.rgb(255, 0, 0));
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(3);
//        canvas.drawRect(rect, paint);

        return result;
    }

    public Bitmap getBitmapWithCordsDateApplied(boolean hor) {

        GPUImageRenderer renderer = new GPUImageRenderer(new GPUImageFilter());
        renderer.setRotation(0, mRenderer.isFlippedHorizontally());
        renderer.setScaleType(mScaleType);
        float[] cube = mRenderer.getCube();
        PixelBuffer buffer;
        if (hor) {
            float rite = Math.abs(cube[0]);
            buffer = new PixelBuffer((int) (mCurrentBitmap.getWidth() * rite), mCurrentBitmap.getHeight());
        } else {
            float rite = Math.abs(cube[1]);
            buffer = new PixelBuffer(mCurrentBitmap.getWidth(), (int) (mCurrentBitmap.getHeight() * rite));
        }

        buffer.setRenderer(renderer);
        renderer.setImageBitmap(mCurrentBitmap, false);
        Bitmap result = buffer.getBitmap();
        renderer.deleteImage();
        buffer.destroy();
        return result;
    }

    public void getPictureByFliter(Bitmap bitmap, GPUImageFilter gpuImageFilterCustom, ResultFrameCallback resultFrameCallback) {
        mRenderer.getPictureByFliter(bitmap, gpuImageFilterCustom, resultFrameCallback);
    }

    public void capturePicture(ResultFrameCallback resultFrameCallback) {
        mRenderer.capturePicture(resultFrameCallback);
        mGlSurfaceView.requestRender();
    }
    private int getOutputWidth() {
        if (mRenderer != null && mRenderer.getFrameWidth() != 0) {
            return mRenderer.getFrameWidth();
        } else if (mCurrentBitmap != null) {
            return mCurrentBitmap.getWidth();
        } else {
            WindowManager windowManager =
                    (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            return display.getWidth();
        }
    }

    private int getOutputHeight() {
        if (mRenderer != null && mRenderer.getFrameHeight() != 0) {
            return mRenderer.getFrameHeight();
        } else if (mCurrentBitmap != null) {
            return mCurrentBitmap.getHeight();
        } else {
            WindowManager windowManager =
                    (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            return display.getHeight();
        }
    }


    /**
     * Runs the given Runnable on the OpenGL thread.
     *
     * @param runnable The runnable to be run on the OpenGL thread.
     */
    // region ulsee
    public
    // endregion
    void runOnGLThread(Runnable runnable) {
        mRenderer.runOnDrawEnd(runnable);
    }

    // region ulsee
    public void runOnGLThreadDraw(Runnable runnable) {
        mRenderer.runOnDraw(runnable);
    }

    // endregion
    // region ulsee
    public void setTextureCords(float[] textureCord) {
        mRenderer.setTextureCords(textureCord);
    }

    public void setCubeCords(float[] cubes) {
        mRenderer.setCubeCords(cubes);
    }

    public float[] getCube() {
        return mRenderer.getCube();
    }

    public void setScale(float scale) {
        mRenderer.setScale(scale);
    }

    public Bitmap getCurrentBitmap() {
        return mCurrentBitmap;
    }

    public PointF getTextureDistance() {
        return mRenderer.getTextureDistance();
    }

    public PointF getCubeDistance() {
        return mRenderer.getCubeDistance();
    }

    public float[] getTextureCoods() {
        return mRenderer.getTextureCords();
    }

    public boolean isTranspose() {
        return mRenderer.isTranspose();
    }

    // endregion

    public enum ScaleType {CENTER_INSIDE, CENTER_CROP}


    public interface ResponseListener<T> {
        void response(T item);
    }


    private class LoadImageUriTask extends LoadImageTask {

        private final Uri mUri;

        public LoadImageUriTask(GPUImage gpuImage, Uri uri) {
            super(gpuImage);
            mUri = uri;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            try {
                InputStream inputStream;
                if (mUri.getScheme().startsWith("http") || mUri.getScheme().startsWith("https")) {
                    inputStream = new URL(mUri.toString()).openStream();
                } else {
                    inputStream = mContext.getContentResolver().openInputStream(mUri);
                }
                return BitmapFactory.decodeStream(inputStream, null, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected int getImageOrientation() throws IOException {
            Cursor cursor = mContext.getContentResolver().query(mUri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

            if (cursor == null || cursor.getCount() != 1) {
                return 0;
            }

            cursor.moveToFirst();
            int orientation = cursor.getInt(0);
            cursor.close();
            return orientation;
        }
    }

    private class LoadImageFileTask extends LoadImageTask {

        private final File mImageFile;

        public LoadImageFileTask(GPUImage gpuImage, File file) {
            super(gpuImage);
            mImageFile = file;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            return BitmapFactory.decodeFile(mImageFile.getAbsolutePath(), options);
        }

        @Override
        protected int getImageOrientation() throws IOException {
            ExifInterface exif = new ExifInterface(mImageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private abstract class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        private final GPUImage mGPUImage;
        private int mOutputWidth;
        private int mOutputHeight;

        @SuppressWarnings("deprecation")
        public LoadImageTask(final GPUImage gpuImage) {
            mGPUImage = gpuImage;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (mRenderer != null && mRenderer.getFrameWidth() == 0) {
                try {
                    synchronized (mRenderer.mSurfaceChangedWaiter) {
                        mRenderer.mSurfaceChangedWaiter.wait(3000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mOutputWidth = getOutputWidth();
            mOutputHeight = getOutputHeight();
            return loadResizedImage();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mGPUImage.deleteImage();
            mGPUImage.setImage(bitmap);
        }

        protected abstract Bitmap decode(BitmapFactory.Options options);

        private Bitmap loadResizedImage() {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decode(options);
            int scale = 1;
            while (checkSize(options.outWidth / scale > mOutputWidth, options.outHeight / scale > mOutputHeight)) {
                scale++;
            }

            scale--;
            if (scale < 1) {
                scale = 1;
            }
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inTempStorage = new byte[32 * 1024];
            Bitmap bitmap = decode(options);
            if (bitmap == null) {
                return null;
            }
            bitmap = rotateImage(bitmap);
            bitmap = scaleBitmap(bitmap);
            return bitmap;
        }

        private Bitmap scaleBitmap(Bitmap bitmap) {
            // resize to desired dimensions
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] newSize = getScaleSize(width, height);
            Bitmap workBitmap = Bitmap.createScaledBitmap(bitmap, newSize[0], newSize[1], true);
            if (workBitmap != bitmap) {
                bitmap.recycle();
                bitmap = workBitmap;
                System.gc();
            }

            if (mScaleType == ScaleType.CENTER_CROP) {
                // Crop it
                int diffWidth = newSize[0] - mOutputWidth;
                int diffHeight = newSize[1] - mOutputHeight;
                workBitmap = Bitmap.createBitmap(bitmap, diffWidth / 2, diffHeight / 2,
                        newSize[0] - diffWidth, newSize[1] - diffHeight);
                if (workBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = workBitmap;
                }
            }

            return bitmap;
        }

        /**
         * Retrieve the scaling size for the image dependent on the ScaleType.<br>
         * <br>
         * If CROP: sides are same size or bigger than output's sides<br>
         * Else   : sides are same size or smaller than output's sides
         */
        private int[] getScaleSize(int width, int height) {
            float newWidth;
            float newHeight;

            float withRatio = (float) width / mOutputWidth;
            float heightRatio = (float) height / mOutputHeight;

            boolean adjustWidth = mScaleType == ScaleType.CENTER_CROP
                    ? withRatio > heightRatio : withRatio < heightRatio;

            if (adjustWidth) {
                newHeight = mOutputHeight;
                newWidth = (newHeight / height) * width;
            } else {
                newWidth = mOutputWidth;
                newHeight = (newWidth / width) * height;
            }
            return new int[]{Math.round(newWidth), Math.round(newHeight)};
        }

        private boolean checkSize(boolean widthBigger, boolean heightBigger) {
            if (mScaleType == ScaleType.CENTER_CROP) {
                return widthBigger && heightBigger;
            } else {
                return widthBigger || heightBigger;
            }
        }

        private Bitmap rotateImage(final Bitmap bitmap) {
            if (bitmap == null) {
                return null;
            }
            Bitmap rotatedBitmap = bitmap;
            try {
                int orientation = getImageOrientation();
                if (orientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return rotatedBitmap;
        }

        protected abstract int getImageOrientation() throws IOException;
    }
}
