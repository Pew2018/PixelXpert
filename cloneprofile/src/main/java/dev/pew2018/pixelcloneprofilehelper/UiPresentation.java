package dev.pew2018.pixelcloneprofilehelper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Display-only conversions. Never changes stored timestamps or framework identifiers. */
final class UiPresentation {
    private UiPresentation() {}

    static ZonedDateTime parseTime(String raw, ZoneId zone) {
        if (raw == null) return null;
        try {
            // DiagnosticTimestamp uses a space separator and a literal UTC suffix.
            return Instant.parse(raw.replace(' ', 'T')).atZone(zone);
        } catch (DateTimeParseException ignored) {
            try {
                // Legacy records without an offset were written as local wall time.
                return LocalDateTime.parse(raw.replace(' ', 'T')).atZone(zone);
            } catch (DateTimeParseException invalid) {
                return null;
            }
        }
    }

    static String reportTime(String raw) {
        ZonedDateTime time = parseTime(raw, ZoneId.systemDefault());
        return time == null ? raw : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault()).format(time);
    }

    static String logDate(String raw) {
        ZonedDateTime time = parseTime(raw, ZoneId.systemDefault());
        return time == null ? raw : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault()).format(time);
    }

    static String logTime(String raw) {
        ZonedDateTime time = parseTime(raw, ZoneId.systemDefault());
        // Unknown timestamps remain available in details without filling the time column.
        return time == null ? "—" : DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(time);
    }

    static String framework(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        // Only shorten a recognized, connected service description; keep unknown formats intact.
        Matcher match = Pattern.compile("^已连接：(.*?)\\s+[vV]?(\\d+(?:\\.\\d+)+).*?（API (\\d+)）$").matcher(raw);
        return match.matches() ? match.group(1) + " " + match.group(2) + " · API " + match.group(3) : raw;
    }
}
