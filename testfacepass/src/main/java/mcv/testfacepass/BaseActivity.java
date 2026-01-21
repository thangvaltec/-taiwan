package mcv.testfacepass;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.easyapp.ui.IBaseActivity;
import com.google.easyapp.utils.L;

import butterknife.ButterKnife;

public abstract class BaseActivity extends AppCompatActivity implements IBaseActivity {
    protected String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG = getClass().getSimpleName();
        L.d(TAG, "onCreate " + this);
        super.onCreate(savedInstanceState);

        View contextView = LayoutInflater.from(this).inflate(getLayout(), null);
        Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.setFlags(flag, flag);

        setContentView(contextView);
        initView(contextView);
        Context mContext = this.getBaseContext();
        doBusiness(mContext);
    }

    @Override
    protected void onDestroy() {
        L.d(TAG, "onDestroy " + this);
        System.gc();
        super.onDestroy();
    }

}
