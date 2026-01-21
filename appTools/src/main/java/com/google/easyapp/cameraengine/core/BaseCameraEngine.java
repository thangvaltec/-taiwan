package com.google.easyapp.cameraengine.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.google.easyapp.utils.L;
import com.google.easyapp.utils.ToastUtils;
import com.google.easyapp.utils.WeakHandler;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public abstract class BaseCameraEngine {
    protected static final String TAG = "CameraEngine";
    protected static Handler handler;
    protected static final HandlerThread handlerThread = new HandlerThread("CameraEngineThread");
    protected static final Set<Integer> openedCameraIds = new HashSet<>();
    private WeakHandler mainHandler;

    static {
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        L.d(TAG, "camera thread init");
    }

    protected int mCameraId = CameraDefine.CAMERA_ID_FRONT;
    protected Size mPreviewSize = new Size(1920, 1080);
    protected Point mFpsRange;
    protected String mFocusMode = null;
    protected int mExposureCompensation = 0;
    protected SurfaceTexture mSurfaceTexture = null;

    protected Context mContext;
    protected Activity mActivity;
    protected int mActivityRotate;
    protected Disposable permissionDispose = null;
    protected final Vector<CameraCallBack> cameraCallBacks = new Vector<>();

    // FLAGS 超过两个换成按位读取
    // 预览目标是否自定义
    private boolean mIsPreviewTargetCustom;

    public BaseCameraEngine(Context context) {
        this(context, false);
    }


    public BaseCameraEngine(Context context, boolean isPreviewTargetCustom) {
        mContext = context;
        mIsPreviewTargetCustom = isPreviewTargetCustom;
        if (context instanceof Activity) {
            this.mActivity = (Activity) context;
        }
        mActivityRotate = CameraUtils.getActivityDisplayOrientation(this.mActivity);
        mainHandler = new WeakHandler(Looper.getMainLooper());
    }

    public static void runOnCameraThread(Runnable runnable) {
        if (Thread.currentThread() == handlerThread) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    public void setCameraId(final int cameraId) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "setCameraId 0 cameraId=" + cameraId);
                mCameraId = cameraId;
                L.d(TAG, "setCameraId 1");
            }
        });
    }

    public void setPreviewSize(final Size previewSize) {
        runOnCameraThread(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "setPreviewSize 0 previewSize=" + previewSize);
                mPreviewSize = previewSize;
                L.d(TAG, "setPreviewSize 1");
            }
        });
    }

    public void onResume() {
        L.d(TAG, "onResume 0 mActivity=" + mActivity);
        if (null == mActivity) {
            openCamera();
        } else {
            if (null != permissionDispose) {
                permissionDispose.dispose();
            }
            permissionDispose = new RxPermissions(mActivity).request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean b) {
                    if (b) {
                        openCamera();
                    } else {
                        ToastUtils.showShort("获取权限失败");
                        synchronized (cameraCallBacks) {
                            for (CameraCallBack cameraCallBack : cameraCallBacks) {
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraError(new Exception("获取权限失败"));
                                }
                            }
                        }
                    }
                }
            });
        }
        L.d(TAG, "onResume 1");
    }

    public void onPause() {
        L.d(TAG, "onPause 0");
        if (null != permissionDispose) {
            permissionDispose.dispose();
        }
        closeCamera();
        L.d(TAG, "onPause 1");
    }

    public void addCameraCallBack(CameraCallBack cameraCallBack) {
        L.d(TAG, "addCameraCallBack cameraCallBack=" + cameraCallBack);
        synchronized (cameraCallBacks) {
            if (null != cameraCallBack && !cameraCallBacks.contains(cameraCallBack)) {
                cameraCallBacks.add(cameraCallBack);
            }
        }
    }

    public void removeCameraCallBack(CameraCallBack cameraCallBack) {
        L.d(TAG, "removeCameraCallBack cameraCallBack=" + cameraCallBack);
        synchronized (cameraCallBacks) {
            if (cameraCallBack != null) {
                cameraCallBacks.remove(cameraCallBack);
            }
        }
    }

    public void switchCamera() {
        L.d(TAG, "switchCamera");
        List<Integer> cameraIdList = getSupportCameraId();
        int currentCameraIndex = cameraIdList.indexOf(mCameraId);
        if (currentCameraIndex >= 0) {
            ++currentCameraIndex;
            currentCameraIndex %= cameraIdList.size();
        } else {
            currentCameraIndex = 0;
        }
        switchId(cameraIdList.get(currentCameraIndex));
    }

    public void switchId(int cameraId) {
        closeCamera();
        setCameraId(cameraId);
        openCamera();
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraDefine.CAMERA_ID_FRONT;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public Point getFpsRange() {
        return mFpsRange;
    }

    public int getExposureCompensation() {
        return mExposureCompensation;
    }

    /**
     * 获取相机预览数据逆时针旋转到activity方向所需要的旋转角度
     */
    public int getCameraRotate() {
        return CameraUtils.getImageOrient(getCameraOrientation(), mActivityRotate, mCameraId);
    }

    protected boolean isPreviewTargetCustom() {
        return mIsPreviewTargetCustom;
    }

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract List<Integer> getSupportCameraId();

    public abstract List<Size> getSupportPreviewSize();

    protected abstract void openCamera();

    protected abstract void closeCamera();

    protected abstract int getCameraOrientation();

    public abstract int getPreviewFormat();
}
