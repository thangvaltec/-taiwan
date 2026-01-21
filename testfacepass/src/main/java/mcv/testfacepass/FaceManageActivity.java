package mcv.testfacepass;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.easyapp.GoogleWidgetHelper;
import com.google.easyapp.ui.BaseActivity;
import com.google.easyapp.ui.camera.TakePicActivity;
import com.google.easyapp.utils.BitmapUtils;
import com.google.easyapp.utils.L;
import com.google.easyapp.utils.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.OnClick;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import mcv.facepass.FacePassException;
import mcv.facepass.types.FacePassAddFaceResult;
import mcv.testfacepass.adapter.FeatureItemAdapter;
import mcv.testfacepass.bean.FaceFeatureBean;
import mcv.testfacepass.data.DatabaseHelper;
import mcv.testfacepass.utils.FacePassManager;


public class FaceManageActivity extends BaseActivity {
    private static final int IMAGE_RESULT_CODE = 2;// 表示打开照相机
    private static final int REQUEST_CODE_ALBUM = 1001;//相册
    RecyclerView recyclerView;
    ImageView showImg;
    ImageView ivGoBack;
    EditText name_edt;
    RelativeLayout addFaceLayout;
    private FeatureItemAdapter adapter;

    private Bitmap currBitmap;
    private String currFilePath;

    private List<FaceFeatureBean> faces = new ArrayList<>();

    private DatabaseHelper dbHelper;

    private String faceToken = "";


