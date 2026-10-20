package dev.pew2018.pixelcloneprofilehelper;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class HelperApplication extends Application implements XposedServiceHelper.OnServiceListener {
    static final String PREFS = "clone_profile_helper", ENTRY = "open_entry", THIRD = "allow_third_party";
    static volatile XposedService service;
    static volatile String framework = "未知：等待 Vector/LSPosed 服务";
    static volatile boolean scoped, metadataReady;
    enum SyncState { WAITING, SENDING, SENT, FAILED }
    static volatile SyncState syncState = SyncState.WAITING;
    // Application-owned queue survives Activity recreation and preserves remote write order.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> new Thread(r, "CloneHelper-state"));
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile long serviceEpoch, syncRevision;
    private static long metadataRevision;

    @Override public void onCreate() {
        super.onCreate();
        AppConfig.init(this);
        XposedServiceHelper.registerListener(this);
        requestSync(); // Process startup only, never on every Activity resume / log event.
    }
    private static void onMain(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) task.run(); else MAIN.post(task);
    }
    @Override public void onServiceBind(XposedService value) {
        onMain(() -> {
            service = value; serviceEpoch++; metadataReady = false; scoped = false;
            framework = "未知：正在读取框架服务";
            refreshState(null);
            requestSync();
            MainActivity.refreshVisible();
        });
    }
    @Override public void onServiceDied(XposedService value) {
        onMain(() -> {
            if (service != value) return;
            service = null; serviceEpoch++; syncRevision++; metadataRevision++;
            scoped = false; metadataReady = false;
            syncState = SyncState.WAITING; framework = "未知：框架服务已断开";
            MainActivity.refreshVisible();
        });
    }
    static void requestSync() { onMain(HelperApplication::enqueueSync); }
    private static void enqueueSync() {
        final XposedService current = service;
        final long epoch = serviceEpoch, revision = ++syncRevision;
        // Immutable snapshot of both local values; a queued old task cannot read a mixed pair.
        SharedPreferences local = AppConfig.prefs();
        final boolean entry = local.getBoolean(ENTRY, true), third = local.getBoolean(THIRD, true);
        syncState = current == null ? SyncState.WAITING : SyncState.SENDING;
        MainActivity.refreshVisible();
        if (current == null) return;
        IO.execute(() -> {
            if (epoch != serviceEpoch || revision != syncRevision) return; // Coalesce tasks not yet started.
            boolean delivered = false;
            try {
                SharedPreferences remote = current.getRemotePreferences(PREFS);
                SharedPreferences.Editor editor = remote.edit();
                if (editor != null) delivered = editor.putBoolean(ENTRY, entry).putBoolean(THIRD, third)
                        .putLong("config_updated_at", System.currentTimeMillis()).commit();
            } catch (RuntimeException ex) {
                Log.w("PCPH", "Remote config sync failed: " + ex.getClass().getSimpleName());
            }
            final boolean result = delivered;
            MAIN.post(() -> {
                if (epoch != serviceEpoch || revision != syncRevision || service != current) return;
                syncState = result ? SyncState.SENT : SyncState.FAILED;
                MainActivity.refreshVisible();
            });
        });
    }
    static final class RefreshTask {
        private volatile boolean cancelled;
        private volatile Future<?> future;
        void cancel() {
            cancelled = true;
            Future<?> work = future;
            if (work != null) work.cancel(false); // Do not interrupt a binder call or unrelated config writes.
        }
    }

    /** Read-only refresh. All binder access runs on the same serial worker, never on the UI thread. */
    static RefreshTask refreshState(Consumer<Boolean> complete) {
        RefreshTask task = new RefreshTask();
        onMain(() -> {
            if (task.cancelled) return;
            final XposedService current = service;
            final long epoch = serviceEpoch, revision = ++metadataRevision;
            task.future = IO.submit(() -> {
                if (task.cancelled) return;
                boolean success = true, scope = false;
                String description = framework;
                if (current != null && epoch == serviceEpoch) {
                    try {
                        Collection<String> targets = current.getScope();
                        scope = targets != null && targets.contains("com.android.settings");
                        description = "已连接：" + current.getFrameworkName() + " " + current.getFrameworkVersion()
                                + "（API " + current.getApiVersion() + "）";
                    } catch (RuntimeException ex) {
                        success = false; description = "未知：框架状态读取失败";
                        Log.w("PCPH", "Framework state read failed: " + ex.getClass().getSimpleName());
                    }
                }
                final boolean result = success, scopeResult = scope;
                final String frameworkResult = description;
                MAIN.post(() -> {
                    if (task.cancelled) return;
                    boolean valid = epoch == serviceEpoch && revision == metadataRevision && current == service;
                    if (valid && current != null) {
                        scoped = scopeResult; framework = frameworkResult; metadataReady = result;
                    }
                    MainActivity.refreshVisible();
                    if (complete != null) complete.accept(valid && result);
                });
            });
        });
        return task;
    }
}
