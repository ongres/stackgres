package io.stackgres.matriarch.quarkus.store;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.EngineSpec;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.InstanceRole;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.spec.TlsMode;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes a {@link ClusterSpec} to/from a JSON string for the durable {@link SqliteStateStore},
 * using the JSON-P (Parsson) already on the classpath — no reflection, no extra dependency, and the
 * same provider the rest of the module (e.g. {@code DocirVersionCatalog}) uses. The store governs
 * serialization only; the strongly-typed model stays in the core (§3.2). The sealed {@link EngineSpec}
 * has exactly one impl ({@link PostgresSpec}); a new engine spec adds a case here.
 */
final class ClusterSpecCodec {

    private ClusterSpecCodec() {
    }

    static String toJson(ClusterSpec s) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("id", s.id().value());
        putString(b, "name", s.name());
        putString(b, "engine", s.engine() == null ? null : s.engine().name());
        putString(b, "version", s.version());
        JsonArrayBuilder instances = Json.createArrayBuilder();
        for (InstanceSpec i : s.instances()) {
            instances.add(instanceToJson(i));
        }
        b.add("instances", instances);
        if (s.credential() != null) {
            JsonObjectBuilder cb = Json.createObjectBuilder();
            putString(cb, "username", s.credential().username());
            cb.add("generate", s.credential().generate());
            b.add("credential", cb);
        }
        putString(b, "tls", s.tls() == null ? null : s.tls().name());
        if (s.engineSpec() instanceof PostgresSpec ps) {
            b.add("engineSpec", postgresToJson(ps));
        }
        b.add("tags", mapToJson(s.tags()));
        return b.build().toString();
    }

    static ClusterSpec fromJson(String json) {
        JsonObject o;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            o = reader.readObject();
        }
        List<InstanceSpec> instances = new ArrayList<>();
        if (o.containsKey("instances")) {
            for (JsonObject i : o.getJsonArray("instances").getValuesAs(JsonObject.class)) {
                instances.add(instanceFromJson(i));
            }
        }
        CredentialSpec credential = null;
        if (o.containsKey("credential") && !o.isNull("credential")) {
            JsonObject c = o.getJsonObject("credential");
            credential = new CredentialSpec(getString(c, "username"), c.getBoolean("generate", false));
        }
        EngineSpec engineSpec = (o.containsKey("engineSpec") && !o.isNull("engineSpec"))
                ? postgresFromJson(o.getJsonObject("engineSpec")) : null;
        return new ClusterSpec(
                new ClusterId(o.getString("id")),
                getString(o, "name"),
                enumValue(DatabaseEngine.class, getString(o, "engine")),
                getString(o, "version"),
                instances,
                credential,
                enumValue(TlsMode.class, getString(o, "tls")),
                engineSpec,
                mapFromJson(o, "tags"));
    }

    private static JsonObjectBuilder instanceToJson(InstanceSpec i) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("id", i.id().value());
        putString(b, "role", i.role() == null ? null : i.role().name());
        if (i.requestedPort() != null) {
            b.add("requestedPort", i.requestedPort());
        }
        putString(b, "listenAddress", i.listenAddress());
        if (i.engineSpec() instanceof PostgresSpec ps) {
            b.add("engineSpec", postgresToJson(ps));
        }
        return b;
    }

    private static InstanceSpec instanceFromJson(JsonObject i) {
        Integer port = (i.containsKey("requestedPort") && !i.isNull("requestedPort")) ? i.getInt("requestedPort") : null;
        EngineSpec engineSpec = (i.containsKey("engineSpec") && !i.isNull("engineSpec"))
                ? postgresFromJson(i.getJsonObject("engineSpec")) : null;
        return new InstanceSpec(
                new InstanceId(i.getString("id")),
                enumValue(InstanceRole.class, getString(i, "role")),
                port,
                getString(i, "listenAddress"),
                engineSpec);
    }

    private static JsonObjectBuilder postgresToJson(PostgresSpec p) {
        JsonArrayBuilder extensions = Json.createArrayBuilder();
        for (Extension e : p.extensions()) {
            JsonObjectBuilder eb = Json.createObjectBuilder();
            putString(eb, "name", e.name());
            putString(eb, "version", e.version());
            putString(eb, "revision", e.revision());
            extensions.add(eb);
        }
        return Json.createObjectBuilder().add("extensions", extensions).add("settings", mapToJson(p.settings()));
    }

    private static PostgresSpec postgresFromJson(JsonObject o) {
        List<Extension> extensions = new ArrayList<>();
        if (o.containsKey("extensions")) {
            for (JsonObject e : o.getJsonArray("extensions").getValuesAs(JsonObject.class)) {
                extensions.add(new Extension(getString(e, "name"), getString(e, "version"), getString(e, "revision")));
            }
        }
        return new PostgresSpec(extensions, mapFromJson(o, "settings"));
    }

    private static JsonObjectBuilder mapToJson(Map<String, String> map) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (map != null) {
            map.forEach((k, v) -> putString(b, k, v));
        }
        return b;
    }

    private static Map<String, String> mapFromJson(JsonObject o, String key) {
        Map<String, String> map = new LinkedHashMap<>();
        if (o.containsKey(key) && !o.isNull(key)) {
            JsonObject m = o.getJsonObject(key);
            for (String k : m.keySet()) {
                map.put(k, m.isNull(k) ? null : m.getString(k));
            }
        }
        return map;
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

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name) {
        return name == null ? null : Enum.valueOf(type, name);
    }
}
