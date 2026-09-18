package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.time.Instant;

/**
 * Serializes a {@link ClusterEvent} to/from JSON for the durable {@link SqliteClusterEventLog}, using
 * the JSON-P already on the classpath (as {@code ClusterSpecCodec} does). A {@code type} discriminator
 * selects the sealed subtype on the way back in; a new event record adds one case here.
 */
final class ClusterEventCodec {

    private ClusterEventCodec() {
    }

    /** The stable discriminator persisted with each event (the record's simple name). */
    static String type(ClusterEvent e) {
        return e.getClass().getSimpleName();
    }

    static String toJson(ClusterEvent e) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("type", type(e))
                .add("timestamp", e.timestamp().toString())
                .add("clusterId", e.clusterId().value());
        switch (e) {
            case ClusterEvent.ClusterAccepted a -> {
                putString(b, "name", a.name());
                putString(b, "version", a.version());
                b.add("standalone", a.standalone());
            }
            case ClusterEvent.ClusterFailed f -> putString(b, "reason", f.reason());
            case ClusterEvent.ClusterRecovered r -> putString(b, "name", r.name());
            case ClusterEvent.ClusterObserved o -> putString(b, "name", o.name());
            default -> { /* Healthy/Starting/Stopping/Restarting/Deleting/Deleted carry no extra fields */ }
        }
        return b.build().toString();
    }

    static ClusterEvent fromJson(String json) {
        JsonObject o;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            o = reader.readObject();
        }
        Instant ts = Instant.parse(o.getString("timestamp"));
        ClusterId id = new ClusterId(o.getString("clusterId"));
        String type = o.getString("type");
        return switch (type) {
            case "ClusterAccepted" -> new ClusterEvent.ClusterAccepted(ts, id,
                    getString(o, "name"), getString(o, "version"), o.getBoolean("standalone", false));
            case "ClusterHealthy" -> new ClusterEvent.ClusterHealthy(ts, id);
            case "ClusterFailed" -> new ClusterEvent.ClusterFailed(ts, id, getString(o, "reason"));
            case "ClusterStarting" -> new ClusterEvent.ClusterStarting(ts, id);
            case "ClusterStopping" -> new ClusterEvent.ClusterStopping(ts, id);
            case "ClusterRestarting" -> new ClusterEvent.ClusterRestarting(ts, id);
            case "ClusterDeleting" -> new ClusterEvent.ClusterDeleting(ts, id);
            case "ClusterDeleted" -> new ClusterEvent.ClusterDeleted(ts, id);
            case "ClusterRecovered" -> new ClusterEvent.ClusterRecovered(ts, id, getString(o, "name"));
            case "ClusterObserved" -> new ClusterEvent.ClusterObserved(ts, id, getString(o, "name"));
            default -> throw new IllegalArgumentException("unknown cluster event type: " + type);
        };
    }

    private static void putString(JsonObjectBuilder b, String key, String value) {
        if (value == null) {
            b.addNull(key);
        } else {
            b.add(key, value);
        }
    }

    private static String getString(JsonObject o, String key) {
        return (o.containsKey(key) && !o.isNull(key)) ? o.getString(key) : null;
    }
}
