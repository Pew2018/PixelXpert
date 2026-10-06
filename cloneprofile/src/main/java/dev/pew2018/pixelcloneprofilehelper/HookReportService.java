package dev.pew2018.pixelcloneprofilehelper;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import java.util.*;
import java.text.SimpleDateFormat;
public final class HookReportService extends Service {
    private Messenger messenger;
    @Override public void onCreate(){super.onCreate();messenger=new Messenger(new Handler(Looper.getMainLooper(),m->{
        if(!settingsUid(m.sendingUid)){Log.w("PCPH","Rejected diagnostic IPC");return true;}
        Bundle b=m.getData();String e=safe(b.getString("event"),80),d=safe(b.getString("detail"),180);
        long time=b.getLong("time",System.currentTimeMillis());SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS UTC",Locale.US);format.setTimeZone(TimeZone.getTimeZone("UTC"));
        append(format.format(new Date(time))+" | com.android.settings | "+e+(d.isEmpty()?"":" | "+d));MainActivity.refreshVisible();return true;
    }));}
    @Override public IBinder onBind(Intent i){return messenger.getBinder();}
    private boolean settingsUid(int uid){try{String[] p=getPackageManager().getPackagesForUid(uid);if(p!=null)for(String s:p)if("com.android.settings".equals(s))return true;}
        catch(RuntimeException e){Log.w("PCPH","Caller check failed");}return false;}
    private void append(String line){android.content.SharedPreferences p=getSharedPreferences("diagnostics",MODE_PRIVATE);
        ArrayList<String> rows=new ArrayList<>(p.getStringSet("logs",Collections.emptySet()));rows.add(line);
        if(rows.size()>100)rows=new ArrayList<>(rows.subList(rows.size()-100,rows.size()));
        p.edit().putStringSet("logs",new LinkedHashSet<>(rows)).apply();}
    private static String safe(String s,int n){if(s==null)return "";s=s.replaceAll("[\\r\\n\\t]"," ");return s.substring(0,Math.min(n,s.length()));}
}
