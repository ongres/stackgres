package io.stackgres.cli;

import java.time.Duration;
import java.time.Instant;

import static picocli.CommandLine.Help.Ansi.AUTO;

public final class Strings {

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static String errorAnsi(String string) {
        return AUTO.string("@|red,bold " + string + "|@");
    }

    public static String warnAnsi(String string) {
        return AUTO.string("@|yellow " + string + "|@");
    }

    public static String commentAnsi(String string) {
        return AUTO.string("\033[38:5:246m" + string + "\033[39m");
    }

    public static String formatTimeAgo(Instant instant, int nowThresholdSeconds) {
        if (instant == null) return "Never";
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < nowThresholdSeconds) return "Just now";
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }

    public static String formatMemory(long memory) {
        if (memory <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(memory) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format("%.1f %s", memory / Math.pow(1024, digitGroups), units[digitGroups]);
    }

}