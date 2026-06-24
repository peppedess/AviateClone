package android.app.prediction;

import android.os.Parcel;
import android.os.Parcelable;

/** Stub di compilazione — vedi nota in AppTargetId.java. */
public final class AppTargetEvent implements Parcelable {

    public static final int ACTION_LAUNCH = 1;
    public static final int ACTION_DISMISS = 2;
    public static final int ACTION_PIN = 3;
    public static final int ACTION_UNPIN = 4;

    private final AppTarget target;
    private final int action;
    private final String launchLocation;

    private AppTargetEvent(AppTarget target, int action, String launchLocation) {
        this.target = target;
        this.action = action;
        this.launchLocation = launchLocation;
    }

    public AppTarget getTarget() {
        return target;
    }

    public int getAction() {
        return action;
    }

    public String getLaunchLocation() {
        return launchLocation == null ? "" : launchLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(target, flags);
        dest.writeInt(action);
        dest.writeString(launchLocation);
    }

    public static final Creator<AppTargetEvent> CREATOR = new Creator<AppTargetEvent>() {
        @Override
        public AppTargetEvent createFromParcel(Parcel in) {
            AppTarget t = in.readParcelable(AppTarget.class.getClassLoader());
            int a = in.readInt();
            String loc = in.readString();
            return new AppTargetEvent(t, a, loc);
        }

        @Override
        public AppTargetEvent[] newArray(int size) {
            return new AppTargetEvent[size];
        }
    };

    public static final class Builder {
        private final AppTarget target;
        private final int action;
        private String launchLocation = "";

        public Builder(AppTarget target, int action) {
            this.target = target;
            this.action = action;
        }

        public Builder setLaunchLocation(String launchLocation) {
            this.launchLocation = launchLocation;
            return this;
        }

        public AppTargetEvent build() {
            return new AppTargetEvent(target, action, launchLocation);
        }
    }
}
