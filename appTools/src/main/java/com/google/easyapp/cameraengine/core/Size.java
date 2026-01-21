package com.google.easyapp.cameraengine.core;

import android.support.annotation.Nullable;

import java.io.Serializable;

public class Size implements Serializable {
    public int width;
    public int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size(Size other) {
        if(null != other) {
            this.width = other.width;
            this.height = other.height;
        }
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean isEquals = false;
        if(obj instanceof Size) {
            Size other = (Size) obj;
            isEquals = ((width == other.width) && (height == other.height));
        }
        return isEquals;
    }
}