package dev.pew2018.pixelcloneprofilehelper;
import android.content.Context;
import android.content.SharedPreferences;
final class AppConfig {
    private static Context context;
    static void init(Context value) { context = value.getApplicationContext(); }
    static SharedPreferences prefs() {
        if (context == null) throw new IllegalStateException("not initialized");
        return context.getSharedPreferences("private_config", Context.MODE_PRIVATE);
    }
}
