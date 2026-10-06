package dev.pew2018.pixelcloneprofilehelper;
import android.app.Application;
import android.content.Context;
import android.content.pm.*;
import android.util.Log;
import java.lang.reflect.*;
import java.util.*;
import io.github.libxposed.api.XposedModule;
final class HookInstaller {
    private static final String C="com.android.settings.applications.ClonedAppsPreferenceController";
    private static final String B="com.android.settings.applications.AppStateClonedAppsBridge";
    static void install(CloneProfileHelperModule m,ClassLoader loader,HookReporter r){
        int matched=0;
        try{
            Class<?> c=Class.forName(C,false,loader);Method target=null;
            for(Method x:c.getDeclaredMethods())if(x.getName().equals("getAvailabilityStatus")&&x.getParameterCount()==0&&x.getReturnType()==int.class){
                if(target!=null)throw new NoSuchMethodException("ambiguous target");target=x;}
            if(target==null)throw new NoSuchMethodException(C+"#getAvailabilityStatus()");
            target.setAccessible(true);
            m.hook(target).intercept(chain->{boolean[] cfg=CloneProfileHelperModule.config(m);
                if(cfg[0]){HookReporter.report("CONFIG_READ","entry=on");return 0;}
                HookReporter.report("CONFIG_READ","entry=off");return chain.proceed();});
            r.report("HOOK_MATCHED","ClonedAppsPreferenceController.getAvailabilityStatus()");matched++;
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){r.report("HOOK_FAILED","entry target: "+summary(e));m.log(Log.WARN,"PCPH","Entry hook skipped: "+e.getClass().getSimpleName());}
        try{
            Class<?> c=Class.forName(B,false,loader);Field f=c.getDeclaredField("mAllowedApps");f.setAccessible(true);
            Constructor<?>[] ctors=c.getDeclaredConstructors();if(ctors.length==0)throw new NoSuchMethodException("constructors missing");
            for(Constructor<?> ctor:ctors){ctor.setAccessible(true);m.hook(ctor).intercept(chain->{Object result=chain.proceed();
                boolean[] cfg=CloneProfileHelperModule.config(m);Object instance=chain.getThisObject();
                if(instance!=null){try{Object raw=f.get(instance);if(!(raw instanceof List<?>)){r.report("HOOK_PARTIAL","mAllowedApps type mismatch");return result;}
                    List<String> white=new ArrayList<>();for(Object x:(List<?>)raw)if(x instanceof String)white.add((String)x);
                    Application app=currentApp();if(app==null)throw new IllegalStateException("Settings Application unavailable");
                    List<CandidatePolicy.Installed> all=new ArrayList<>();
                    for(PackageInfo p:app.getPackageManager().getInstalledPackages(0))if(p.packageName!=null&&p.applicationInfo!=null)
                        all.add(new CandidatePolicy.Installed(p.packageName,p.applicationInfo.flags));
                    Set<String> cloned=clonePackages(loader,app);
                    f.set(instance,CandidatePolicy.merge(white,all,cloned,cfg[1]));
                    HookReporter.report("CANDIDATE_LIST_UPDATED","system whitelist retained; third-party candidates merged");
                }catch(ReflectiveOperationException|RuntimeException e){r.report("HOOK_PARTIAL","candidate list skipped: "+e.getClass().getSimpleName());m.log(Log.WARN,"PCPH","Candidate hook skipped: "+e.getClass().getSimpleName());}}
                return result;});}
            r.report("HOOK_MATCHED","AppStateClonedAppsBridge constructors + mAllowedApps");matched++;
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){r.report("HOOK_FAILED","candidate target: "+e.getClass().getSimpleName());m.log(Log.WARN,"PCPH","Candidate hook skipped: "+e.getClass().getSimpleName());}
        r.report("HOOK_INSTALL_RESULT","matched targets="+matched+"/2");
    }
    private static String summary(Throwable e){StackTraceElement[] s=e.getStackTrace();return e.getClass().getSimpleName()+(s.length==0?"":" @ "+s[0].getClassName()+"."+s[0].getMethodName()+":"+s[0].getLineNumber());}
    private static Application currentApp(){try{Class<?> c=Class.forName("android.app.ActivityThread");return (Application)c.getDeclaredMethod("currentApplication").invoke(null);}
        catch(ReflectiveOperationException|RuntimeException e){return null;}}
    private static Set<String> clonePackages(ClassLoader loader,Application app){
        Set<String> out=new HashSet<>();try{Class<?> u=Class.forName("com.android.settings.Utils",false,loader);
            Method id=u.getDeclaredMethod("getCloneUserId",Context.class);id.setAccessible(true);int user=((Number)id.invoke(null,app)).intValue();if(user<=0)return out;
            Method q=PackageManager.class.getDeclaredMethod("getInstalledPackagesAsUser",int.class,int.class);q.setAccessible(true);
            @SuppressWarnings("unchecked") List<PackageInfo> ps=(List<PackageInfo>)q.invoke(app.getPackageManager(),0,user);
            if(ps!=null)for(PackageInfo p:ps)if(p.packageName!=null)out.add(p.packageName);return out;
        }catch(ReflectiveOperationException|RuntimeException e){HookReporter.report("CLONE_PROFILE_QUERY_FAILED",e.getClass().getSimpleName());throw new IllegalStateException("clone profile query failed",e);}
    }
}
