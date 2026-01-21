package com.google.easyapp.utils;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    public static Bitmap getOriginBitmap(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if(bitmap != null) {
            int bitmapDegree = BitmapUtils.getBitmapDegree(imagePath);
            bitmap = BitmapUtils.rotateBitmap(bitmap, bitmapDegree, true);
        }
        return bitmap;
    }

    public static Bitmap createBitmap(int w, int h) {
        return Bitmap.createBitmap(w, h,
                Bitmap.Config.ARGB_8888);
    }

    public static Bitmap createBitmapByMatrix(Bitmap source,
                                              Matrix m, boolean filter, boolean isRecycle) {
        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), m, filter);
        // createBitmapByMatrix 有可能会直接返回source出来，所以此处需要判断result和source是否相同
        if (isRecycle && (result != source)) {
            recycleBitmap(source);
        }
        return result;
    }


    public static String saveBitmap(Context context, Bitmap b, String name, Bitmap.CompressFormat format) {
        File file = new File(name);
        File photoFile = savePhotoToSDCard(b, file.getParent(), file.getName(), format);
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getParent(), file.getName(), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(photoFile);
        intent.setData(uri);
        context.sendBroadcast(intent);
        assert photoFile != null;
        return photoFile.getAbsolutePath();
    }
    /**
     * Save image to the SD card
     *
     * @param photoBitmap 相片文件本身
     * @param photoName   储存的相片名字
     * @param path        储存的相片路径
     */
    public static File savePhotoToSDCard(Bitmap photoBitmap, String path, String photoName) {
        return savePhotoToSDCard(photoBitmap, path, photoName, Bitmap.CompressFormat.JPEG);
    }

    public static File savePhotoToSDCard(Bitmap photoBitmap, String path, String photoName, Bitmap.CompressFormat format) {
        return savePhotoToSDCard(photoBitmap, path, photoName, format, 100);
    }

    public static File savePhotoToSDCard(Bitmap photoBitmap, String path, String photoName, Bitmap.CompressFormat format, int quality) {
        if (checkSDCardAvailable()) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File photoFile = new File(path, photoName);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(photoFile);
                if (photoBitmap.compress(format, quality, fileOutputStream)) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                return photoFile;
            } catch (FileNotFoundException e) {
                photoFile.delete();
                e.printStackTrace();
            } catch (IOException e) {
                photoFile.delete();
                e.printStackTrace();
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return null;
    }

    /**
     * Check the SD card
     *
     * @return 是否获能获取到SD卡
     */
    public static boolean checkSDCardAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }




    public static Bitmap getScaleBitmap(Bitmap bitmap, float rate, boolean isRecycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(rate, rate);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }



    public static Bitmap getFilpBitmap(Bitmap bitmap, boolean isRecycle) {
        Matrix matrix = new Matrix();
        // sw sh的绝对值为绽放宽高的比例，sw为负数表示X方向翻转，sh为负数表示Y方向翻转
        matrix.postScale(-1, 1);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }


    public static Bitmap getFilpBitmap(Bitmap bitmap, float angle, boolean isRecycle) {
        Matrix matrix = new Matrix();
        // sw sh的绝对值为绽放宽高的比例，sw为负数表示X方向翻转，sh为负数表示Y方向翻转
        matrix.postScale(-1, 1);
        matrix.postRotate(angle);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }

    public static Bitmap getTBFilpBitmap(Bitmap bitmap, float mAngle, boolean isRecycle) {
        Matrix matrix = new Matrix();
        // sw sh的绝对值为绽放宽高的比例，sw为负数表示X方向翻转，sh为负数表示Y方向翻转
        matrix.postScale(1, -1);
        matrix.postRotate(mAngle);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }

    public static Bitmap rotateBitmap(Bitmap currentSelBitmap, float angle, boolean isRecycle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return createBitmapByMatrix(currentSelBitmap, matrix, false, isRecycle);
    }

    public static Bitmap getSmallBitmapByX(Bitmap bitmap, int mWidth, boolean isRecycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale2 = (float) mWidth / w;
        matrix.postScale(scale2, scale2);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }


    /**
     * 根据高度压缩
     *
     * @param bitmap
     * @param mHight
     * @param isRecycle
     * @return
     */
    public static Bitmap getSmallBitmapByY(Bitmap bitmap, int mHight, boolean isRecycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale2 = (float) mHight / h;
        matrix.postScale(scale2, scale2);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }

    public static Bitmap getSmallBitmapByXY(Bitmap bitmap, int mWidth, int mHight, boolean isRecycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scaleH = (float) mHight / h;
        float scaleW = (float) mWidth / w;
        Matrix matrix = new Matrix();
        float min = Math.min(scaleH, scaleW);
        matrix.postScale(min, min);
        return createBitmapByMatrix(bitmap, matrix, true, isRecycle);
    }

    public static Bitmap adjustViewAndBitmap(Bitmap bmp, View multipleLayersView, boolean isRecycle) {
        Bitmap result;
        ViewGroup.LayoutParams layoutParams = multipleLayersView.getLayoutParams();
        int width = multipleLayersView.getMeasuredWidth();
        int height = multipleLayersView.getMeasuredHeight();

        int bitmapW = bmp.getWidth();
        int bitmapH = bmp.getHeight();
        float f1 = (float) width / bitmapW;
        float f2 = (float) height / bitmapH;

        if (f2 > f1) {
            result = getSmallBitmapByX(bmp, width, isRecycle);
            layoutParams.height = result.getHeight();
            layoutParams.width = result.getWidth();

        } else {
            result = getSmallBitmapByY(bmp, height, isRecycle);
            layoutParams.height = result.getHeight();
            layoutParams.width = result.getWidth();
        }
        multipleLayersView.setLayoutParams(layoutParams);
        return result;
    }

    public static Bitmap createTxtBitmap(String str, Typeface font, int color) {
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(color);
        textPaint.setTypeface(font);
        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.setTextSize(300);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();

        String[] split = str.split("\n");

        int maxIndex = 0;
        for (int i = 0; i < split.length; ++i) {
            if (split[maxIndex].length() < split[i].length()) {
                maxIndex = i;
            }
        }
        //measureText = bound.right - bound.left + 字符两边的留白宽度
//        宽度获取方法：Paint.measureText(text)
//        高度获取方法：descent+Math.abs(ascent)
        int imageWidth = (int) textPaint.measureText(split[maxIndex]);
        int imageHeight = fontMetrics.descent + Math.abs(fontMetrics.ascent);
        int baseLine = 0 - fontMetrics.ascent;


        Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight * split.length, Bitmap.Config.ARGB_8888); //图象大小要根据文字大小算下,以和文本长度对应
        Canvas canvasTemp = new Canvas(bmp);
        canvasTemp.drawColor(Color.TRANSPARENT);
        if (str.equals("")){
            return bmp;
        }
        for (int i=0;i<split.length;++i){

            canvasTemp.drawText(split[i], 0, imageHeight * i + baseLine, textPaint);
        }
        return bmp;
    }

    public static Bitmap getWaterMask(Bitmap bitmap,Bitmap maskBitmap,boolean recycle) {
        Bitmap bmp = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888); //图象大小要根据文字大小算下,以和文本长度对应
        Canvas canvasTemp = new Canvas(bmp);
        canvasTemp.drawColor(Color.TRANSPARENT);
        int pading = (int) (bitmap.getWidth() * 0.025);

        int newWidth = bitmap.getWidth() / 8;
        float rate = newWidth/(float)maskBitmap.getWidth();
        int left = bitmap.getWidth() -newWidth- pading;
        int right = bitmap.getWidth() - pading;
        int top = bitmap.getHeight() -(int)(maskBitmap.getHeight()*rate)-pading;
        int bottom = bitmap.getHeight() -pading;

        canvasTemp.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        canvasTemp.drawBitmap(bitmap,0,0,null);
        canvasTemp.drawBitmap(maskBitmap,new Rect(0,0,maskBitmap.getWidth(),maskBitmap.getHeight()),new Rect(left,top,right,bottom),null);
        if (recycle){
            recycleBitmap(bitmap);
        }
        return bmp;
    }

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * 读取图片的旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);//NORMAL 为0，即不旋转
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90://右旋90度
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


}