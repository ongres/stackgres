package io.stackgres.cli;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * The one place the CLI formats timestamps, so every command renders them identically. Absolute times
 * are UTC ({@code yyyy-MM-dd HH:mm:ss UTC}); freshness is expressed relatively ({@code 5m ago}); the two
 * combine for "last seen" style fields. Replaces the half-dozen ad-hoc {@code DateTimeFormatter}s that
 * had drifted apart across the commands.
 */
public final class Times {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private Times() {
    }

    /** Absolute UTC instant, e.g. {@code 2026-09-02 12:32:05 UTC}. Empty string for {@code null}. */
    public static String stamp(Instant t) {
        return t == null ? "" : STAMP.format(t) + " UTC";
    }

    /** Relative age, e.g. {@code Just now} / {@code 5m ago} / {@code 3d ago}. Empty for {@code null}. */
    public static String ago(Instant t) {
        return ago(t, 5);
    }

    /** As {@link #ago(Instant)} but treats anything under {@code nowThresholdSeconds} as "Just now". */
    public static String ago(Instant t, int nowThresholdSeconds) {
        if (t == null) {
            return "";
        }
        long seconds = Math.max(0, Duration.between(t, Instant.now()).getSeconds());
        if (seconds < nowThresholdSeconds) {
            return "Just now";
        }
        if (seconds < 60) {
            return seconds + "s ago";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m ago";
        }
        if (seconds < 86400) {
            return (seconds / 3600) + "h ago";
        }
        return (seconds / 86400) + "d ago";
    }

    /** Absolute + relative, e.g. {@code 2026-09-02 12:32:05 UTC (5m ago)}. Empty for {@code null}. */
    public static String stampAndAgo(Instant t) {
        return t == null ? "" : stamp(t) + " (" + ago(t) + ")";
    }
}