    private final Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                adapter.notifyDataSetChanged();
            } else if (msg.what == 1) {
                L.d(TAG, "查询数据库完成");
                //用来从数据库查询完成
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void initView(View view) {

        recyclerView = (RecyclerView) view.findViewById(R.id.rcyFace);
        showImg = (ImageView) view.findViewById(R.id.image_add);
        name_edt = (EditText) view.findViewById(R.id.name_edt);
        addFaceLayout = (RelativeLayout) view.findViewById(R.id.add_face_layout);

        ivGoBack = (ImageView) view.findViewById(R.id.ivGoBack);
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
//        adapter = new FeatureItemAdapter(FaceManageActivity.this, FeatureDataSource.getInstance().getList());
        //获取列表
        adapter = new FeatureItemAdapter(FaceManageActivity.this, faces);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getBaseContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new FeatureItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, FaceFeatureBean item) {
                showDeleteDialog(position, item.faceId);
            }
        });

        dbHelper = new DatabaseHelper(this);
    }


    private void goTakePic() {
        new RxPermissions(this).request(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(@NonNull Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    GoogleWidgetHelper.startCameraActivity(FaceManageActivity.this, IMAGE_RESULT_CODE, 0, false);
                }
            }
        });
    }


    @OnClick({R.id.ok, R.id.iv_add, R.id.iv_add_image})
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.ok:
                final String name = name_edt.getText().toString().trim();
                if (!TextUtils.isEmpty(name) && currBitmap != null) {

                    featureAndSaveFacePhoto(currBitmap, currFilePath, name);

                }
                break;
            case R.id.iv_add:
                goTakePic();
                break;
            case R.id.iv_add_image:
                openAlbum();
                break;
        }
    }

    private void openAlbum() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.GET_CONTENT");
        intent.addCategory("android.intent.category.OPENABLE");
        startActivityForResult(intent, REQUEST_CODE_ALBUM);
    }

    /**
     * 保存裁剪之后的图片数据
     */
    private void showImage(Bitmap photo, String photoFilePath) {
        try {
            FacePassAddFaceResult result = FacePassManager.mFacePassHandler.addFace(photo);

            if (result != null) {
                if (result.result == 0) {
                    android.util.Log.d("addfaceDemo", "result:" + result.result
                            + ",bl:" + result.blur
                            + ",pp:" + result.pose.pitch
                            + ",pr:" + result.pose.roll
                            + ",py:" + result.pose.yaw);

                    addFaceLayout.setVisibility(View.VISIBLE);
                    currFilePath = photoFilePath;
                    currBitmap = photo;
                    showImg.setImageBitmap(photo);
                    faceToken = new String(result.faceToken);
                } else if (result.result == 1) {
                    ToastUtils.showShort("未找到人脸");
                } else {
                    android.util.Log.d("addfaceDemo", "result:" + result.result
                            + ",bl:" + result.facePassQualityCheck.isBlurPassed
                            + ",pp:" + result.facePassQualityCheck.isPitchPassed
                            + ",pr:" + result.facePassQualityCheck.isRollPassed
                            + ",py:" + result.facePassQualityCheck.isYawPassed
                            + ",edge:" + result.facePassQualityCheck.isEdgefacePassed
                            + ",lmkscore:" + result.landmarkscore
                            + ",brt:" + result.facePassQualityCheck.isBrightnessPassd
                            + ",occ_valid:" + result.lmkoccsta.valid);
                    ToastUtils.showShort("人脸校验失败，请重新选择图片");
                }
            }
        } catch (FacePassException e) {
            e.printStackTrace();
            ToastUtils.showShort(e.getMessage());
        }
    }


    /**
     * 添加人脸
     *
     * @param currBitmap
     * @param headPath
     * @param name
     * @return
     */
    private boolean featureAndSaveFacePhoto(final Bitmap currBitmap, String headPath, final String name) {

        if (FacePassManager.mFacePassHandler == null) {
            ToastUtils.showShort("FacePassHandle is null ! ");
        }

        final byte[] token = faceToken.getBytes();
        final String groupName = FacePassManager.group_name;
        if (token.length == 0 || TextUtils.isEmpty(groupName)) {
            ToastUtils.showShort("params error！");
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    final boolean b = FacePassManager.mFacePassHandler.bindGroup(groupName, token);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String result = b ? "成功" : "失败";
                            ToastUtils.showShort("录入" + result);

                            FaceFeatureBean faceBean = new FaceFeatureBean();
                            faceBean.name = name;
                            faceBean.faceId = faceToken;
                            faceBean.bitmap = currBitmap;
                            faces.add(faceBean);
                            adapter.notifyDataSetChanged();
                            addFaceLayout.setVisibility(View.GONE);
                            dbHelper.add(faceToken, name);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v(TAG, e.getMessage());
                }
            }
        }.start();

        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            // 表示 调用照相机拍照
            case IMAGE_RESULT_CODE:
                if (data != null) {
                    String selectedFilePath = data.getStringExtra(TakePicActivity.KEY_RESULT);
                    if (!TextUtils.isEmpty(selectedFilePath)) {
                        Bitmap selectedBitmap = BitmapUtils.getOriginBitmap(selectedFilePath);
                        if (selectedBitmap != null) {
                            showImage(selectedBitmap, selectedFilePath);
                        } else {
                            ToastUtils.showShort("图片错误，请重新拍照");
                        }
                    } else {
                        ToastUtils.showShort("照片走失啦！");
                    }
                }
                break;
            case REQUEST_CODE_ALBUM:
                if (data != null) {
                    Uri uri = data.getData();
                    String selectedFilePath = saveImageToCache(uri);

                    if (!TextUtils.isEmpty(selectedFilePath)) {
                        Bitmap selectedBitmap = BitmapUtils.getOriginBitmap(selectedFilePath);
                        if (selectedBitmap != null) {
                            showImage(selectedBitmap, selectedFilePath);
                        } else {
                            ToastUtils.showShort("图片错误，请重新拍照");
                        }
                    } else {
                        ToastUtils.showShort("照片走失啦！");
                    }
                }
                break;

        }
    }

    private String saveImageToCache(Uri imageUri) {
        String filePath = null; // 用于保存生成的文件路径

        try {
            // 获取输入流，通过 Uri 读取图片数据
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // 获取缓存目录，并创建保存图片的文件
            File cacheDir = getCacheDir(); // 获取缓存目录
            File imageFile = new File(cacheDir, "cached_image_" + System.currentTimeMillis() + ".jpg");

            // 将输入流写入缓存文件
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.close();
            inputStream.close();

            // 获取文件的绝对路径
            filePath = imageFile.getAbsolutePath();
//            Toast.makeText(this, "Image saved to cache: " + filePath, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
//            Toast.makeText(this, "Failed to save image to cache", Toast.LENGTH_LONG).show();
        }

        // 返回保存的文件路径
        return filePath;
    }


    public void showDeleteDialog(final int position, final String faceId) {
        Dialog dialog = new AlertDialog.Builder(this).setTitle("删除确认").setMessage("是否删除这张照片呢？").setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                FeatureDataSource.getInstance().deletePhotoBean(position);
                if (faceId == null) return;
                byte[] faceToken = faceId.getBytes();
                try {
                    boolean b = FacePassManager.mFacePassHandler.deleteFace(faceToken);
                    dbHelper.delete(faceId);
                    faces.remove(position);
                } catch (FacePassException e) {
                    e.printStackTrace();
                }

                if (adapter != null) adapter.notifyDataSetChanged();
                Toast.makeText(FaceManageActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create();

        dialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int getLayout() {
        return R.layout.activity_facemanage;
    }


    @Override
    public void doBusiness(Context mContext) {
        getFaces();
    }

    private void getFaces() {
        if (FacePassManager.mFacePassHandler == null) {
            ToastUtils.showShort("FacePassHandle is null ! ");
            return;
        }
        String groupName = FacePassManager.group_name;
        if (TextUtils.isEmpty(groupName)) {
            ToastUtils.showShort("group name  is null ！");
            return;
        }
        try {
            byte[][] faceTokens = FacePassManager.mFacePassHandler.getLocalGroupInfo(groupName);
            if (faceTokens != null && faceTokens.length > 0) {
                for (byte[] faceToken : faceTokens) {
                    if (faceToken.length > 0) {
                        FaceFeatureBean faceBean = new FaceFeatureBean();
                        faceBean.name = dbHelper.findName(new String(faceToken));
                        faceBean.bitmap = FacePassManager.mFacePassHandler.getFaceImage(faceToken);
                        faceBean.faceId= new String(faceToken);
                        faces.add(faceBean);
                    }
                }
                mainHandler.sendEmptyMessage(1);

            }

        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShort("get local group info error!");
        }

    }

}
