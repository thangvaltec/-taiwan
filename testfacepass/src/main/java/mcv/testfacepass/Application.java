package mcv.testfacepass;

import android.support.multidex.MultiDexApplication;

import com.google.easyapp.APPUtils;

public class Application extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        APPUtils.init(this);
    }
}
