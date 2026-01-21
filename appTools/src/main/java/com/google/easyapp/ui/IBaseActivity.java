package com.google.easyapp.ui;

import android.content.Context;
import android.view.View;

public interface IBaseActivity {

    int getLayout();

    void initView(final View view);

    void doBusiness(Context mContext);

}
