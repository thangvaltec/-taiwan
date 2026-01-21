package com.google.easyapp;

import android.os.Environment;

import java.io.File;


public class Constants {

    public static String getRootDir() {
        String dir = null;
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();  //获取根目录
        }
        if (sdDir != null) {
            dir = makeDir(sdDir.toString() + File.separator + "Google" + File.separator);
        }
        return dir;
    }

    public static String getFilePath() {
        String dir = getRootDir();
        if (dir != null) {
            return dir + File.separator + File.separator;
        } else {
            String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            absolutePath += File.separator + "Google";
            return absolutePath + File.separator + File.separator;
        }
    }



    public static String getBitmapPath() {
        return makeDir(getFilePath() + "bitmap" + File.separator);
    }

    public static String makeDir(String dir) {
        File file = new File(dir);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return dir;
    }
}
