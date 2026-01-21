package com.google.easyapp.cameraengine.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.easyapp.Constants;
import com.google.easyapp.R;
import com.google.easyapp.cameraengine.DrawInterface;
import com.google.easyapp.cameraengine.MutiLayerFilter;
import com.google.easyapp.cameraengine.OpenGlUtils;
import com.google.easyapp.cameraengine.core.BaseCameraEngine;
import com.google.easyapp.cameraengine.core.CameraCallBack;
import com.google.easyapp.cameraengine.core.CameraEngine;
import com.google.easyapp.cameraengine.core.CameraUtils;
import com.google.easyapp.cameraengine.core.Size;
import com.google.easyapp.cameraengine.core.gpuimage.GPUImage;
import com.google.easyapp.cameraengine.core.gpuimage.GPUImageFilter;
import com.google.easyapp.cameraengine.core.gpuimage.GPUImageView;
import com.google.easyapp.utils.BitmapUtils;
import com.google.easyapp.utils.ToastUtils;

import java.io.File;
import java.util.List;


public class CameraContainer extends FrameLayout implements CameraCallBack {
    private GPUImageView gpuImageView;
    private ImageView switchBtn;
    private BaseCameraEngine cameraEngine;
    private View takePicBtn;
    private DrawInfoView drawInfoView;

    private boolean openBackCamera;
    private int scaleType;

    private boolean refreshCanvas;

    private int cameraRotateAdjust;
    private boolean flipX = false;

    private static final int SCALE_TYPE_CENTER_CROP = 0;
    private static final int SCALE_TYPE_CENTER_INSIDE = 1;
    private TakePicCallBack onTakePicCallback;

    public CameraContainer(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CameraContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RendererView, 0, 0);
        scaleType = ta.getInt(R.styleable.RendererView_scaleType, SCALE_TYPE_CENTER_CROP);
        ta.recycle();
        ta = context.obtainStyledAttributes(attrs, R.styleable.CameraContainer, 0, 0);
        openBackCamera = ta.getBoolean(R.styleable.CameraContainer_openBackCamera, false);
        ta.recycle();
        View inflate = LayoutInflater.from(context).inflate(R.layout.include_camera_container, this, true);
        gpuImageView = inflate.findViewById(R.id.gpuImageView);

