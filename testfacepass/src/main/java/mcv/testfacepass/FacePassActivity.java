package mcv.testfacepass;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import mcv.facepass.FacePassException;
import mcv.facepass.types.FacePassConfig;
import mcv.facepass.types.FacePassImage;
import mcv.facepass.types.FacePassImageRotation;
import mcv.facepass.types.FacePassImageType;
import mcv.facepass.types.FacePassLivenessMode;
import mcv.facepass.types.FacePassLivenessResult;
import mcv.facepass.types.FacePassRCAttribute;
import mcv.facepass.types.FacePassRecogMode;
import mcv.facepass.types.FacePassRecognitionResult;
import mcv.facepass.types.FacePassRecognitionState;
import mcv.facepass.types.FacePassTrackOptions;
import mcv.facepass.types.FacePassTrackResult;
import mcv.facepass.types.FacePassTrackedFace;
import mcv.testfacepass.camera.CameraManager;
import mcv.testfacepass.camera.CameraPreview;
import mcv.testfacepass.camera.CameraPreviewData;
import mcv.testfacepass.camera.ComplexFrameHelper;
import mcv.testfacepass.data.DatabaseHelper;
import mcv.testfacepass.utils.FacePassManager;

public class FacePassActivity extends Activity implements CameraManager.CameraListener {


    private static final String DEBUG_TAG = "FacePassDemo";
    private static final String FD_DEBUG_TAG = "FeedFrameDemo";
    private static final String RG_DEBUG_TAG = "RecognizeDemo";


    /* 程序所需权限 ：相机 文件存储 网络访问 */
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    private String[] Permission = new String[]{PERMISSION_CAMERA, PERMISSION_WRITE_STORAGE, PERMISSION_READ_STORAGE, PERMISSION_INTERNET, PERMISSION_ACCESS_NETWORK_STATE};


    /* 相机实例 */
    private CameraManager manager;
    private CameraManager mIRCameraManager;


    /* 显示faceId */
    private TextView faceEndTextView;

    /* 相机预览界面 */
    private CameraPreview cameraView;
    private CameraPreview mIRCameraView;


    /* 在预览界面圈出人脸 */
    private FaceView faceView;

    private ScrollView scrollView;

    /* 相机是否使用前置摄像头 */
    private static boolean cameraFacingFront = true;

    private int cameraRotation = 270;

    private static final int cameraWidth = 1280;
    private static final int cameraHeight = 720;

    private int mSecretNumber = 0;
    private static final long CLICK_INTERVAL = 600;
    private long mLastClickTime;

    private int heightPixels;
    private int widthPixels;

    int screenState = 0;// 0 横 1 竖

    LinearLayout ll;
    FrameLayout frameLayout;
    private Button settingButton;
    /*Toast*/
    private Toast mRecoToast;

    /*DetectResult queue*/
    ArrayBlockingQueue<RecognizeData> mDetectResultQueue;
    ArrayBlockingQueue<Long> mstartTimeQueue;

    /*recognize thread*/
    RecognizeThread mRecognizeThread;
    FeedFrameThread mFeedFrameThread;


    private Handler mAndroidHandler;

    private DatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDetectResultQueue = new ArrayBlockingQueue<RecognizeData>(5);
        mstartTimeQueue = new ArrayBlockingQueue<Long>(5);
        initAndroidHandler();

        /* 初始化界面 */
        initView();


        mRecognizeThread = new RecognizeThread();
        mRecognizeThread.start();
        mFeedFrameThread = new FeedFrameThread();
        mFeedFrameThread.start();

