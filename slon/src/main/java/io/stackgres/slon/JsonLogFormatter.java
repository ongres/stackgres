package io.stackgres.slon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class JsonLogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendField(sb, "ts", Instant.ofEpochMilli(record.getMillis()).toString());
        sb.append(',');
        appendField(sb, "level", record.getLevel().getName());
        sb.append(',');
        appendField(sb, "logger", String.valueOf(record.getLoggerName()));
        sb.append(',');
        appendField(sb, "thread", Thread.currentThread().getName());
        sb.append(',');
        appendField(sb, "msg", formatMessage(record));
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            sb.append(",\"error\":{");
            appendField(sb, "class", thrown.getClass().getName());
            if (thrown.getMessage() != null) {
                sb.append(',');
                appendField(sb, "message", thrown.getMessage());
            }
            sb.append(',');
            appendField(sb, "stack", stackTraceOf(thrown));
            sb.append('}');
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":\"");
        escape(sb, value);
        sb.append('"');
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static void escape(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }

}