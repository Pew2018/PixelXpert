package dev.pew2018.pixelcloneprofilehelper;

import org.junit.Test;
import java.time.ZoneId;
import static org.junit.Assert.*;

public class UiPresentationTest {
    @Test public void utcReportCrossesLocalDateBoundary() {
        var time = UiPresentation.parseTime("2026-10-15 23:15:39.747Z", ZoneId.of("Asia/Taipei"));
        assertNotNull(time);
        assertEquals(16, time.getDayOfMonth());
        assertEquals(7, time.getHour());
        assertEquals(747000000, time.getNano());
    }
    @Test public void acceptsIsoOffsetsAndLegacyLocalRecords() {
        var zone = ZoneId.of("Asia/Taipei");
        assertEquals(14, UiPresentation.parseTime("2026-10-15T06:15:00Z", zone).getHour());
        assertEquals(14, UiPresentation.parseTime("2026-10-15T08:15:00+02:00", zone).getHour());
        assertEquals(6, UiPresentation.parseTime("2026-10-15 06:15:00.123", zone).getHour());
    }
    @Test public void missingAndMalformedTimesNeverBecomeNow() {
        assertNull(UiPresentation.parseTime(null, ZoneId.of("UTC")));
        assertNull(UiPresentation.parseTime("not a date", ZoneId.of("UTC")));
        assertNull(UiPresentation.parseTime("2026-02-30 06:15:00Z", ZoneId.of("UTC")));
    }
    @Test public void frameworkBuildStaysOutOfSummaryAndUnknownFormatsSurvive() {
        assertEquals("Vector 2.2 · API 102", UiPresentation.framework("已连接：Vector 2.2-abcdef（API 102）"));
        assertEquals("unknown custom framework", UiPresentation.framework("unknown custom framework"));
    }
}
