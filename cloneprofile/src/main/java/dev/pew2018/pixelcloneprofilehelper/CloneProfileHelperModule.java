package dev.pew2018.pixelcloneprofilehelper;
import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
public final class CloneProfileHelperModule extends XposedModule {
    public CloneProfileHelperModule(){super();}
    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam p){log(Log.INFO,"PCPH","Loaded in "+p.getProcessName());}
    @Override public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam p){
        if(!"com.android.settings".equals(p.getPackageName()))return;
        HookReporter reporter=new HookReporter(this); String fp=android.os.Build.FINGERPRINT; if(fp.length()>48)fp=fp.substring(0,48); reporter.report("SETTINGS_PROCESS_LOADED","Android "+android.os.Build.VERSION.RELEASE+"; SDK="+android.os.Build.VERSION.SDK_INT+"; device="+android.os.Build.MODEL+"; fingerprint="+fp);
        HookInstaller.install(this,p.getDefaultClassLoader(),reporter);
    }
    static boolean[] config(CloneProfileHelperModule m){
        try{SharedPreferences p=m.getRemotePreferences(HelperApplication.PREFS);
            return new boolean[]{p.getBoolean(HelperApplication.ENTRY,true),p.getBoolean(HelperApplication.THIRD,true)};
        }catch(RuntimeException e){m.log(Log.WARN,"PCPH","Config unavailable: "+e.getClass().getSimpleName());return new boolean[]{false,false};}
    }
}
