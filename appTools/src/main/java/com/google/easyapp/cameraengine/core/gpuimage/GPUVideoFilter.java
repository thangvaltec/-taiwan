/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.easyapp.cameraengine.core.gpuimage;

import android.opengl.GLES20;

import com.google.easyapp.cameraengine.core.gpuimage.util.YuvMode;
import com.google.easyapp.cameraengine.core.gpuimage.util.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GPUVideoFilter extends GPUImageFilter {
    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec2 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER_NV21 = "" +
            "precision mediump float;  \n" +
            "uniform sampler2D inputImageTexture;  \n" +
            "uniform sampler2D inputImageTextureAlpha;  \n" +
            "varying highp vec2 textureCoordinate;  \n" +
            "  \n" +
            "const mat3 yuv2rgb = mat3(  \n" +
            "    1, 0, 1.2802,  \n" +
            "    1, -0.214821, -0.380589,  \n" +
            "    1, 2.127982, 0  \n" +
            ");  \n" +
            "  \n" +
            "void main() {  \n" +
            "    vec3 yuv = vec3(  \n" +
            "        1.1643 * (texture2D(inputImageTexture, textureCoordinate).r - 0.0625),  \n" +
            "        texture2D(inputImageTextureAlpha, textureCoordinate).a - 0.5,  \n" +
            "        texture2D(inputImageTextureAlpha, textureCoordinate).r - 0.5  \n" +
            "    ); \n" +
            "    vec3 rgb = yuv * yuv2rgb;  \n" +
            "    gl_FragColor = vec4(rgb, 1);  \n" +
            //"    gl_FragColor = vec4(1, 0, 0, 1);  \n" +
            "} ";
    public static final String NO_FILTER_FRAGMENT_SHADER_NV12 = "" +
            "precision mediump float;  \n" +
            "uniform sampler2D inputImageTexture;  \n" +
            "uniform sampler2D inputImageTextureAlpha;  \n" +
            "varying highp vec2 textureCoordinate;  \n" +
            "  \n" +
            "const mat3 yuv2bgr = mat3(  \n" +
            "    1, 0, 1.2802,  \n" +
            "    1, -0.214821, -0.380589,  \n" +
            "    1, 2.127982, 0  \n" +
            ");  \n" +
            "  \n" +
            "void main() {  \n" +
            "    vec3 yuv = vec3(  \n" +
            "        1.1643 * (texture2D(inputImageTexture, textureCoordinate).r - 0.0625),  \n" +
            "        texture2D(inputImageTextureAlpha, textureCoordinate).a - 0.5,  \n" +
            "        texture2D(inputImageTextureAlpha, textureCoordinate).r - 0.5  \n" +
            "    ); \n" +
            "    vec3 bgr = yuv * yuv2bgr;  \n" +
            "    gl_FragColor = vec4(bgr.z, bgr.y, bgr.x, 1);  \n" +
            //"    gl_FragColor = vec4(1, 0, 0, 1);  \n" +
            "} ";
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    private int[] mFrameBuffers = null;
    private int[] mFrameBufferTextures = null;
    private int mFrameWidth;
    private int mFrameHeight;

    protected int mGLUniformTextureAlpha;

    public GPUVideoFilter() {
        super(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER_NV21);
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);
    }

    public int onDrawToTexture(int textureId, int textureIdAlpha) {
        if (mFrameBuffers == null)
            return OpenGlUtils.NO_TEXTURE;
        runPendingOnDrawTasks();
        GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glUseProgram(mGLProgId);
        if (!isInitialized()) {
            return -1;
        }
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);


        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        if (textureIdAlpha != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdAlpha);
            GLES20.glUniform1i(mGLUniformTextureAlpha, 1);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
        return mFrameBufferTextures[0];
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        initFrameBuffer(width, height);
    }

    private void initFrameBuffer(int width, int height) {
        if (mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            destroyFramebuffers();
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];

            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            OpenGlUtils.deleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

    @Override
    public void onInit() {
        super.onInit();
        mGLUniformTextureAlpha = GLES20.glGetUniformLocation(mGLProgId, "inputImageTextureAlpha");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyFramebuffers();
    }

    public void setYuvMode(YuvMode yuvMode) {
        setFragmentShader(YuvMode.YUV_NV12 == yuvMode ? NO_FILTER_FRAGMENT_SHADER_NV12 : NO_FILTER_FRAGMENT_SHADER_NV21);
    }
}
