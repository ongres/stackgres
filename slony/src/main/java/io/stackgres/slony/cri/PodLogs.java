package io.stackgres.slony.cri;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PodLogs {

    private static final Pattern CRI_LOG_FORMAT = Pattern.compile("^(?<time>[^ ]+) (?<out>stdout|stderr) (?<tag>[^ ]*) (?<message>.*)$");

    public static String parseLogLine(String line) {
        Matcher matcher = CRI_LOG_FORMAT.matcher(line);
        if (!matcher.matches())
            return "";
        return matcher.group("message");
    }

    public static String parseLogFile(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(PodLogs::parseLogLine)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read container logs (at " + path + ")", e);
        }
    }

}