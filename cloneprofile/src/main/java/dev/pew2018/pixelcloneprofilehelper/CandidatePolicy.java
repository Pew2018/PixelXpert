package dev.pew2018.pixelcloneprofilehelper;
import android.content.pm.ApplicationInfo;
import java.util.*;
final class CandidatePolicy {
    static final int SYSTEM_FLAGS=ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
    static final class Installed { final String name; final int flags; Installed(String n,int f){name=n;flags=f;} }
    static List<String> merge(List<String> whitelist, Collection<Installed> apps, Set<String> cloned, boolean extend){
        Map<String,Installed> installed=new HashMap<>(); for(Installed a:apps)if(valid(a.name))installed.put(a.name,a);
        LinkedHashSet<String> out=new LinkedHashSet<>();
        if(whitelist!=null) for(String n:whitelist)if(valid(n)&&installed.containsKey(n))out.add(n);
        if(extend){TreeSet<String> third=new TreeSet<>(); for(Installed a:apps)
            if(valid(a.name)&&(a.flags&SYSTEM_FLAGS)==0&&installed.containsKey(a.name))third.add(a.name);
            out.addAll(third);}
        if(cloned!=null)out.removeIf(n->{Installed a=installed.get(n);return a!=null&&(a.flags&SYSTEM_FLAGS)!=0&&cloned.contains(n);});
        return new ArrayList<>(out);
    }
    private static boolean valid(String n){return n!=null&&!n.trim().isEmpty();}
}
