package dev.pew2018.pixelcloneprofilehelper;

import java.util.ArrayList;
import java.util.List;

/** Stable identity is the entire stored event, not a truncated/formatted timestamp or its hash. */
final class LogProjection {
    enum Kind { DATE, EVENT, EMPTY }
    static final class Item {
        final String key, text;
        final Kind kind;
        Item(String key, String text, Kind kind) { this.key = key; this.text = text; this.kind = kind; }
    }
    static List<Item> project(List<String> ascendingRows) {
        List<Item> result = new ArrayList<>();
        if (ascendingRows.isEmpty()) {
            result.add(new Item("empty", "", Kind.EMPTY)); return result;
        }
        String previousDate = null;
        for (int i = ascendingRows.size() - 1; i >= 0; i--) {
            String raw = ascendingRows.get(i);
            String timestamp = raw.split(" \\| ", 2)[0];
            String date = UiPresentation.logDate(timestamp);
            if (!date.equals(previousDate)) {
                result.add(new Item("date:" + date, date, Kind.DATE)); previousDate = date;
            }
            result.add(new Item("event:" + raw, raw, Kind.EVENT));
        }
        return result;
    }
}
