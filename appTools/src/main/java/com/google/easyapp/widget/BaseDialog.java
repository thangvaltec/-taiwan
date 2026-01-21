package com.google.easyapp.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.easyapp.R;

public abstract class BaseDialog {
    protected Context context;
    private Dialog mDialog;

    public BaseDialog(Context context) {
        this.context = context;
        this.builder();
    }

    public BaseDialog builder() {
        View view = LayoutInflater.from(this.context).inflate(getLayout(), null);
        bindView(view);
        this.mDialog = new Dialog(this.context, R.style.AlertDialogStyle);
        this.mDialog.setContentView(view);
        if (isFullScreen()) {
            Window window = mDialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                // 隐藏顶部的状态栏
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } else {
            view.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }
        this.mDialog.setCancelable(false);
        this.mDialog.setCanceledOnTouchOutside(false);
        return this;
    }

    public boolean isFullScreen() {
        return false;
    }

    protected abstract void bindView(View view);

    protected abstract int getLayout();

    public void show() {
        this.mDialog.show();
    }


    public boolean isShow() {
        return mDialog.isShowing();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
