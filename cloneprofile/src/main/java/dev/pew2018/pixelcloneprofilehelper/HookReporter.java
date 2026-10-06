package dev.pew2018.pixelcloneprofilehelper;
import android.content.*;
import android.os.*;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
final class HookReporter {
    private static volatile Messenger remote; private static volatile boolean connecting;
    private static volatile Context context; private static volatile XposedModule module;
    HookReporter(XposedModule m){module=m;try{
        Class<?> c=Class.forName("android.app.ActivityThread");context=(Context)c.getDeclaredMethod("currentApplication").invoke(null);connect();
    }catch(ReflectiveOperationException|RuntimeException e){m.log(Log.WARN,"PCPH","Settings context unavailable: "+e.getClass().getSimpleName());}}
    private static synchronized void connect(){if(connecting||remote!=null||context==null)return;connecting=true;
        try{Intent i=new Intent().setComponent(new ComponentName("dev.pew2018.pixelcloneprofilehelper","dev.pew2018.pixelcloneprofilehelper.HookReportService"));
            context.bindService(i,new ServiceConnection(){
                public void onServiceConnected(ComponentName n,IBinder b){remote=new Messenger(b);connecting=false;report("REPORT_CHANNEL_CONNECTED","ready");}
                public void onServiceDisconnected(ComponentName n){remote=null;connecting=false;}
                public void onBindingDied(ComponentName n){remote=null;connecting=false;}
                public void onNullBinding(ComponentName n){remote=null;connecting=false;report("REPORT_CHANNEL_REJECTED","caller not accepted");}
            },Context.BIND_AUTO_CREATE);
        }catch(RuntimeException e){connecting=false;module.log(Log.WARN,"PCPH","IPC bind failed: "+e.getClass().getSimpleName());}}
    static void report(String event,String detail){try{Messenger m=remote;if(m==null){connect();return;}
        Message msg=Message.obtain();Bundle b=new Bundle();b.putString("event",event);b.putString("detail",detail);b.putLong("time",System.currentTimeMillis());msg.setData(b);m.send(msg);
    }catch(Exception e){remote=null;XposedModule m=module;if(m!=null)m.log(Log.WARN,"PCPH","IPC failed: "+e.getClass().getSimpleName());}}
}