        switchBtn = inflate.findViewById(R.id.iv_switch);
        switchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraEngine.switchCamera();
            }
        });

        takePicBtn = inflate.findViewById(R.id.iv_take_pic);
        takePicBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = takePic();
                File file = BitmapUtils.savePhotoToSDCard(bitmap, Constants.getBitmapPath(), "IMG_" + System.currentTimeMillis() + ".jpg");
                if (file != null) {
                    if (onTakePicCallback != null) {
                        onTakePicCallback.OnTakePic(bitmap, file.getAbsolutePath());
                    }
                } else {
                    ToastUtils.showShort("保存文件失败啦");
                    BitmapUtils.recycleBitmap(bitmap);
                }
            }
        });
        drawInfoView = inflate.findViewById(R.id.drawPointView);


        cameraEngine = new CameraEngine(getContext());
        cameraEngine.addCameraCallBack(this);
        cameraEngine.setCameraId(openBackCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);

        showSwitchCamera(true);

        boolean isCenterCrop = (scaleType == SCALE_TYPE_CENTER_CROP);
        gpuImageView.setScaleType((isCenterCrop ? GPUImage.ScaleType.CENTER_CROP : GPUImage.ScaleType.CENTER_INSIDE));
        drawInfoView.setCenterCrop(isCenterCrop);

        MutiLayerFilter mutiLayerFilter = new MutiLayerFilter(new GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, OpenGlUtils.readShaderFromRawResource(R.raw.opaque_fragment)));
        gpuImageView.setFilter(mutiLayerFilter);
    }

    @Override
    public void openCameraSucceed(final BaseCameraEngine cameraEngine, int cameraId) {
        cameraEngine.startPreview();
    }

    @Override
    public void openCameraError(Exception e) {

    }

    public PointF getTextureDistance() {
        return gpuImageView.getGPUImage().getTextureDistance();
    }

    @Override
    public void onPreviewFrame(BaseCameraEngine baseCameraEngine, byte[] data) {
        Size previewSize = cameraEngine.getPreviewSize();
        // 特殊设备手动适配
        int cameraRotate = cameraEngine.getCameraRotate();
        cameraRotate += cameraRotateAdjust;
        cameraRotate += 360;
        cameraRotate %= 360;
        boolean flipHorizontal = cameraEngine.isFrontCamera();
        if (flipX) flipHorizontal = !flipHorizontal;

        gpuImageView.getGPUImage().onPreviewFrameNv21(data, previewSize.width, previewSize.height, cameraRotate, flipHorizontal);

    }

    @Deprecated
    public BaseCameraEngine getCameraEngine() {
        return cameraEngine;
    }

    public void setFilter(GPUImageFilter filter) {
        MutiLayerFilter mutiLayerFilter = new MutiLayerFilter();
        mutiLayerFilter.addFilter(filter);
        gpuImageView.setFilter(mutiLayerFilter);
    }

    public void addCameraCallBack(CameraCallBack cameraCallBack) {
        cameraEngine.addCameraCallBack(cameraCallBack);
    }

    public void removeCameraCallBack(CameraCallBack cameraCallBack) {
        cameraEngine.removeCameraCallBack(cameraCallBack);
    }

    public void setPreviewSize(int width, int height) {
        cameraEngine.setPreviewSize(new Size(width, height));
    }

    public void setCameraId(int cameraId) {
        cameraEngine.setCameraId(cameraId);
    }

    public void runOnCameraThread(Runnable runnable) {
        cameraEngine.runOnCameraThread(runnable);
    }

    public Size getPreviewSize() {
        return cameraEngine.getPreviewSize();
    }

    public int getCameraRotate() {
        return cameraEngine.getCameraRotate();
    }

    public boolean isFrontCamera() {
        return cameraEngine.isFrontCamera();
    }

    /**
     * 强制刷新上层绘制
     */
    public void refreshCanvasDraw() {
        Size previewSize = cameraEngine.getPreviewSize();
        int width = previewSize.width;
        int height = previewSize.height;
        if (gpuImageView.getGPUImage().isTranspose()) {
            width = previewSize.height;
            height = previewSize.width;
        }
        drawInfoView.postInvalidate(width, height);
    }

    public void showSwitchCamera(boolean show) {
        if (show && cameraEngine.getSupportCameraId().size() > 1) {
            switchBtn.setVisibility(VISIBLE);
        } else {
            switchBtn.setVisibility(GONE);
        }
    }

    public Bitmap takePic() {
        try {
            return gpuImageView.capture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void showTakePic(boolean show) {
        takePicBtn.setVisibility(show ? VISIBLE : GONE);
    }

    public void onResume() {
        cameraEngine.onResume();
    }

    public void onPause() {
        cameraEngine.onPause();
    }

    @Override
    protected void onDetachedFromWindow() {
        cameraEngine.removeCameraCallBack(this);
        super.onDetachedFromWindow();
        cameraEngine.onPause();
    }

    public void refreshConfig(UiConfig uiConfig) {
        showTakePic(uiConfig.showTakePic);
        this.onTakePicCallback = uiConfig.onTakePicCallback;
        showSwitchCamera(uiConfig.showSwitchCamera);

        this.refreshCanvas = uiConfig.refreshCanvas;
        this.cameraRotateAdjust = uiConfig.cameraRotateAdjust;
        this.flipX = uiConfig.flipX;
        drawInfoView.setDrawInterface(uiConfig.cameraDrawInterface);
    }

    public void requestRender() {
        gpuImageView.requestRender();
    }

    public interface TakePicCallBack {
        void OnTakePic(Bitmap bitmap, String path);
    }

    public static class UiConfig {
        private boolean showTakePic = true;
        private DrawInterface cameraDrawInterface;
        private boolean showSwitchCamera = true;
        private boolean showChangeImageQuality = true;
        private boolean refreshCanvas = true;
        public int cameraRotateAdjust = 0;
        public boolean flipX = false;
        private TakePicCallBack onTakePicCallback;

        /**
         * 显示拍照按钮
         *
         * @param show
         * @return
         */
        public UiConfig showTakePic(boolean show) {
            showTakePic = show;
            return this;
        }

        public UiConfig refreshCanvasWhenPointRefresh(boolean show) {
            refreshCanvas = show;
            return this;
        }

        /**
         * 显示拍照按钮切换分辨率
         *
         * @param show
         * @return
         */
        public UiConfig showChangeImageQuality(boolean show) {
            showChangeImageQuality = show;
            return this;
        }

        public UiConfig showSwitchCamera(boolean show) {
            showSwitchCamera = show;
            return this;
        }


        public UiConfig setOnTakePicCallback(TakePicCallBack onTakePicCallback) {
            this.onTakePicCallback = onTakePicCallback;
            return this;
        }


        /**
         * 自定义绘制
         */
        public UiConfig drawInfoCanvas(DrawInterface cameraDrawInterface) {
            this.cameraDrawInterface = cameraDrawInterface;
            return this;
        }

        public UiConfig setCameraRotateAdjust(int cameraRotateAdjust) {
            this.cameraRotateAdjust = cameraRotateAdjust;
            return this;
        }

        public UiConfig setFlipX(boolean flipX) {
            this.flipX = flipX;
            return this;
        }
    }
}