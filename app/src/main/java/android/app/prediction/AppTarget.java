package android.app.prediction;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

/** Stub di compilazione — vedi nota in AppTargetId.java. */
public final class AppTarget implements Parcelable {

    private final AppTargetId id;
    private final String packageName;
    private final String className;
    private final UserHandle user;

    private AppTarget(AppTargetId id, String packageName, String className, UserHandle user) {
        this.id = id;
        this.packageName = packageName;
        this.className = className;
        this.user = user;
    }

    public AppTargetId getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className == null ? "" : className;
    }

    public UserHandle getUser() {
        return user;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(id, flags);
        dest.writeString(packageName);
        dest.writeString(className);
    }

    public static final Creator<AppTarget> CREATOR = new Creator<AppTarget>() {
        @Override
        public AppTarget createFromParcel(Parcel in) {
            AppTargetId id = in.readParcelable(AppTargetId.class.getClassLoader());
            String pkg = in.readString();
            String cls = in.readString();
            return new AppTarget(id, pkg, cls, null);
        }

        @Override
        public AppTarget[] newArray(int size) {
            return new AppTarget[size];
        }
    };

    public static final class Builder {
        private final AppTargetId id;
        private final String packageName;
        private final UserHandle user;
        private String className = "";

        public Builder(AppTargetId id, String packageName, UserHandle user) {
            this.id = id;
            this.packageName = packageName;
            this.user = user;
        }

        public Builder setClassName(String className) {
            this.className = className;
            return this;
        }

        public AppTarget build() {
            return new AppTarget(id, packageName, className, user);
        }
    }
}
