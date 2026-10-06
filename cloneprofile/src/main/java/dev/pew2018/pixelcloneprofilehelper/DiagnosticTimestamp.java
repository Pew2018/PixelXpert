package dev.pew2018.pixelcloneprofilehelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
final class DiagnosticTimestamp {
    private DiagnosticTimestamp(){}
    static String format(long millis){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'",Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }
}
