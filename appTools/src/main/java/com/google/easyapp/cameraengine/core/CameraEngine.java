package com.google.easyapp.cameraengine.core;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.google.easyapp.utils.L;

import java.util.ArrayList;
import java.util.List;

public class CameraEngine extends BaseCameraEngine {

    private Camera mCamera = null;
    private boolean isPreviewing = false;
    private int mPreviewFromat;
    private int mCameraOrientation;
    private List<Size> mSupportPreviewSize;

    public CameraEngine(Context context) {
        super(context);
    }

    public CameraEngine(Context context, boolean isPreviewTargetCustom) {
        super(context, isPreviewTargetCustom);
    }

    @Override
    public void startPreview() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                try {
                    L.d(TAG, "startPreview 0 mCamera=" + mCamera + ",isPreviewing=" + isPreviewing);
                    if (null != mCamera && !isPreviewing) {
                        if (!isPreviewTargetCustom()) {
                            if (null == mSurfaceTexture) {
                                mSurfaceTexture = new SurfaceTexture(-1);
                            }
                        }
                        if (null != mSurfaceTexture) {
                            mCamera.setPreviewTexture(mSurfaceTexture);
                            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                                @Override
                                public void onPreviewFrame(byte[] data, Camera camera) {
                                    // 在华为麦芒6上，切换分辨率时会出现data和previewSize不同步的问题，导致崩溃，此处采取丢弃措施
                                    int rightLenght = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(mPreviewFromat) / Byte.SIZE;
                                    if (data != null && data.length == rightLenght) {
                                        synchronized (cameraCallBacks) {
                                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                                if (cameraCallBack != null) {
                                                    cameraCallBack.onPreviewFrame(CameraEngine.this, data);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            Camera.Parameters parameters = mCamera.getParameters();
                            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                            parameters.setFocusMode(mFocusMode);
                            parameters.setPreviewFpsRange(mFpsRange.x, mFpsRange.y);
                            parameters.setExposureCompensation(mExposureCompensation);
                            mPreviewFromat = parameters.getPreviewFormat();
                            L.d(TAG, "startPreview mPreviewFromat=" + mPreviewFromat);
                            mCamera.setParameters(parameters);
                            mCamera.startPreview();
                            isPreviewing = true;
                        }
                    }
                } catch (Exception e) {
                    L.e(TAG, "startPreview failed, e=" + e);
                    e.printStackTrace();
                }
                L.d(TAG, "startPreview 1");
            }
        });
    }

    @Override
    public void stopPreview() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "stopPreview 0 mCamera=" + mCamera);
                try {
                    if (null != mCamera) {
                        mCamera.setPreviewCallback(null);
                        mCamera.stopPreview();
                    }
                    if (!isPreviewTargetCustom() && null != mSurfaceTexture) {
                        mSurfaceTexture.release();
                        mSurfaceTexture = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isPreviewing = false;
                L.d(TAG, "stopPreview 1");
            }
        });
    }

    @Override
    public List<Integer> getSupportCameraId() {
        int cameraCount = Camera.getNumberOfCameras();
        List<Integer> cameraIdList = new ArrayList<>();
        for (int i = 0; i < cameraCount; i++) {
            cameraIdList.add(i);
        }
        return cameraIdList;
    }

    @Override
    public List<Size> getSupportPreviewSize() {
        return mSupportPreviewSize;
    }

    @Override
    protected void openCamera() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "openCamera 0 mCameraId=" + mCameraId);
                do {
                    if (null != mCamera) {
                        L.w(TAG, "openCamera CameraEngine is opened");
                        break;
                    }

                    Exception openCameraException = null;
                    try {
                        try {
                            L.d(TAG, "openCamera CameraUtils.openCamera mCameraId=" + mCameraId);
                            mCamera = Camera.open(mCameraId);
                            openedCameraIds.add(mCameraId);
                        } catch (Exception e) {
                            L.w(TAG, "openCamera cameraId(" + mCameraId + ") open failed, e=" + e);
                            int cameraId = ((mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                                    ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
                            if (openedCameraIds.contains(cameraId)) {
                                throw e;
                            }
                            mCameraId = cameraId;
                            mCamera = Camera.open(mCameraId);
                        }
                    } catch (Exception e) {
                        L.e(TAG, "openCamera cameraId(" + mCameraId + ") error, e=" + e);
                        e.printStackTrace();
                        openCameraException = e;
                        mCamera = null;
                    }

                    if (null == mCamera) {
                        synchronized (cameraCallBacks) {
                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraError(openCameraException);
                                }
                            }
                        }
                        break;
                    }
                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        mPreviewSize = CameraUtils.findBestPreviewSizeValue(mPreviewSize, parameters);
                        mFocusMode = CameraUtils.findBestFocusModeValue(mFocusMode, parameters);
                        mFpsRange = CameraUtils.findMaxFpsRangeValue(parameters);
                        mExposureCompensation = CameraUtils.findBestExposureCompensation(mExposureCompensation, parameters);
                        mSupportPreviewSize = CameraUtils.convertSizeList(parameters.getSupportedPreviewSizes());

                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        Camera.getCameraInfo(mCameraId, cameraInfo);
                        mCameraOrientation = cameraInfo.orientation;

                        L.d(TAG, "openCamera cameraId(" + mCameraId + ") success, mPreviewSize=" + mPreviewSize
                                + ",mFocusMode=" + mFocusMode + ",mFpsRange=" + mFpsRange + ",mCameraOrientation=" + mCameraOrientation
                                + ",mExposureCompensation=" + mExposureCompensation);
                        synchronized (cameraCallBacks) {
                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraSucceed(CameraEngine.this, mCameraId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        L.e(TAG, "config camera parameters error");
                        openCameraException = e;
                        synchronized (cameraCallBacks) {
                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraError(openCameraException);
                                }
                            }
                        }
                    }

                } while (false);
                L.d(TAG, "openCamera 1");
            }
        });
    }

    @Override
    protected void closeCamera() {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "closeCamera 0");
                stopPreview();
                try {
                    if (null != mCamera) {
                        mCamera.release();
                        mCamera = null;
                    }
                    openedCameraIds.remove(mCameraId);
                } catch (Exception e) {
                    L.e(TAG, "closeCamera error e=" + e);
                    e.printStackTrace();
                }
                L.d(TAG, "closeCamera 1");
            }
        });
    }

    @Override
    protected int getCameraOrientation() {
        return mCameraOrientation;
    }

    @Override
    public int getPreviewFormat() {
        return mPreviewFromat;
    }
}
