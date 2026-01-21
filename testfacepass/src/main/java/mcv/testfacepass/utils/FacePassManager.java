package mcv.testfacepass.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.auth.FacePassAuthCode;
import mcv.facepass.types.FacePassConfig;
import mcv.facepass.types.FacePassModel;
import mcv.facepass.types.FacePassPose;
import mcv.testfacepass.R;

public class FacePassManager {

    private static final String DEBUG_TAG = "FacePassDemo";
    private static final String FD_DEBUG_TAG = "FeedFrameDemo";
    private static final String RG_DEBUG_TAG = "RecognizeDemo";

    public static final String FILE_ROOT_PATH = "/sdcard/Download";

    /* 人脸识别Group */
    public static String group_name = "facepass";

    public static Boolean isLocalGroupExist = false;
    private ProgressDialog progressDialog;

    public enum FacePassSDKMode {
        MODE_ONLINE,
        MODE_OFFLINE
    }

    public enum FacePassAuthMode {
        FACEPASS_AUTH_CHIP,
        FACEPASS_AUTH_MCVSAFE,
        FACEPASS_AUTH_FACEPPLUS,
        FACEPASS_AUTH_ALGOMALL
    }

    public static FacePassSDKMode SDK_MODE = FacePassSDKMode.MODE_OFFLINE;


    /* SDK 实例对象 */
    public static FacePassHandler mFacePassHandler;

    // mcvface 授权打开该注释
    public static final String authIP = "https://api-cn.faceplusplus.com";
    public static final String apiKey = "KyYYa4n6d-N0J6pAnB-uBM8BXBAsYQXo";
    public static final String apiSecret = "5scLZqrawhqxrnTTEGGeieub0cai20c4";

    // 默认授权相关
    public static final String FACE_CERT_PATH = "/Download/CBG_Panel_Face_Reco_MiniCapacity---30-Trial-one-stage.cert";
    public static final String FACE_ACTIVE_PATH = "/Download/CBG_Android_Face_Reco---30-Trial--1-active.txt";
    public static String FACE_ALGOMALL_CERT_PATH = "/Download/RK3568-test-20241127.cert";

    private String modelsPath = "";
    // 单例实例
    private static volatile FacePassManager instance;


    public static FacePassManager getInstance() {
        if (instance == null) {
            synchronized (FacePassManager.class) {
                if (instance == null) {
                    instance = new FacePassManager();
                }
            }
        }
        return instance;
    }

