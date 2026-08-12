package io.stackgres.matriarch.quarkus.version;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ExtensionCatalog} backed by the DOCIR extension catalog — a port of the old matriarch's
 * {@code DocirClient.fetchExtensions} + {@code PostgresExtensions}. For a given engine + exact
 * Postgres version it fetches the available extensions (each with an ordered list of versions, the
 * first being the default) and resolves each requested extension to its exact version + revision.
 * Cached per (engine, version); fails loudly if DOCIR is unreachable.
 */
@ApplicationScoped
public class DocirExtensionCatalog implements ExtensionCatalog {

    private static final System.Logger LOG = System.getLogger(DocirExtensionCatalog.class.getName());

    /** One available extension version and its platform revision. */
    private record ExtVersion(String version, String revision) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final URI extensionsUri;
    private final Map<String, Map<String, List<ExtVersion>>> cache = new ConcurrentHashMap<>();

    public DocirExtensionCatalog() {
        String base = env("STACKGRES_DOCIR_API_URL", "https://sgcr.dev:1443/api/stackgres/v1/");
        this.extensionsUri = URI.create(base + "extensions");
    }

    @Override
    public List<Extension> resolveExtensions(DatabaseEngine engine, String version, List<Extension> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Map<String, List<ExtVersion>> available = cache.computeIfAbsent(
                flavorId(engine) + "|" + version, k -> fetch(engine, version));
        return requested.stream().map(r -> {
            List<ExtVersion> versions = available.get(r.name());
            if (versions == null || versions.isEmpty()) {
                throw new IllegalArgumentException(
                        "Extension " + r.name() + " is not available for " + flavorId(engine) + " " + version);
            }
            ExtVersion chosen = (r.version() == null || r.version().isBlank())
                    ? versions.get(0)   // default = first
                    : versions.stream().filter(v -> v.version().equals(r.version())).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Extension " + r.name() + " version " + r.version() + " is not available"));
            return new Extension(r.name(), chosen.version(), chosen.revision());
        }).toList();
    }

    @Override
    public List<Extension> availableExtensions(DatabaseEngine engine, String version) {
        Map<String, List<ExtVersion>> available = cache.computeIfAbsent(
                flavorId(engine) + "|" + version, k -> fetch(engine, version));
        return available.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new Extension(e.getKey(), v.version(), v.revision())))
                .toList();
    }

    private Map<String, List<ExtVersion>> fetch(DatabaseEngine engine, String pgVersion) {
        try {
            String[] parts = pgVersion.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Version has no major & minor: " + pgVersion);
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(extensionsUri + "?flavor=" + flavorId(engine)
                            + "&major=" + parts[0] + "&minor=" + parts[1]))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                Map<String, List<ExtVersion>> byName = new LinkedHashMap<>();
                for (JsonObject o : json.getJsonArray("extensions").getValuesAs(JsonObject.class)) {
                    if (!o.containsKey("name") || !o.containsKey("extensionVersions")) {
                        continue;
                    }
                    List<ExtVersion> versions = o.getJsonArray("extensionVersions").getValuesAs(JsonObject.class).stream()
                            .map(e -> new ExtVersion(e.getString("version"),
                                    e.getJsonArray("platforms").getJsonObject(0).getString("revision")))
                            .toList();
                    byName.put(o.getString("name"), versions);
                }
                LOG.log(Level.INFO, "Fetched {0} extensions for {1} {2} from DOCIR",
                        byName.size(), flavorId(engine), pgVersion);
                return byName;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Loading extensions from DOCIR failed: {0}", e.getMessage());
            throw new IllegalStateException("Could not load extensions from central repository (DOCIR)", e);
        }
    }

    private static String flavorId(DatabaseEngine engine) {
        return engine == DatabaseEngine.IVORY ? "ivorysql" : "postgres";
    }

    private static String env(String key, String fallback) {
        String v = System.getProperty(key, System.getenv(key));
        return v == null || v.isBlank() ? fallback : v;
    }
}
