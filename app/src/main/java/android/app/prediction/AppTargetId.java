package android.app.prediction;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stub di compilazione per android.app.prediction.AppTargetId.
 * Le vere classi vivono nel framework di sistema Android (API 30+, SystemApi
 * parziale) e non sono incluse nell'android.jar pubblico usato in build.
 * Questo stub esiste solo per permettere la compilazione: a runtime, se il
 * sistema reale espone l'API, è il classloader di sistema a fornirla.
 */
public final class AppTargetId implements Parcelable {

    private final String id;

    public AppTargetId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
    }

    public static final Creator<AppTargetId> CREATOR = new Creator<AppTargetId>() {
        @Override
        public AppTargetId createFromParcel(Parcel in) {
            return new AppTargetId(in.readString());
        }

        @Override
        public AppTargetId[] newArray(int size) {
            return new AppTargetId[size];
        }
    };
}