    public void init(final Context context) {
        // 创建并显示加载对话框
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("SDK正在初始化...");
        progressDialog.setCancelable(false); // 不可取消
        progressDialog.show();



        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initFacePassSDK(context);
                    // 3秒后自动关闭
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private boolean initFacePassSDK(Context context) throws IOException {
        modelsPath = context.getExternalFilesDir(null).getAbsolutePath() + "/models";
        FACE_ALGOMALL_CERT_PATH = modelsPath + "/facepass_test.cert";
        if (!new File(FACE_ALGOMALL_CERT_PATH).exists()) {
            FileUtil.copyAssetsToInternal(context, "models", modelsPath);
        }

        Log.d("MainActivity", "FACE_ALGOMALL_CERT_PATH: " + FACE_ALGOMALL_CERT_PATH);
        Context ctx = context.getApplicationContext();
        FacePassHandler.initSDK(ctx, "");
        Log.d(DEBUG_TAG, FacePassHandler.getVersion());

        auth(ctx, FacePassAuthMode.FACEPASS_AUTH_ALGOMALL);
        boolean ret = FacePassHandler.isAvailable();
        if (ret) {
            createHandler(context);
        } else {
            Log.d(DEBUG_TAG, "Face auth result : failed.");
            return false;
        }

        return true;
    }


    private boolean auth(Context ctx, FacePassAuthMode mode) {
        if (mode == FacePassAuthMode.FACEPASS_AUTH_CHIP) {
            Log.d(DEBUG_TAG, "face chip success.");
            return true;
        } else if (mode == FacePassAuthMode.FACEPASS_AUTH_FACEPPLUS) {
            // mcvface
            int ret = FacePassHandler.prepare_facepplus(ctx, modelsPath + "/license.bin"); //, "license.bin"
            if (ret != FacePassAuthCode.FP_AUTH_OK) {
                return false;
            }

            if (!FacePassHandler.checkAuth_facepplus()) {
                ret = FacePassHandler.auth_facepplus(authIP, apiKey, apiSecret, true);
                if (ret != FacePassAuthCode.FP_AUTH_OK) {
                    return false;
                }
            }

            Log.d(DEBUG_TAG, "face mcvface success.");
            return true;
        } else if (mode == FacePassAuthMode.FACEPASS_AUTH_MCVSAFE) {
            // single certification
            {
                String cert = FileUtil.readExternal(FACE_CERT_PATH).trim();
                if (TextUtils.isEmpty(cert)) {
                    Log.d(DEBUG_TAG, "face mcvsafe single certification is null");
                    return false;
                }

                if (!FacePassHandler.checkAuth_mcvsafe()) {
                    int ret = FacePassHandler.auth_mcvsafe(cert, "");
                    if (ret != FacePassAuthCode.FP_AUTH_OK) {
                        Log.e(DEBUG_TAG, "face mcvsafe single certification failed, error: " + ret);
                    }
                }

                Log.d(DEBUG_TAG, "face mcvsafe single certification success.");
            }

            // double certification
            {
                String cert = FileUtil.readExternal(FACE_CERT_PATH).trim();
                String active = FileUtil.readExternal(FACE_ACTIVE_PATH).trim();
                if (TextUtils.isEmpty(cert) || TextUtils.isEmpty(active)) {
                    Log.d(DEBUG_TAG, "face double cert or active is null");
                    return false;
                }

                if (!FacePassHandler.checkAuth_mcvsafe()) {
                    int ret = FacePassHandler.auth_mcvsafe(cert, active);
                    if (ret != FacePassAuthCode.FP_AUTH_OK) {
                        Log.e(DEBUG_TAG, "face mcvsafe double certification failed, error: " + ret);
                    }
                }

                Log.d(DEBUG_TAG, "face mcvsafe double certification success.");
            }

            return true;
        } else if (mode == FacePassAuthMode.FACEPASS_AUTH_ALGOMALL) {
            // 算法商城单独授权
            {
                String cert = FileUtil.readExternal(FACE_ALGOMALL_CERT_PATH).trim();
                if (TextUtils.isEmpty(cert)) {
                    Log.d(DEBUG_TAG, "face algo mall cert is null");
                    return false;
                }

                int ret = FacePassHandler.auth_algomall(cert);
                if (ret != FacePassAuthCode.FP_AUTH_OK) {
                    return false;
                }

                Log.d(DEBUG_TAG, "face algo mall success.");
            }

            // 已有的金雅拓授权转化算法商城授权
//            {
//                String mcvsafecert = FileUtil.readExternal(FACE_CERT_PATH).trim();
//                if (TextUtils.isEmpty(mcvsafecert)) {
//                    Log.d(DEBUG_TAG, "face mcvsafe cert is null");
//                    return false;
//                }
//
//                String algomallcert = FileUtil.readExternal(FACE_ALGOMALL_CERT_PATH).trim();
//                if (TextUtils.isEmpty(algomallcert)) {
//                    Log.d(DEBUG_TAG, "face algo mall cert is null");
//                    return false;
//                }
//
//                int ret = FacePassHandler.auth_mcvsafe2algomall(mcvsafecert, algomallcert);
//                if (ret != FacePassAuthCode.FP_AUTH_OK) {
//                    return false;
//                }
//
//                Log.d(DEBUG_TAG, "face mcvsafe convert algo mall auth success.");
//            }

            return true;
        }

        return false;
    }


    private void createHandler(final Context context) {
        new Thread() {
            @Override
            public void run() {
//                while (!isFinishing()) {
                while (FacePassHandler.isAvailable()) {
                    synchronized (FacePassHandler.class) {
                        while (!FacePassHandler.isAvailable()) {
                            try {
                                FacePassHandler.class.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return;
                            }
                        }
                    }

                    try {
                        /* 填入所需要的配置 */
                        FacePassConfig config = new FacePassConfig();
                        config.rgbIrLivenessModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_liveness_A));
                        config.LivenessModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_livenessrgb_A));
                        config.searchModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_feature_Ari));
                        config.poseBlurModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_poseblur_A));
                        config.postFilterModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_postfilter_A));
                        config.rcAttributeModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_rc_attribute_A));
                        config.detectModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_rk3568_det_A_det));
                        config.occlusionFilterModel = FacePassModel.initModel(context.getAssets(), context.getString(R.string.mcv_occlusion_B));
                        /* 送识别阈值参数 */
                        config.searchThreshold = 75f;
                        config.livenessThreshold = 88f; //单目推荐80
                        config.faceMinThreshold = 100;
                        config.poseThreshold = new FacePassPose(45f, 45f, 45);
                        config.blurThreshold = 0.8f;
                        config.lowBrightnessThreshold = 30f;
                        config.highBrightnessThreshold = 210f;
                        config.brightnessSTDThreshold = 80f;
                        config.rgbIrLivenessEnabled = true;
