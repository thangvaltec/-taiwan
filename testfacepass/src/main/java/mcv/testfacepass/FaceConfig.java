package mcv.testfacepass;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * @author thomas
 * @date 2019/4/1
 * @describe TODO
 */
public class FaceConfig implements Parcelable {

    public static final int MODE_ONLINE = 1;
    public static final int MODE_OFFLINE = 2;

    private String GroupName;
    private String FileRootPath;
    private int Mode;
    private boolean Debug;
    private float SearchThreshold;
    private float LivenessThreshold;
    private boolean LivenessEnable;
    private float Roll, Pitch, Yaw;
    private int FaceMinThreshold;
    private float BlurThreshold;
    private float LowBrightnessThreshold;
    private float HighBrightnessThreshold;
    private float BrightnessSTDThreshold;
    private int RetryCount;
    private int Rotation;

    private FaceConfig(String groupName, String fileRootPath, int mode, boolean debug,
                       float searchThreshold, float livenessThreshold, boolean livenessEnable,
                       float roll, float pitch, float yaw, int faceMinThreshold, float blurThreshold,
                       float lowBrightnessThreshold, float highBrightnessThreshold,
                       float brightnessSTDThreshold, int retryCount, int rotation) {
        GroupName = groupName;
        FileRootPath = fileRootPath;
        Mode = mode;
        Debug = debug;
        SearchThreshold = searchThreshold;
        LivenessThreshold = livenessThreshold;
        LivenessEnable = livenessEnable;
        Roll = roll;
        Pitch = pitch;
        Yaw = yaw;
        FaceMinThreshold = faceMinThreshold;
        BlurThreshold = blurThreshold;
        LowBrightnessThreshold = lowBrightnessThreshold;
        HighBrightnessThreshold = highBrightnessThreshold;
        BrightnessSTDThreshold = brightnessSTDThreshold;
        RetryCount = retryCount;
        Rotation = rotation;
    }

    private FaceConfig(Parcel in) {
        GroupName = in.readString();
        FileRootPath = in.readString();
        Mode = in.readInt();
        Debug = in.readByte() != 0;
        SearchThreshold = in.readFloat();
        LivenessThreshold = in.readFloat();
        LivenessEnable = in.readByte() != 0;
        Roll = in.readFloat();
        Pitch = in.readFloat();
        Yaw = in.readFloat();
        FaceMinThreshold = in.readInt();
        BlurThreshold = in.readFloat();
        LowBrightnessThreshold = in.readFloat();
        HighBrightnessThreshold = in.readFloat();
        BrightnessSTDThreshold = in.readFloat();
        RetryCount = in.readInt();
        Rotation = in.readInt();
    }

    public String getGroupName() {
        return GroupName;
    }

    public String getFileRootPath() {
        return FileRootPath;
    }

    public int getMode() {
        return Mode;
    }

    public boolean isDebug() {
        return Debug;
    }

    public float getSearchThreshold() {
        return SearchThreshold;
    }

    public float getLivenessThreshold() {
        return LivenessThreshold;
    }

    public boolean isLivenessEnable() {
        return LivenessEnable;
    }

    public float getRoll() {
        return Roll;
    }

    public float getPitch() {
        return Pitch;
    }

    public float getYaw() {
        return Yaw;
    }

    public int getFaceMinThreshold() {
        return FaceMinThreshold;
    }

    public float getBlurThreshold() {
        return BlurThreshold;
    }

    public float getLowBrightnessThreshold() {
        return LowBrightnessThreshold;
    }

    public float getHighBrightnessThreshold() {
        return HighBrightnessThreshold;
    }

    public float getBrightnessSTDThreshold() {
        return BrightnessSTDThreshold;
    }

    public int getRetryCount() {
        return RetryCount;
    }

    public int getRotation() {
        return Rotation;
    }

    public static class Builder {
        private String GroupName;
        private String FileRootPath;
        private int Type;
        private boolean Debug;
        private float SearchThreshold;
        private float LivenessThreshold;
        private boolean LivenessEnable;
        private float Roll, Pitch, Yaw;
        private int FaceMinThreshold;
        private float BlurThreshold;
        private float LowBrightnessThreshold;
        private float HighBrightnessThreshold;
        private float BrightnessSTDThreshold;
        private int RetryCount;
        private int Rotation;

        public Builder setGroupName(String groupName) {
            GroupName = groupName;
            return this;
        }

        public Builder setFileRootPath(String fileRootPath) {
            FileRootPath = fileRootPath;
            return this;
        }

