package mcv.testfacepass.bean;

import android.graphics.Bitmap;

import java.io.Serializable;

/***
 * db保存的feature信息
 */
public class FaceFeatureBean implements Serializable {
    public Bitmap bitmap;
    public String faceId;
    public String name;
}