//                          config.LivenessEnabled = true;
                        config.rcAttributeEnabled = true;

                        /* 其他设置 */
                        config.maxFaceEnabled = false;
                        config.retryCount = 6;
                        config.fileRootPath = FILE_ROOT_PATH;
//                          getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

                        /* 创建SDK实例 */
                        mFacePassHandler = new FacePassHandler();
                        int ret = mFacePassHandler.initHandle(config);
                        if (ret != 0) {
                            Log.d(DEBUG_TAG, "Build FacePassHandler failed, error: " + ret);
                            return;
                        }
                        /*设置双目图片配准参数, 偏移量系数需由客户根据自己的设备测试得到.*/
                        mFacePassHandler.setIRConfig(1.0, 0.0, 1.0, 0.0, 0.3);

                        /* 入库阈值参数 */
                        FacePassConfig addFaceConfig = mFacePassHandler.getAddFaceConfig();
                        addFaceConfig.poseThreshold.pitch = 35f;
                        addFaceConfig.poseThreshold.roll = 35f;
                        addFaceConfig.poseThreshold.yaw = 35f;
                        addFaceConfig.blurThreshold = 0.7f;
                        addFaceConfig.lowBrightnessThreshold = 70f;
                        addFaceConfig.highBrightnessThreshold = 220f;
                        addFaceConfig.brightnessSTDThreshold = 60f;
                        addFaceConfig.faceMinThreshold = 40;
                        mFacePassHandler.setAddFaceConfig(addFaceConfig);
                        checkGroup(context);
                        Log.d(DEBUG_TAG, "Build FacePassHandler success.");
                        return;
                    } catch (FacePassException e) {
                        e.printStackTrace();
                        Log.d(DEBUG_TAG, "Build FacePassHandler failed.");
                    }
                }

//                }
            }
        }.start();
    }


    public void checkGroup(Context context) {
        if (mFacePassHandler == null) {
            return;
        }
        String[] localGroups = new String[0];
        try {
            localGroups = mFacePassHandler.getLocalGroups();
        } catch (FacePassException e) {
            e.printStackTrace();
        }
        isLocalGroupExist = false;
        if (localGroups == null || localGroups.length == 0) {

            try {
                boolean isSuccess = mFacePassHandler.createLocalGroup(group_name);
            } catch (FacePassException e) {
                e.printStackTrace();
            }
            return;
        }
        for (String group : localGroups) {
            if (group_name.equals(group)) {
                isLocalGroupExist = true;
                break;
            }
        }
        if (!isLocalGroupExist) {
            try {
                boolean isSuccess = mFacePassHandler.createLocalGroup(group_name);
            } catch (FacePassException e) {
                e.printStackTrace();
            }

        }
    }

}
