package dev.pew2018.pixelcloneprofilehelper;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
public final class HelperApplication extends Application implements XposedServiceHelper.OnServiceListener {
    static final String PREFS="clone_profile_helper", ENTRY="open_entry", THIRD="allow_third_party";
    static volatile XposedService service;
    static volatile String framework="未知：等待 Vector/LSPosed 服务";
    static volatile boolean scoped;
    @Override public void onCreate(){super.onCreate(); XposedServiceHelper.registerListener(this);}
    @Override public void onServiceBind(XposedService value){
        service=value; scoped=value.getScope()!=null && value.getScope().contains("com.android.settings");
        framework="已连接："+value.getFrameworkName()+" "+value.getFrameworkVersion()+"（API "+value.getApiVersion()+"）";
        sync(); MainActivity.refreshVisible();
    }
    @Override public void onServiceDied(XposedService value){
        if(service==value)service=null; scoped=false; framework="未知：框架服务已断开"; MainActivity.refreshVisible();
    }
    static boolean sync(){
        XposedService current=service; if(current==null)return false;
        try {
            SharedPreferences remote=current.getRemotePreferences(PREFS), local=AppConfig.prefs();
            SharedPreferences.Editor e=remote.edit(); if(e==null)return false;
            e.putBoolean(ENTRY,local.getBoolean(ENTRY,true)).putBoolean(THIRD,local.getBoolean(THIRD,true))
              .putLong("config_updated_at",System.currentTimeMillis());
            return e.commit();
        } catch(RuntimeException ex){Log.w("PCPH","Remote config sync failed: "+ex.getClass().getSimpleName()); return false;}
    }
}
