package dev.pew2018.pixelcloneprofilehelper;
import static org.junit.Assert.assertEquals;
import android.content.pm.ApplicationInfo;
import org.junit.Test;
import java.util.*;
public class CandidatePolicyTest {
    private CandidatePolicy.Installed a(String n,int f){return new CandidatePolicy.Installed(n,f);}
    @Test public void retainsWhitelistAndAddsSortedThirdParty(){assertEquals(Arrays.asList("sys.white","user.white","user.a","user.z"),
        CandidatePolicy.merge(Arrays.asList("sys.white","user.white"),Arrays.asList(a("user.z",0),a("sys.white",ApplicationInfo.FLAG_SYSTEM),a("user.white",0),a("sys.hidden",ApplicationInfo.FLAG_SYSTEM),a("user.a",0)),Collections.emptySet(),true));}
    @Test public void dropsMissingAndDeduplicates(){assertEquals(Collections.singletonList("user.app"),CandidatePolicy.merge(Arrays.asList("user.app","user.app","gone.app"),Collections.singletonList(a("user.app",0)),Collections.emptySet(),true));}
    @Test public void excludesSystemAndUpdatedSystemOutsideWhitelist(){assertEquals(Collections.emptyList(),CandidatePolicy.merge(Collections.emptyList(),Arrays.asList(a("sys",ApplicationInfo.FLAG_SYSTEM),a("updated",ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)),Collections.emptySet(),true));}
    @Test public void hidesAlreadyClonedSystemButNotThirdParty(){assertEquals(Collections.singletonList("user.app"),CandidatePolicy.merge(Arrays.asList("sys","user.app"),Arrays.asList(a("sys",ApplicationInfo.FLAG_SYSTEM),a("user.app",0)),new HashSet<>(Arrays.asList("sys","user.app")),true));}
    @Test public void disablingExtensionRestoresWhitelist(){assertEquals(Collections.singletonList("sys"),CandidatePolicy.merge(Arrays.asList("sys","gone"),Arrays.asList(a("sys",ApplicationInfo.FLAG_SYSTEM),a("user",0)),Collections.emptySet(),false));}
}
