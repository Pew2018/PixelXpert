package dev.pew2018.pixelcloneprofilehelper;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class LogProjectionTest {
    private static List<String> keys(List<LogProjection.Item> items) {
        List<String> result = new ArrayList<>();
        for (LogProjection.Item item : items) result.add(item.key);
        return result;
    }
    @Test public void simultaneousEventsHaveDifferentStableIdentitiesAndKeepRawPrecision() {
        String first = "2026-10-15 06:15:39.747Z | com.android.settings | CONFIG_READ | entry=on";
        String second = "2026-10-15 06:15:39.747Z | com.android.settings | HOOK_MATCHED | method";
        List<String> rows = new ArrayList<>(Arrays.asList(first, second));
        List<LogProjection.Item> projected = LogProjection.project(rows);
        assertEquals(3, projected.size());
        assertEquals(LogProjection.Kind.DATE, projected.get(0).kind);
        assertNotEquals(projected.get(1).key, projected.get(2).key);
        assertEquals(second, projected.get(1).text);
        assertEquals(first, projected.get(2).text);
        assertEquals(Arrays.asList(first, second), rows); // Does not reorder the stored population.
    }
    @Test public void prependAndRetentionRemovalKeepUnchangedEventAndDateKeys() {
        String first = "2026-10-15 06:10:00.001Z | settings | CONFIG_READ";
        String retained = "2026-10-15 06:11:00.002Z | settings | CONFIG_READ";
        String added = "2026-10-15 06:12:00.003Z | settings | HOOK_MATCHED";
        List<String> before = keys(LogProjection.project(Arrays.asList(first, retained)));
        List<String> after = keys(LogProjection.project(Arrays.asList(retained, added)));
        assertEquals(before.get(0), after.get(0)); // Same date header, no remove/recreate.
        assertTrue(after.contains("event:" + retained));
        assertFalse(after.contains("event:" + first));
        assertEquals("event:" + added, after.get(1));
    }
    @Test public void clearingAndMalformedRecordsRemainRepresentable() {
        List<LogProjection.Item> empty = LogProjection.project(new ArrayList<>());
        assertEquals(1, empty.size());
        assertEquals(LogProjection.Kind.EMPTY, empty.get(0).kind);
        String malformed = "unparseable raw record";
        List<LogProjection.Item> projected = LogProjection.project(Arrays.asList(malformed));
        assertEquals(malformed, projected.get(1).text);
        assertEquals("event:" + malformed, projected.get(1).key);
    }
}