        dbHelper = new DatabaseHelper(this);
    }

    private void initAndroidHandler() {
        mAndroidHandler = new Handler();
    }


    @Override
    protected void onResume() {
        FacePassManager.getInstance().checkGroup(this);
        initToast();
        /* 打开相机 */
        if (hasPermission()) {
            manager.open(getWindowManager(), false, cameraWidth, cameraHeight);
            mIRCameraManager.open(getWindowManager(), true, cameraWidth, cameraHeight);
        }
        adaptFrameLayout();
        super.onResume();
    }


    /* 判断程序是否有所需权限 android22以上需要自申请权限 */
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_READ_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_INTERNET) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /* 相机回调函数 */
    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {
        ComplexFrameHelper.addRgbFrame(cameraPreviewData);
    }

    private class FeedFrameThread extends Thread {
        boolean isInterrupt;

        @Override
        public void run() {
            while (!isInterrupt) {
                Pair<CameraPreviewData, CameraPreviewData> framePair;
                try {
                    framePair = ComplexFrameHelper.takeComplexFrame();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                if (FacePassManager.mFacePassHandler == null) {
                    Log.d(DEBUG_TAG, " FacePassManager.mFacePassHandler == null !");
                    continue;
                }
                /* 将相机预览帧转成SDK算法所需帧的格式 FacePassImage */
                long startTime = System.currentTimeMillis(); //起始时间

                /* 将每一帧FacePassImage 送入SDK算法， 并得到返回结果 */
                FacePassTrackResult detectionResult = null;
                try {
                    FacePassConfig cfg = FacePassManager.mFacePassHandler.getConfig();
                    if (cfg.rgbIrLivenessEnabled) {
                        FacePassImage imageRGB = new FacePassImage(framePair.first.nv21Data, framePair.first.width, framePair.first.height, cameraRotation, FacePassImageType.NV21);
                        FacePassImage imageIR = new FacePassImage(framePair.second.nv21Data, framePair.second.width, framePair.second.height, cameraRotation, FacePassImageType.NV21);
                        detectionResult = FacePassManager.mFacePassHandler.feedFrameRGBIR(imageRGB, imageIR);
                    } else {
                        FacePassImage imageRGB = new FacePassImage(framePair.first.nv21Data, framePair.first.width, framePair.first.height, cameraRotation, FacePassImageType.NV21);
                        detectionResult = FacePassManager.mFacePassHandler.feedFrame(imageRGB);
                    }
                } catch (FacePassException e) {
                    e.printStackTrace();
                    continue;
                }


                if (detectionResult == null || detectionResult.trackedFaces.length == 0) {
                    /* 当前帧没有检出人脸 */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faceView.clear();
                            faceView.invalidate();
                        }
                    });
                } else {
                    /* 将识别到的人脸在预览界面中圈出，并在上方显示人脸位置及角度信息 */
                    final FacePassTrackedFace[] bufferFaceList = detectionResult.trackedFaces;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFacePassFace(bufferFaceList);
                        }
                    });
                }

                if (FacePassManager.SDK_MODE == FacePassManager.FacePassSDKMode.MODE_OFFLINE) {
//                    Log.d(DEBUG_TAG, "detectionResult.message.length = " + detectionResult.message.length);
                    /*离线模式，将识别到人脸的，message不为空的result添加到处理队列中*/
                    if (detectionResult != null) {
                        /*所有检测到的人脸框的属性信息*/
                        for (int i = 0; i < detectionResult.trackedFaces.length; ++i) {
                            if (!detectionResult.trackedFaces[i].quality.facePassQualityCheck.isBlurPassed) {
                                Log.d(FD_DEBUG_TAG, "BlurScore = " + detectionResult.trackedFaces[i].quality.blur);
                            }
                            if (!detectionResult.trackedFaces[i].quality.facePassQualityCheck.isBrightnessPassd) {
                                Log.d(FD_DEBUG_TAG, "BrightnessScore = " + detectionResult.trackedFaces[i].quality.brightness);
                                Log.d(FD_DEBUG_TAG, "DeviationScore = " + detectionResult.trackedFaces[i].quality.deviation);
                            }
                            if (!detectionResult.trackedFaces[i].quality.facePassQualityCheck.isEdgefacePassed) {
                                Log.d(FD_DEBUG_TAG, "EdgefacePassedScore = " + detectionResult.trackedFaces[i].quality.edgefaceComp);
                            }
                            if (!detectionResult.trackedFaces[i].quality.facePassQualityCheck.isYawPassed ||
                                    !detectionResult.trackedFaces[i].quality.facePassQualityCheck.isPitchPassed ||
                                    !detectionResult.trackedFaces[i].quality.facePassQualityCheck.isRollPassed) {
                                Log.d(FD_DEBUG_TAG, "YawScore = " + detectionResult.trackedFaces[i].quality.pose.yaw);
                                Log.d(FD_DEBUG_TAG, "PitchScore = " + detectionResult.trackedFaces[i].quality.pose.pitch);
                                Log.d(FD_DEBUG_TAG, "RollScore = " + detectionResult.trackedFaces[i].quality.pose.roll);
                            }
                        }
                        Log.d(DEBUG_TAG, "--------------------------------------------------------------------------------------------------------------------------------------------------");
                        if (detectionResult.message.length != 0) {
                            /*送识别的人脸框的属性信息*/
                            FacePassTrackOptions[] trackOpts = new FacePassTrackOptions[detectionResult.images.length];
                            for (int i = 0; i < detectionResult.images.length; ++i) {
                                if (detectionResult.images[i].rcAttr.respiratorType != FacePassRCAttribute.FacePassRespiratorType.NO_RESPIRATOR) {
                                    float searchThreshold = 60f;
                                    float livenessThreshold = 88f; // -1.0f will not change the liveness threshold
                                    trackOpts[i] = new FacePassTrackOptions(detectionResult.images[i].trackId, searchThreshold, livenessThreshold);
                                } else {
                                    trackOpts[i] = new FacePassTrackOptions(detectionResult.images[i].trackId, -1f, -1f);
                                }
                                Log.d(DEBUG_TAG, String.format("rc attribute in FacePassImage, hairType: 0x%x beardType: 0x%x hatType: 0x%x respiratorType: 0x%x glassesType: 0x%x skinColorType: 0x%x",
                                        detectionResult.images[i].rcAttr.hairType.ordinal(),
                                        detectionResult.images[i].rcAttr.beardType.ordinal(),
                                        detectionResult.images[i].rcAttr.hatType.ordinal(),
                                        detectionResult.images[i].rcAttr.respiratorType.ordinal(),
                                        detectionResult.images[i].rcAttr.glassesType.ordinal(),
                                        detectionResult.images[i].rcAttr.skinColorType.ordinal()));
                            }
                            Log.d(DEBUG_TAG, "mRecognizeDataQueue.offer(mRecData);");
                            RecognizeData mRecData = new RecognizeData(detectionResult.message, trackOpts);
                            mDetectResultQueue.offer(mRecData);
                            Log.d(DEBUG_TAG, " startTime " + startTime);
                            mstartTimeQueue.offer(startTime);
                        }
                    }
                }
                long endTime = System.currentTimeMillis(); //结束时间
                long runTime = endTime - startTime;
                if (detectionResult == null) {
                    Log.d(DEBUG_TAG, "detectionResult == null");
                    continue;
                }
                for (int i = 0; i < detectionResult.trackedFaces.length; ++i) {
                    Log.i(DEBUG_TAG, "rect[" + i + "] = (" + detectionResult.trackedFaces[i].rect.left + ", " + detectionResult.trackedFaces[i].rect.top + ", " + detectionResult.trackedFaces[i].rect.right + ", " + detectionResult.trackedFaces[i].rect.bottom);
                }
                Log.i("]time", String.format("feedfream %d ms", runTime));
            }
        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }


    private class RecognizeThread extends Thread {
        boolean isInterrupt;

        @Override
        public void run() {
            while (!isInterrupt) {
                try {
                    RecognizeData recognizeData = mDetectResultQueue.take();
                    long startTime = mstartTimeQueue.take();
                    Log.d(DEBUG_TAG, "mDetectResultQueue.FacePassManager.isLocalGroupExist");
                    if (FacePassManager.isLocalGroupExist) {


					///////先活体再recognize
                        ////////////////////////// live ness test/ FP_REG_MODE_LIVENESS ////////////FP_REG_MODE_LIVENESSTRACK //////////
                        Log.d(DEBUG_TAG, "mFacePassHandler.livenessClassify");
                        FacePassLivenessResult[] livenessResult = FacePassManager.mFacePassHandler.livenessClassify(recognizeData.message, recognizeData.trackOpt[0].trackId, FacePassLivenessMode.FP_REG_MODE_LIVENESS, recognizeData.trackOpt[0].livenessThreshold);
                        if (null != livenessResult && livenessResult.length > 0) {
                            Log.d(DEBUG_TAG, "FacePassLivenessResult length = " + livenessResult.length);
                            for (FacePassLivenessResult result : livenessResult) {
                                String slivenessStat = " Unkonw";
                                switch (result.livenessState) {
                                    case 0:
                                        slivenessStat = "LIVENESS_PASS";
                                        break;
                                    case 1:
                                        slivenessStat = "LIVENESS_RETRY";
                                        break;
                                    case 2:
                                        slivenessStat = "LIVENESS_RETRY_EXPIRED";
                                        break;
                                    case 3:
                                        slivenessStat = "LIVENESS_TRACK_MISSING";
                                        break;
                                    case 4:
                                        slivenessStat = "LIVENESS_UNPASS";
                                        break;
                                    default:
                                        break;
                                }

                                Log.d(DEBUG_TAG, "FacePassLivenessResult: trackId: " + result.trackId
                                        + ", livenessScore: " + result.livenessScore
                                        + ", livenessThreshold: " + result.livenessThreshold
                                        + ", livenessState: " + slivenessStat + ", livenessState: " + result.livenessState);
                            }
                        }
						Log.d(DEBUG_TAG, "mDetectResultQueue.recognize");
                    	FacePassRecognitionResult[] recognizeResult = FacePassManager.mFacePassHandler.recognize(FacePassManager.group_name, recognizeData.message, 1, recognizeData.trackOpt[0].trackId, FacePassRecogMode.FP_REG_MODE_FEAT_COMP, -1.0F, -1.0F);

                    	if (recognizeResult != null && recognizeResult.length > 0) {
                    	Log.d(DEBUG_TAG, "FacePassRecognitionResult length = " + recognizeResult.length);
                        for (FacePassRecognitionResult result : recognizeResult) {
                        	if (null == result.faceToken) {
                        		Log.d(DEBUG_TAG, "result.faceToken is null.");
                            	continue;
                        	}
                            String faceToken = new String(result.faceToken);
                            Log.d(DEBUG_TAG, "FacePassRecognitionState.RECOGNITION_PASS = " + result.recognitionState);
                            if (FacePassRecognitionState.RECOGNITION_PASS == result.recognitionState) {
                            	getFaceImageByFaceToken(result.trackId, faceToken);
                            }
                            showRecognizeResult(result.trackId, result.detail.searchScore, result.detail.livenessScore, !TextUtils.isEmpty(faceToken));
                        }
                    }



                        ////////////////////////////////////////////////////////////
//                        FacePassRecognitionResult[] recognizeResult = FacePassManager.mFacePassHandler.recognize(FacePassManager.group_name, recognizeData.message, 1, recognizeData.trackOpt[0].trackId, FacePassRecogMode.FP_REG_MODE_DEFAULT, recognizeData.trackOpt[0].livenessThreshold, recognizeData.trackOpt[0].searchThreshold);
//                        long endTime = System.currentTimeMillis(); //结束时间
//                        long runTime = endTime - startTime;
//                        Log.i("]time", String.format("ppl time %d ms", runTime));
//                        if (recognizeResult != null && recognizeResult.length > 0) {
//                            Log.d(DEBUG_TAG, "FacePassRecognitionResult length = " + recognizeResult.length);
//                            for (FacePassRecognitionResult result : recognizeResult) {
//                                if (null == result.faceToken) {
//                                    Log.d(DEBUG_TAG, "result.faceToken is null.");
//                                    continue;
//                                }
//                                String faceToken = new String(result.faceToken);
//                                Log.d(DEBUG_TAG, "FacePassRecognitionState.RECOGNITION_PASS = " + result.recognitionState);
//                                if (FacePassRecognitionState.RECOGNITION_PASS == result.recognitionState) {
//                                    getFaceImageByFaceToken(result.trackId, faceToken);
//                                }
//                                showRecognizeResult(result.trackId, result.detail.searchScore, result.detail.livenessScore, result.recognitionState == 0);
//                            }
//                        }
                    }
                } catch (InterruptedException | FacePassException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }

    private void showRecognizeResult(final long trackId, final float searchScore, final float livenessScore, final boolean isRecognizeOK) {
        mAndroidHandler.post(new Runnable() {
            @Override
            public void run() {
                faceEndTextView.append("ID = " + trackId + (isRecognizeOK ? "识别成功" : "识别失败") + "\n");
                faceEndTextView.append("识别分 = " + searchScore + "\n");
                faceEndTextView.append("活体分 = " + livenessScore + "\n");
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }


    private void adaptFrameLayout() {
        SettingVar.isButtonInvisible = false;
        SettingVar.iscameraNeedConfig = false;
    }

    private void initToast() {
        SettingVar.isButtonInvisible = false;
    }

    private void initView() {


//        int windowRotation = ((WindowManager) (getApplicationContext().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getRotation() * 90;
//        if (windowRotation == 0) {
//            cameraRotation = FacePassImageRotation.DEG90;
//        } else if (windowRotation == 90) {
//            cameraRotation = FacePassImageRotation.DEG0;
//        } else if (windowRotation == 270) {
//            cameraRotation = FacePassImageRotation.DEG180;
//        } else {
//            cameraRotation = FacePassImageRotation.DEG270;
//        }
        Log.i(DEBUG_TAG, "cameraRation: " + cameraRotation);
        cameraFacingFront = true;
        SharedPreferences preferences = getSharedPreferences(SettingVar.SharedPrefrence, Context.MODE_PRIVATE);
        SettingVar.isSettingAvailable = preferences.getBoolean("isSettingAvailable", SettingVar.isSettingAvailable);
        SettingVar.isCross = preferences.getBoolean("isCross", SettingVar.isCross);
        SettingVar.faceRotation = preferences.getInt("faceRotation", SettingVar.faceRotation);
        SettingVar.cameraPreviewRotation = preferences.getInt("cameraPreviewRotation", SettingVar.cameraPreviewRotation);
        SettingVar.cameraFacingFront = preferences.getBoolean("cameraFacingFront", SettingVar.cameraFacingFront);
        if (SettingVar.isSettingAvailable) {
            cameraRotation = SettingVar.faceRotation;
            cameraFacingFront = SettingVar.cameraFacingFront;
        }


//        Log.i("orientation", String.valueOf(windowRotation));
        final int mCurrentOrientation = getResources().getConfiguration().orientation;

        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            screenState = 1;
        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenState = 0;
        }
        setContentView(R.layout.activity_facepass);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        heightPixels = displayMetrics.heightPixels;
        widthPixels = displayMetrics.widthPixels;
        SettingVar.mHeight = heightPixels;
        SettingVar.mWidth = widthPixels;
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        AssetManager mgr = getAssets();
        Typeface tf = Typeface.createFromAsset(mgr, "fonts/Univers LT 57 Condensed.ttf");
        /* 初始化界面 */
        faceEndTextView = (TextView) this.findViewById(R.id.tv_meg2);
        faceEndTextView.setTypeface(tf);
        faceView = (FaceView) this.findViewById(R.id.fcview);
        settingButton = (Button) this.findViewById(R.id.settingid);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long curTime = System.currentTimeMillis();
                long durTime = curTime - mLastClickTime;
                mLastClickTime = curTime;
                if (durTime < CLICK_INTERVAL) {
                    ++mSecretNumber;
                    if (mSecretNumber == 5) {
                        Intent intent = new Intent(FacePassActivity.this, SettingActivity.class);
                        startActivity(intent);
                        FacePassActivity.this.finish();
                    }
                } else {
                    mSecretNumber = 0;
                }
            }
        });
        SettingVar.cameraSettingOk = false;
        ll = (LinearLayout) this.findViewById(R.id.ll);
        ll.getBackground().setAlpha(100);

        manager = new CameraManager();
        mIRCameraManager = new CameraManager();

        cameraView = (CameraPreview) findViewById(R.id.preview);
        mIRCameraView = (CameraPreview) findViewById(R.id.preview2);

        manager.setPreviewDisplay(cameraView);
        mIRCameraManager.setPreviewDisplay(mIRCameraView);

        frameLayout = (FrameLayout) findViewById(R.id.frame);
        /* 注册相机回调函数 */
        manager.setListener(this);
        mIRCameraManager.setListener(new CameraManager.CameraListener() {
            @Override
            public void onPictureTaken(CameraPreviewData cameraPreviewData) {

                ComplexFrameHelper.addIRFrame(cameraPreviewData);
            }
        });

        findViewById(R.id.ivGoBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    @Override
    protected void onStop() {
        SettingVar.isButtonInvisible = false;
        mDetectResultQueue.clear();
        if (manager != null) {
            manager.release();
        }
        if (mIRCameraManager != null) {
            mIRCameraManager.release();
        }
        super.onStop();
    }

    @Override
    protected void onRestart() {
        faceView.clear();
        faceView.invalidate();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        mRecognizeThread.isInterrupt = true;
        mFeedFrameThread.isInterrupt = true;

        mRecognizeThread.interrupt();
        mFeedFrameThread.interrupt();

        if (manager != null) {
            manager.release();
        }
        if (mIRCameraManager != null) {
            mIRCameraManager.release();
        }
        if (mAndroidHandler != null) {
            mAndroidHandler.removeCallbacksAndMessages(null);
        }


        super.onDestroy();
    }


    private void showFacePassFace(FacePassTrackedFace[] detectResult) {
        faceView.clear();
        for (FacePassTrackedFace face : detectResult) {
            Log.d("facefacelist", "width " + (face.rect.right - face.rect.left) + " height " + (face.rect.bottom - face.rect.top));
//            Log.d("facefacelist", "smile " + face.smile);
            boolean mirror = cameraFacingFront; /* 前摄像头时mirror为true */
            StringBuilder faceIdString = new StringBuilder();
            String name = face.trackId + "";
            faceIdString.append("ID = ").append(name);
            SpannableString faceViewString = new SpannableString(faceIdString);
            faceViewString.setSpan(new TypefaceSpan("fonts/kai"), 0, faceViewString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            StringBuilder faceRollString = new StringBuilder();
            faceRollString.append("旋转: ").append((int) face.quality.pose.roll).append("°");
            StringBuilder facePitchString = new StringBuilder();
            facePitchString.append("上下: ").append((int) face.quality.pose.pitch).append("°");
            StringBuilder faceYawString = new StringBuilder();
            faceYawString.append("左右: ").append((int) face.quality.pose.yaw).append("°");
            StringBuilder faceBlurString = new StringBuilder();
            faceBlurString.append("模糊: ").append(face.quality.blur);
            StringBuilder smileString = new StringBuilder();
//            smileString.append("微笑: ").append(String.format("%.6f", face.smile));
            Matrix mat = new Matrix();
            int w = cameraView.getMeasuredWidth();
            int h = cameraView.getMeasuredHeight();

            int cameraHeight = manager.getCameraheight();
            int cameraWidth = manager.getCameraWidth();

            float left = 0;
            float top = 0;
            float right = 0;
            float bottom = 0;
            switch (cameraRotation) {
                case 0:
                    left = face.rect.left;
                    top = face.rect.top;
                    right = face.rect.right;
                    bottom = face.rect.bottom;
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraWidth : 0f, 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    break;
                case 90:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = face.rect.top;
                    top = cameraWidth - face.rect.right;
                    right = face.rect.bottom;
                    bottom = cameraWidth - face.rect.left;
                    break;
                case 180:
                    mat.setScale(1, mirror ? -1 : 1);
                    mat.postTranslate(0f, mirror ? (float) cameraHeight : 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    left = face.rect.right;
                    top = face.rect.bottom;
                    right = face.rect.left;
                    bottom = face.rect.top;
                    break;
                case 270:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = cameraHeight - face.rect.bottom;
                    top = face.rect.left;
                    right = cameraHeight - face.rect.top;
                    bottom = face.rect.right;
            }

            RectF drect = new RectF();
            RectF srect = new RectF(left, top, right, bottom);

            mat.mapRect(drect, srect);
            faceView.addRect(drect);
            faceView.addId(faceIdString.toString());
            faceView.addRoll(faceRollString.toString());
            faceView.addPitch(facePitchString.toString());
            faceView.addYaw(faceYawString.toString());
            faceView.addBlur(faceBlurString.toString());
            faceView.addSmile(smileString.toString());
        }
        faceView.invalidate();
    }

    public void showToast(CharSequence text, int duration, boolean isSuccess, Bitmap bitmap) {
        LayoutInflater inflater = getLayoutInflater();
        View toastView = inflater.inflate(R.layout.toast, null);
        LinearLayout toastLLayout = (LinearLayout) toastView.findViewById(R.id.toastll);
        if (toastLLayout == null) {
            return;
        }
        toastLLayout.getBackground().setAlpha(100);
        ImageView imageView = (ImageView) toastView.findViewById(R.id.toastImageView);
        TextView idTextView = (TextView) toastView.findViewById(R.id.toastTextView);
        TextView stateView = (TextView) toastView.findViewById(R.id.toastState);
        SpannableString s;
        if (isSuccess) {
            s = new SpannableString("验证成功");
            imageView.setImageResource(R.drawable.success);
        } else {
            s = new SpannableString("验证失败");
            imageView.setImageResource(R.drawable.success);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
        stateView.setText(s);
        idTextView.setText(text);

        if (mRecoToast == null) {
            mRecoToast = new Toast(getApplicationContext());
            mRecoToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        }
        mRecoToast.setDuration(duration);
        mRecoToast.setView(toastView);

        mRecoToast.show();
    }

    private static final int REQUEST_CODE_CHOOSE_PICK = 1;


    private void getFaceImageByFaceToken(final long trackId, final String faceToken) {
        if (TextUtils.isEmpty(faceToken)) {
            return;
        }

        try {
            final Bitmap bitmap = FacePassManager.mFacePassHandler.getFaceImage(faceToken.getBytes());
            mAndroidHandler.post(new Runnable() {
                @Override
                public void run() {
                    String name = dbHelper.findName(faceToken);
                    Log.i(DEBUG_TAG, "getFaceImageByFaceToken:showToast");
                    showToast("姓名 = " + name, Toast.LENGTH_SHORT, true, bitmap);
                }
            });
            if (bitmap != null) {
                return;
            }
        } catch (FacePassException e) {
            e.printStackTrace();
        }
    }



    public class RecognizeData {
        public byte[] message;
        public FacePassTrackOptions[] trackOpt;

        public RecognizeData(byte[] message) {
            this.message = message;
            this.trackOpt = null;
        }

        public RecognizeData(byte[] message, FacePassTrackOptions[] opt) {
            this.message = message;
            this.trackOpt = opt;
        }
    }

}
