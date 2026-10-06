package dev.pew2018.pixelcloneprofilehelper;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
public class DiagnosticTimestampTest {
    @Test public void formatsUtcMarkerAsLiteralAndDoesNotThrow(){
        assertEquals("1970-01-01 00:00:00.000Z",DiagnosticTimestamp.format(0L));
    }
}
