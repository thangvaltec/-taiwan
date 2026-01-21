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

package com.google.easyapp.cameraengine.core.gpuimage.util;

import com.google.easyapp.cameraengine.core.gpuimage.Rotation;

import java.util.Arrays;


public class TextureRotationUtil {

    public static final float TEXTURE_NO_ROTATION[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };
    public static final float TEXTURE_ROTATION_90[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    public static final float TEXTURE_ROTATION_180[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATION_270[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };

    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private TextureRotationUtil() {
    }

    public static float[] getRotation(final int rotate, final boolean flipHorizontal, final boolean flipVertical) {
        Rotation rotation = Rotation.NORMAL;
        switch (rotate) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        return getRotation(rotation, flipHorizontal, flipVertical);
    }

    public static float[] getRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case ROTATION_90:
                rotatedTex = TEXTURE_ROTATION_90;
                break;
            case ROTATION_180:
                rotatedTex = TEXTURE_ROTATION_180;
                break;
            case ROTATION_270:
                rotatedTex = TEXTURE_ROTATION_270;
                break;
            case NORMAL:
            default:
                rotatedTex = TEXTURE_NO_ROTATION;
                break;
        }
        rotatedTex = Arrays.copyOf(rotatedTex, rotatedTex.length);

        if (flipHorizontal) {
            float tmpX, tmpY;
            tmpX = rotatedTex[0];
            tmpY = rotatedTex[1];
            rotatedTex[0] = rotatedTex[2];
            rotatedTex[1] = rotatedTex[3];
            rotatedTex[2] = tmpX;
            rotatedTex[3] = tmpY;

            tmpX = rotatedTex[4];
            tmpY = rotatedTex[5];
            rotatedTex[4] = rotatedTex[6];
            rotatedTex[5] = rotatedTex[7];
            rotatedTex[6] = tmpX;
            rotatedTex[7] = tmpY;
        }

        if (flipVertical) {
            float tmpX, tmpY;
            tmpX = rotatedTex[0];
            tmpY = rotatedTex[1];
            rotatedTex[0] = rotatedTex[4];
            rotatedTex[1] = rotatedTex[5];
            rotatedTex[4] = tmpX;
            rotatedTex[5] = tmpY;

            tmpX = rotatedTex[2];
            tmpY = rotatedTex[3];
            rotatedTex[2] = rotatedTex[6];
            rotatedTex[3] = rotatedTex[7];
            rotatedTex[6] = tmpX;
            rotatedTex[7] = tmpY;
        }
        return rotatedTex;
    }


    public static float flip(final float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }
}
