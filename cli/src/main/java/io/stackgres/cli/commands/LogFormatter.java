package io.stackgres.cli.commands;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class LogFormatter {

    private static final String RESET = "\033[0m";
    private static final String DIM = "\033[2m";
    private static final String BOLD = "\033[1m";
    private static final String CYAN = "\033[36m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED = "\033[31m";
    private static final String MAGENTA = "\033[35m";
    private static final String WHITE = "\033[37m";

    private static final Set<String> STANDARD_ETCD_KEYS = Set.of("logTime", "component", "level", "message", "processId");

    private LogFormatter() {
    }

    public static String formatLine(String line, String format) {
        if (line == null || line.isEmpty())
            return line;
        if ("json".equals(format))
            return line;

        Map<String, String> fields = parseJsonLine(line);
        if (fields == null)
            return line; // not JSON, return as-is

        String logTime = fields.getOrDefault("logTime", "");
        String level = fields.getOrDefault("level", "");
        String component = fields.getOrDefault("component", "");
        String message = fields.getOrDefault("message", "");
        String processId = fields.getOrDefault("processId", "");

        if ("plain".equals(format))
            return formatPlain(logTime, level, component, message, processId, fields);
        return formatColored(logTime, level, component, message, processId, fields);
    }

    public static String formatBlock(String block, String format) {
        if (block == null || block.isEmpty())
            return block;
        if ("json".equals(format))
            return block;

        StringBuilder sb = new StringBuilder();
        for (String line : block.split("\n")) {
            if (!sb.isEmpty())
                sb.append('\n');
            sb.append(formatLine(line, format));
        }
        return sb.toString();
    }

    private static String formatPlain(String logTime, String level, String component, String message, String processId, Map<String, String> fields) {
        return switch (component) {
            case "postgres" -> formatPlainPostgres(logTime, level, message, processId);
            case "patroni" -> formatPlainPatroni(logTime, level, message);
            case "slon" -> formatPlainSlon(logTime, level, message);
            case "etcd" -> formatPlainEtcd(logTime, level, message, fields);
            default -> formatPlainDefault(logTime, level, message);
        };
    }

    private static String formatPlainPostgres(String logTime, String level, String message, String processId) {
        // Original: 2026-04-09 16:25:04 UTC [41]: LOG:  message
        String ts = logTime.isEmpty() ? "" : toPostgresTimestamp(logTime) + " UTC ";
        String pid = processId.isEmpty() ? "" : "[" + processId + "]: ";
        return ts + pid + level + ":  " + message;
    }

    private static String formatPlainPatroni(String logTime, String level, String message) {
        // Original: 2026-04-09 13:37:53,113 DEBUG: message
        String ts = logTime.isEmpty() ? "" : toPatroniTimestamp(logTime) + " ";
        return ts + level + ": " + message;
    }

    private static String formatPlainSlon(String logTime, String level, String message) {
        // Original: [2026-04-09 13:37:53] INFO com.ongres...: message
        String ts = logTime.isEmpty() ? "" : "[" + toSlonTimestamp(logTime) + "] ";
        return ts + level + " " + message;
    }

    private static String formatPlainEtcd(String logTime, String level, String message, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        if (!logTime.isEmpty())
            sb.append(formatTimestamp(logTime)).append(' ');
        if (!level.isEmpty())
            sb.append(level).append(' ');
        String caller = fields.getOrDefault("caller", "");
        if (!caller.isEmpty())
            sb.append(caller).append(' ');
        sb.append(message);
        String extras = formatEtcdExtras(fields);
        if (!extras.isEmpty())
            sb.append(" | ").append(extras);
        return sb.toString();
    }

    private static String formatEtcdExtras(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (STANDARD_ETCD_KEYS.contains(entry.getKey()) || "caller".equals(entry.getKey()) || "logger".equals(entry.getKey()))
                continue;
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    private static String formatPlainDefault(String logTime, String level, String message) {
        StringBuilder sb = new StringBuilder();
        if (!logTime.isEmpty())
            sb.append(formatTimestamp(logTime)).append(' ');
        if (!level.isEmpty())
            sb.append(level).append(' ');
        sb.append(message);
        return sb.toString();
    }

    private static String toPostgresTimestamp(String isoTime) {
        // 2026-04-09T13:37:53.113Z -> 2026-04-09 13:37:53.113
        return formatTimestamp(isoTime);
    }

    private static String toPatroniTimestamp(String isoTime) {
        // 2026-04-09T13:37:53.113Z -> 2026-04-09 13:37:53,113
        return formatTimestamp(isoTime).replace('.', ',');
    }

    private static String toSlonTimestamp(String isoTime) {
        // 2026-04-09T13:37:53.113Z -> 2026-04-09 13:37:53
        String ts = formatTimestamp(isoTime);
        int dotIdx = ts.indexOf('.');
        return dotIdx > 0 ? ts.substring(0, dotIdx) : ts;
    }

    private static String formatTimestamp(String logTime) {
        String formatted = logTime.replace('T', ' ');
        if (formatted.endsWith("Z"))
            formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    private static String formatColored(String logTime, String level, String component, String message, String processId, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        if (!logTime.isEmpty()) {
            String ts = formatTimestamp(logTime);
            int spaceIdx = ts.indexOf(' ');
            if (spaceIdx > 0) {
                sb.append(DIM).append(ts, 0, spaceIdx).append(RESET)
                        .append(' ')
                        .append(WHITE).append(ts.substring(spaceIdx + 1)).append(RESET);
            } else {
                sb.append(DIM).append(ts).append(RESET);
            }
            sb.append(' ');
        }
        if (!processId.isEmpty())
            sb.append(MAGENTA).append('[').append(processId).append(']').append(RESET).append(' ');
        if (!level.isEmpty()) {
            String upper = level.toUpperCase();
            sb.append(levelColor(upper)).append(upper).append(RESET).append(' ');
        }
        if ("etcd".equals(component)) {
            String caller = fields.getOrDefault("caller", "");
            if (!caller.isEmpty())
                sb.append(DIM).append(caller).append(RESET).append(' ');
        }
        sb.append(message);
        if ("etcd".equals(component)) {
            String extras = formatEtcdExtras(fields);
            if (!extras.isEmpty())
                sb.append(' ').append(DIM).append(extras).append(RESET);
        }
        return sb.toString();
    }

    private static String levelColor(String level) {
        return switch (level) {
            case "DEBUG", "DETAIL" -> CYAN;
            case "INFO", "LOG", "NOTICE" -> GREEN;
            case "WARNING", "WARN" -> YELLOW;
            case "ERROR", "FATAL", "PANIC", "CRITICAL", "SEVERE" -> RED + BOLD;
            default -> "";
        };
    }

    static Map<String, String> parseJsonLine(String line) {
        if (line == null || line.isEmpty() || line.charAt(0) != '{')
            return null;
        try (JsonReader reader = Json.createReader(new StringReader(line))) {
            JsonObject obj = reader.readObject();
            Map<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                JsonValue value = entry.getValue();
                if (value.getValueType() == JsonValue.ValueType.STRING)
                    map.put(entry.getKey(), ((jakarta.json.JsonString) value).getString());
                else
                    map.put(entry.getKey(), value.toString());
            }
            return map;
        } catch (Exception e) {
            return null;
        }
    }

}