package com.google.easyapp.cameraengine.core.gpuimage.gl.output.picture;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;

import com.google.easyapp.cameraengine.core.gpuimage.GPUImageRenderer;
import com.google.easyapp.cameraengine.core.gpuimage.gl.EglCore;
import com.google.easyapp.cameraengine.core.gpuimage.gl.WindowSurface;

import java.nio.ByteBuffer;

public class ResultReader implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ResultReader";
    private final int height;
    private final int width;
    private ImageReader imageReader;
    private WindowSurface readImageWindowSurface;
    private GPUImageRenderer mRenderer;
    private Bitmap tempBitmap;
    private ResultFrameCallback resultFrameCallback;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;

    public ResultReader(int width, int height, Handler handler, EglCore eglCore) {
        this.width = width;
        this.height = height;
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        imageReader.setOnImageAvailableListener(this, handler);

        readImageWindowSurface = new WindowSurface(eglCore, imageReader.getSurface(), true);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireNextImage();//获取下一个
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();//设置的宽
        int height = image.getHeight();//设置的高
        int pixelStride = planes[0].getPixelStride();//内存对齐参数
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

//        byte[] data = getBuffer(rowStride * height);//获得byte
        byte[] data = new byte[rowStride * height];//获得byte

        ByteBuffer buffer = planes[0].getBuffer();//获得buffer
        buffer.get(data);//将buffer数据写入byte中

        if (tempBitmap == null) {
            tempBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        }
        buffer.rewind();
        tempBitmap.copyPixelsFromBuffer(buffer);

        image.close();//用完需要关闭

        final Bitmap bitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height);

        if (resultFrameCallback != null) {
            resultFrameCallback.onResultFrame(bitmap);
        }
    }

    public void setResultFrameCallback(ResultFrameCallback resultFrameCallback) {
        this.resultFrameCallback = resultFrameCallback;
    }

    public void startCapture() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        eglSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        eglContext = EGL14.eglGetCurrentContext();
        readImageWindowSurface.makeCurrent();
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0, 0, readImageWindowSurface.getWidth(), readImageWindowSurface.getHeight());
    }

    public void stopCapture() {
        readImageWindowSurface.swapBuffers();
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    }

    public void requestRenderer(GPUImageRenderer renderer) {
        mRenderer = renderer;

        startCapture();
        // Call the renderer initialization routines
        mRenderer.onSurfaceCreated(null, null);
        mRenderer.onSurfaceChanged(null, width, height);
//        startCapture();
        mRenderer.onDrawFrame(null);
        stopCapture();
    }

    public void release() {
        readImageWindowSurface.releaseEglSurface();
        imageReader.close();
    }
}
