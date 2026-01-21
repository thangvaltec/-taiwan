package com.google.easyapp.ui.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.easyapp.R;
import com.google.easyapp.cameraengine.core.BaseCameraEngine;
import com.google.easyapp.cameraengine.core.CameraCallBack;
import com.google.easyapp.cameraengine.core.CameraDefine;
import com.google.easyapp.cameraengine.core.Size;
import com.google.easyapp.cameraengine.widget.CameraContainer;
import com.google.easyapp.ui.BaseActivity;
import com.google.easyapp.utils.L;
import com.google.easyapp.utils.ToastUtils;

import java.io.File;

public class TakePicActivity extends BaseActivity {
    public static final String KEY_RESULT = "DATA";
    public static int sUsedCameraId;
    public static Size sUsedCameraPreviewSize;

    public static final String KEY_CAMERA_ID = "KEY_CAMERA_ID";
    public static final String KEY_CAMERA_PREVIEW_SIZE = "KEY_CAMERA_PREVIEW_SIZE";
    public static final String KEY_CAMERA_ROTATE_ADJUST = "KEY_CAMERA_ROTATE_ADJUST";
    public static final String KEY_CAMERA_PREVIEW_FLIPX = "KEY_CAMERA_PREVIEW_FLIPX";
    CameraContainer cameraContainer;
    private View lvConfim;
    private String path;
    private ImageView resultImage;

    private CameraCallBack cameraCallBack = new CameraCallBack() {
        @Override
        public void openCameraError(Exception e) {
            ToastUtils.showShort(e.getMessage());
        }

        @Override
        public void onPreviewFrame(BaseCameraEngine cameraEngine, byte[] data) {
            cameraContainer.requestRender();
        }

        @Override
        public void openCameraSucceed(BaseCameraEngine cameraEngine, int cameraId) {
        }
    };

    @Override
    public int getLayout() {
        Window window = getWindow();
        //隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //设置当前窗体为全屏显示
        window.setFlags(flag, flag);
        return R.layout.activity_takepic;
    }

    @Override
    public void initView(View view) {
        lvConfim = findViewById(R.id.lv_confim);
        resultImage = findViewById(R.id.resultImage);
        cameraContainer = findViewById(R.id.surface);
        findViewById(R.id.cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
                path = "";
                lvConfim.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(KEY_RESULT, path);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        int cameraId = getIntent().getIntExtra(KEY_CAMERA_ID, CameraDefine.CAMERA_ID_BACK);
        Size previewSize = (Size) getIntent().getSerializableExtra(KEY_CAMERA_PREVIEW_SIZE);
        if (previewSize == null) {
            previewSize = new Size(1280, 720);
        }
        sUsedCameraId = cameraId;
        sUsedCameraPreviewSize = new Size(previewSize);
        L.d(TAG, "cameraId=" + cameraId + " previewSize=" + previewSize);
        cameraContainer.setCameraId(cameraId);
        cameraContainer.setPreviewSize(previewSize.width, previewSize.height);
        cameraContainer.addCameraCallBack(cameraCallBack);

        CameraContainer.UiConfig uiConfig = new CameraContainer.UiConfig().showTakePic(true).showChangeImageQuality(false).refreshCanvasWhenPointRefresh(true).setCameraRotateAdjust(getIntent().getIntExtra(KEY_CAMERA_ROTATE_ADJUST, 0)) // 特殊设备手动适配
                .setFlipX(getIntent().getBooleanExtra(KEY_CAMERA_PREVIEW_FLIPX, false)) // 特殊设备手动适配
                .setOnTakePicCallback(new CameraContainer.TakePicCallBack() {


                    @Override
                    public void OnTakePic(final Bitmap bitmap, final String path) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TakePicActivity.this.path = path;
                                lvConfim.setVisibility(View.VISIBLE);
                                resultImage.setImageBitmap(bitmap);
                            }
                        });
                    }
                });
        cameraContainer.refreshConfig(uiConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraContainer.onResume();
    }

    @Override
    protected void onDestroy() {
        cameraContainer.removeCameraCallBack(cameraCallBack);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        sUsedCameraId = (cameraContainer.isFrontCamera() ? CameraDefine.CAMERA_ID_FRONT : CameraDefine.CAMERA_ID_BACK);
        sUsedCameraPreviewSize = new Size(cameraContainer.getPreviewSize());
        cameraContainer.onPause();
    }

    @Override
    public void doBusiness(Context mContext) {
    }


}
