package io.stackgres.matriarch.quarkus.grpc;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The "light parse" of the local log-tail path: re-shape a raw component log line (as tailed by the
 * slon) into the canonical JSON record the CLI's {@code LogFormatter} renders —
 * {@code {logTime, level, component, message, processId, ...}}. Postgres arrives as csvlog; slon,
 * patroni and etcd arrive as their own JSON. Normalizing here (matriarch-side, where a JSON parser is
 * available) lets the CLI colour/format every component exactly as it does for the cloud path, with no
 * client change. A line that can't be parsed is passed through verbatim (the CLI prints non-JSON lines
 * as-is), which also covers multi-line csvlog record continuations.
 */
final class ComponentLogNormalizer {

    private ComponentLogNormalizer() {
    }

    static String normalize(String component, String raw) {
        try {
            return switch (component) {
                case "postgres" -> postgres(raw);
                case "patroni" -> remap(raw, "patroni", "asctime", "levelname", "message");
                case "slon" -> remap(raw, "slon", "ts", "level", "msg");
                case "etcd" -> etcd(raw);
                default -> raw;
            };
        } catch (RuntimeException e) {
            return raw;
        }
    }

    // Postgres csvlog columns: log_time(0), process_id(3), error_severity(11), message(13).
    private static String postgres(String raw) {
        List<String> f = splitCsv(raw);
        if (f.size() < 14) {
            return raw;
        }
        return Json.createObjectBuilder()
                .add("logTime", dateTime(f.get(0)))
                .add("level", f.get(11))
                .add("component", "postgres")
                .add("message", f.get(13))
                .add("processId", f.get(3))
                .build().toString();
    }

    // slon / patroni JSON: {tsKey, levelKey, msgKey, ...} -> canonical.
    private static String remap(String raw, String component, String tsKey, String levelKey, String msgKey) {
        JsonObject obj = parse(raw);
        if (obj == null) {
            return raw;
        }
        return Json.createObjectBuilder()
                .add("logTime", str(obj, tsKey))
                .add("level", str(obj, levelKey))
                .add("component", component)
                .add("message", str(obj, msgKey))
                .build().toString();
    }

    // etcd (zap JSON): map ts->logTime, msg->message; keep level/caller and any extra fields for the extras line.
    private static String etcd(String raw) {
        JsonObject obj = parse(raw);
        if (obj == null) {
            return raw;
        }
        JsonObjectBuilder out = Json.createObjectBuilder()
                .add("logTime", str(obj, "ts"))
                .add("level", str(obj, "level"))
                .add("component", "etcd")
                .add("message", str(obj, "msg"));
        for (var entry : obj.entrySet()) {
            String key = entry.getKey();
            if (key.equals("ts") || key.equals("level") || key.equals("msg")) {
                continue;   // already mapped above
            }
            out.add(key, asString(entry.getValue()));
        }
        return out.build().toString();
    }

    private static JsonObject parse(String raw) {
        String trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
            return null;
        }
        try (JsonReader reader = Json.createReader(new StringReader(trimmed))) {
            return reader.readObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonValue v = obj.get(key);
        return v == null ? "" : asString(v);
    }

    private static String asString(JsonValue v) {
        return v.getValueType() == JsonValue.ValueType.STRING ? ((JsonString) v).getString() : v.toString();
    }

    // csvlog log_time "2026-08-24 05:07:35.298 UTC" -> "2026-08-24 05:07:35.298" (drop the zone token).
    private static String dateTime(String logTime) {
        String[] parts = logTime.split(" ");
        return parts.length >= 2 ? parts[0] + " " + parts[1] : logTime;
    }

    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
