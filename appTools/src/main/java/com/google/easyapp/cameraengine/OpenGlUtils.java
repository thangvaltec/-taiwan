package com.google.easyapp.cameraengine;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import com.google.easyapp.APPUtils;
import com.google.easyapp.utils.L;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_TEXTURE_2D;

public class OpenGlUtils {
    public static final int NO_TEXTURE = -1;
    public static final int NOT_INIT = -1;
    public static final int ON_DRAWN = 1;
    public static final int BYTES_PER_FLOAT = 4;
    public static final short BYTES_PER_INT = 4;
    public static final short BYTES_PER_SHORT = 2;
    private static final String TAG = "OpenGlUtils";

    public static int loadTexture(final Bitmap img, final int usedTexId) {
        return loadTexture(img, usedTexId, false);
    }

    public static int loadTexture(final Bitmap img, final int usedTexId, boolean recyled) {
        if (img == null)
            return NO_TEXTURE;
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recyled)
            img.recycle();
        return textures[0];
    }
    public static void checkGLError(String msg) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String str = msg + ": glError 0x" + Integer.toHexString(error);
            L.e(TAG, str);
            int values[] = new int[2];
            GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, values, 0);
            GLES20.glGetIntegerv(GLES20.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, values, 1);
            L.e(TAG, "Current bound array buffer: " + values[0]);
            L.e(TAG, "Current bound vertex attrib: "+ values[1]);
            throw new RuntimeException(msg);
        }
    }
    public static int loadTexture(final Buffer data, final int width, final int height, final int usedTexId) {
        if (data == null)
            return NO_TEXTURE;
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int loadTexture(final Buffer data, final int width, final int height, final int usedTexId, final int type) {
        if (data == null)
            return NO_TEXTURE;
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, type, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_RGBA, type, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int loadTexture(final Context context, final String name) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {

            // Read in the resource
            final Bitmap bitmap = getImageFromAssetsFile(context, name);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int loadLumianceTexture(final Buffer data, int width, int height) {
        data.position(0);
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, data);
        return textures[0];
    }

    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static int loadProgram(final String strVSource, final String strFSource) {
        int iVShader;
        int iFShader;
        int iProgId;
        int[] link = new int[1];
        iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if (iVShader == 0) {
            L.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
        if (iFShader == 0) {
            L.d("Load Program", "Fragment Shader Failed");
            return 0;
        }

        iProgId = GLES20.glCreateProgram();
        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);
        GLES20.glLinkProgram(iProgId);
        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            L.d("Load Program", "Linking Failed");
            return 0;
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        return iProgId;
    }

    private static int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            L.e("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }
//
//    public static Bitmap drawToBitmapByFilter(Bitmap bitmap, GPUImageFilter filter,
//                                              int displayWidth, int displayHeight, boolean rotate){
//        if(filter == null)
//            return null;
//        int level = bitmap.getWidth();
//        int height = bitmap.getHeight();
//        int[] mFrameBuffers = new int[1];
//        int[] mFrameBufferTextures = new int[1];
//        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
//        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, level, height, 0,
//                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
//                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
//        GLES20.glViewport(0, 0, level, height);
//        filter.onInputSizeChanged(level, height);
//        filter.onDisplaySizeChanged(displayWidth, displayHeight);
//        int textureId = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, true);
//        if(rotate){
//            FloatBuffer gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);
//
//            FloatBuffer gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.ROTATION_90, true, false)).position(0);
//            filter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
//        }else {
//            filter.onDrawFrame(textureId);
//        }
//        IntBuffer ib = IntBuffer.allocate(level * height);
//        GLES20.glReadPixels(0, 0, level, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
//        Bitmap result = Bitmap.createBitmap(level, height, Bitmap.Config.ARGB_8888);
//        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
//        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
//        GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
//        GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
//        filter.onInputSizeChanged(displayWidth, displayHeight);
//        return result;
//    }

    public static int getExternalOESTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public static String readShaderFromRawResource(final int resourceId) {
        final InputStream inputStream = APPUtils.getApp().getResources().openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String nextLine;
            final StringBuilder body = new StringBuilder();

            try {
                while ((nextLine = bufferedReader.readLine()) != null) {
                    body.append(nextLine);
                    body.append('\n');
                }
            } catch (IOException e) {
                return null;
            }
            String decrypt = body.toString();
            return decrypt;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";

    }

    public static FloatBuffer createFloatBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asFloatBuffer();
    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        FloatBuffer fb = createFloatBuffer(coords.length);
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static ShortBuffer createShortBuffer(short[] data) {
        ShortBuffer sb = ByteBuffer.allocateDirect(data.length * BYTES_PER_SHORT)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        sb.put(data).position(0);
        return sb;
    }

    public static IntBuffer createIntBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * BYTES_PER_INT);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }

    /**
     * 创建Texture
     * @param bytes
     * @param width
     * @param height
     * @return
     */
    public static int createTexture(byte[] bytes, int width, int height) {
        if(bytes.length != width * height * 4) {
            throw new RuntimeException("Illegal byte array");
        }
        return createTexture(ByteBuffer.wrap(bytes), width, height);
    }

    /**
     * 创建Texture
     * @param byteBuffer
     * @param width
     * @param height
     * @return
     */
    public static int createTexture(ByteBuffer byteBuffer, int width, int height) {
        if (byteBuffer.array().length != width * height * 4) {
            throw new RuntimeException("Illegal byte array");
        }
        final int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        if (texture[0] == 0) {
            L.d("createTexture","Failed at glGenTextures");
            return 0;
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);

        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width,height, 0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                byteBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        return texture[0];
    }
}
