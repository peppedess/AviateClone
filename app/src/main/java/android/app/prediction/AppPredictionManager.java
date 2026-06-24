package android.app.prediction;

/** Stub di compilazione — vedi nota in AppTargetId.java. */
public final class AppPredictionManager {

    /**
     * Equivalente di PackageManager.FEATURE_APP_PREDICTION_API, che è
     * @SystemApi e quindi non risolvibile nell'android.jar pubblico.
     * Nessun device "normale" la dichiara comunque (hasSystemFeature
     * ritorna sempre false), quindi il fallback al ML locale resta garantito.
     */
    public static final String FEATURE_APP_PREDICTION_API = "android.software.app_prediction";

    public AppPredictionSession createAppPredictionSession(AppPredictionContext context) {
        return new AppPredictionSession(context);
    }
}