        public Builder setType(int type) {
            Type = type;
            return this;
        }

        public Builder setDebug(boolean debug) {
            Debug = debug;
            return this;
        }

        public Builder setSearchThreshold(float searchThreshold) {
            SearchThreshold = searchThreshold;
            return this;
        }

        public Builder setLivenessThreshold(float livenessThreshold) {
            LivenessThreshold = livenessThreshold;
            return this;
        }

        public Builder setLivenessEnable(boolean livenessEnable) {
            LivenessEnable = livenessEnable;
            return this;
        }

        public Builder setRoll(float roll) {
            Roll = roll;
            return this;
        }

        public Builder setPitch(float pitch) {
            Pitch = pitch;
            return this;
        }

        public Builder setYaw(float yaw) {
            Yaw = yaw;
            return this;
        }

        public Builder setFaceMinThreshold(int faceMinThreshold) {
            FaceMinThreshold = faceMinThreshold;
            return this;
        }

        public Builder setBlurThreshold(float blurThreshold) {
            BlurThreshold = blurThreshold;
            return this;
        }

        public Builder setLowBrightnessThreshold(float lowBrightnessThreshold) {
            LowBrightnessThreshold = lowBrightnessThreshold;
            return this;
        }

        public Builder setHighBrightnessThreshold(float highBrightnessThreshold) {
            HighBrightnessThreshold = highBrightnessThreshold;
            return this;
        }

        public Builder setBrightnessSTDThreshold(float brightnessSTDThreshold) {
            BrightnessSTDThreshold = brightnessSTDThreshold;
            return this;
        }

        public Builder setRetryCount(int retryCount) {
            RetryCount = retryCount;
            return this;
        }

        public Builder setRotation(int rotation) {
            return this;
        }

        public FaceConfig Build() {
            return new FaceConfig(GroupName, FileRootPath, Type, Debug, SearchThreshold,
                    LivenessThreshold, LivenessEnable, Roll, Pitch, Yaw, FaceMinThreshold,
                    BlurThreshold, LowBrightnessThreshold, HighBrightnessThreshold,
                    BrightnessSTDThreshold, RetryCount, Rotation);
        }
    }

    @NonNull
    @Override
    public String toString() {
        String log = "GroupName:\"" + getGroupName() + "\" | " +
                "FileRootPath:\"" + getFileRootPath() + "\" | " +
                "Mode:\"" + getMode() + "\" | " +
                "SearchThreshold:\"" + getSearchThreshold() + "\" | " +
                "LivenessThreshold:\"" + getLivenessThreshold() + "\" | " +
                "LivenessEnable:\"" + isLivenessEnable() + "\" | " +
                "Roll:\"" + getRoll() + "\" | " +
                "Pitch:\"" + getPitch() + "\" | " +
                "Yaw:\"" + getYaw() + "\" | " +
                "FaceMinThreshold:\"" + getFaceMinThreshold() + "\" | " +
                "BlurThreshold:\"" + getBlurThreshold() + "\" | " +
                "LowBrightnessThreshold:\"" + getLowBrightnessThreshold() + "\" | " +
                "HighBrightnessThreshold:\"" + getHighBrightnessThreshold() + "\" | " +
                "BrightnessSTDThreshold:\"" + getBrightnessSTDThreshold() + "\" | " +
                "RetryCount:\"" + getRetryCount() + "\" | " +
                "Rotation:\"" + getRotation() + "\"";
        return log;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(GroupName);
        dest.writeString(FileRootPath);
        dest.writeInt(Mode);
        dest.writeByte((byte) (Debug ? 1 : 0));
        dest.writeFloat(SearchThreshold);
        dest.writeFloat(LivenessThreshold);
        dest.writeByte((byte) (LivenessEnable ? 1 : 0));
        dest.writeFloat(Roll);
        dest.writeFloat(Pitch);
        dest.writeFloat(Yaw);
        dest.writeInt(FaceMinThreshold);
        dest.writeFloat(BlurThreshold);
        dest.writeFloat(LowBrightnessThreshold);
        dest.writeFloat(HighBrightnessThreshold);
        dest.writeFloat(BrightnessSTDThreshold);
        dest.writeInt(RetryCount);
        dest.writeInt(Rotation);
    }

    public static final Creator<FaceConfig> CREATOR = new Creator<FaceConfig>() {
        @Override
        public FaceConfig createFromParcel(Parcel source) {
            return new FaceConfig(source);
        }

        @Override
        public FaceConfig[] newArray(int size) {
            return new FaceConfig[size];
        }
    };
}
