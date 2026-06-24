package android.app.prediction;

import android.content.Context;

/** Stub di compilazione — vedi nota in AppTargetId.java. */
public final class AppPredictionContext {

    private final String uiSurface;
    private final int predictedTargetCount;
    private final String packageName;

    private AppPredictionContext(String uiSurface, int predictedTargetCount, String packageName) {
        this.uiSurface = uiSurface;
        this.predictedTargetCount = predictedTargetCount;
        this.packageName = packageName;
    }

    public String getUiSurface() {
        return uiSurface;
    }

    public int getPredictedTargetCount() {
        return predictedTargetCount;
    }

    public String getPackageName() {
        return packageName;
    }

    public static final class Builder {
        private final Context context;
        private String uiSurface = "";
        private int predictedTargetCount = 8;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setUiSurface(String uiSurface) {
            this.uiSurface = uiSurface;
            return this;
        }

        public Builder setPredictedTargetCount(int count) {
            this.predictedTargetCount = count;
            return this;
        }

        public AppPredictionContext build() {
            return new AppPredictionContext(uiSurface, predictedTargetCount,
                    context.getPackageName());
        }
    }
}
