package mcv.testfacepass;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import mcv.testfacepass.utils.FacePassManager;

public class MainActivity extends BaseActivity{



    /* 程序所需权限 ：相机 文件存储 网络访问 */
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    private String[] Permission = new String[]{PERMISSION_CAMERA, PERMISSION_WRITE_STORAGE, PERMISSION_READ_STORAGE, PERMISSION_INTERNET, PERMISSION_ACCESS_NETWORK_STATE};

    @Override
    public int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void initView(View view) {
        view.findViewById(R.id.btAddFace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,FaceManageActivity.class));
            }
        });

        view.findViewById(R.id.btFaceDetect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,FacePassActivity.class));
            }
        });

    }



    @Override
    public void doBusiness(Context mContext) {
        /* 申请程序所需权限 */
        if (!hasPermission()) {
            requestPermission();
        } else {
            FacePassManager.getInstance().init(this);
        }
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

    /* 请求程序所需权限 */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(Permission, PERMISSIONS_REQUEST);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED)
                    granted = false;
            }
            if (!granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    if (!shouldShowRequestPermissionRationale(PERMISSION_CAMERA)
                            || !shouldShowRequestPermissionRationale(PERMISSION_READ_STORAGE)
                            || !shouldShowRequestPermissionRationale(PERMISSION_WRITE_STORAGE)
                            || !shouldShowRequestPermissionRationale(PERMISSION_INTERNET)
                            || !shouldShowRequestPermissionRationale(PERMISSION_ACCESS_NETWORK_STATE)) {
                        Toast.makeText(getApplicationContext(), "需要开启摄像头网络文件存储权限", Toast.LENGTH_SHORT).show();
                    }
            } else {
                FacePassManager.getInstance().init(this);

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (FacePassManager.mFacePassHandler != null) {
            FacePassManager.mFacePassHandler.release();
        }
    }
}
