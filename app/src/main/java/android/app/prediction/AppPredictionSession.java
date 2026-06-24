package android.app.prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Stub di compilazione — vedi nota in AppTargetId.java.
 * Una sessione reale non produce mai aggiornamenti spontaneamente: in questo
 * stub i target restano vuoti, così il chiamante (AiPredictionManager) si
 * affida sempre al fallback ML locale quando l'API di sistema non è presente.
 */
public final class AppPredictionSession {

    private final AppPredictionContext context;
    private boolean destroyed = false;

    AppPredictionSession(AppPredictionContext context) {
        this.context = context;
    }

    public void registerPredictionUpdates(Executor executor, Consumer<List<AppTarget>> callback) {
        // Nessun aggiornamento reale: il sistema host non implementa l'API.
    }

    public void unregisterPredictionUpdates(Consumer<List<AppTarget>> callback) {
        // no-op
    }

    public void requestPredictionUpdate() {
        // no-op: nessuna sorgente di previsione reale disponibile
    }

    public void notifyAppTargetEvent(AppTargetEvent event) {
        // no-op
    }

    public List<AppTarget> getTargets() {
        return new ArrayList<>();
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
