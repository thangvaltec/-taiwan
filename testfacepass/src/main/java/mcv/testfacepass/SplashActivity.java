package mcv.testfacepass;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

public class SplashActivity extends Activity {

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        int mCurrentOrientation = getResources().getConfiguration().orientation;

        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_splash);
        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_splash);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, FacePassActivity.class);
                startActivity(intent);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
