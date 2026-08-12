package io.stackgres.matriarch.quarkus.version;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.spi.VersionCatalog;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link VersionCatalog} backed by the DOCIR image catalog — a port of the old matriarch's
 * {@code DocirClient} + {@code PostgresVersions}. Fetches the available engine versions over HTTP
 * and resolves a description (blank / "17" / "17.4") to the exact version (e.g. "17.4"). Versions
 * are cached per engine (lazily); the catalog fails loudly if DOCIR is unreachable.
 */
@ApplicationScoped
public class DocirVersionCatalog implements VersionCatalog {

    private static final System.Logger LOG = System.getLogger(DocirVersionCatalog.class.getName());

    /** A DOCIR version entry: {@code version} is exact (17.4), {@code label} is the selector (17.latest). */
    private record Version(String version, String label, boolean latest) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final URI versionsUri;
    private final Map<DatabaseEngine, List<Version>> cache = new ConcurrentHashMap<>();

    public DocirVersionCatalog() {
        String base = env("STACKGRES_DOCIR_API_URL", "https://sgcr.dev:1443/api/stackgres/v1/");
        this.versionsUri = URI.create(base + "versions");
    }

    @Override
    public String resolveVersion(DatabaseEngine engine, String versionDescription) {
        List<Version> available = cache.computeIfAbsent(engine, this::fetch);
        if (available.isEmpty()) {
            throw new IllegalArgumentException("no versions available for " + flavorId(engine));
        }

        if (versionDescription == null || versionDescription.isBlank()) {
            return available.stream().filter(Version::latest).findAny()
                    .orElse(available.get(available.size() - 1)).version();   // latest, else newest
        }
        // A major-only number ("17") selects the latest of that major via the "<major>.latest" label.
        String label = versionDescription.trim();
        if (label.chars().allMatch(Character::isDigit)) {
            label = label + ".latest";
        }
        final String wanted = label;
        return available.stream()
                .filter(v -> v.label().equals(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + versionDescription + " is not known or available (yet)"))
                .version();
    }

    @Override
    public List<String> availableVersions(DatabaseEngine engine) {
        return cache.computeIfAbsent(engine, this::fetch).stream().map(Version::label).toList();
    }

    private List<Version> fetch(DatabaseEngine engine) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(versionsUri + "?flavor=" + flavorId(engine) + "&tshirt-size=full"))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                List<Version> versions = json.getJsonArray("versions").getValuesAs(JsonObject.class).stream()
                        .filter(o -> o.containsKey("version") && o.containsKey("label"))
                        .map(o -> new Version(o.getString("version"), o.getString("label"), o.getBoolean("isLatest", false)))
                        .sorted(Comparator.comparing(Version::version, Comparator.reverseOrder()))
                        .toList();
                LOG.log(Level.INFO, "Fetched {0} {1} versions from DOCIR", versions.size(), flavorId(engine));
                return versions;   // empty is a valid answer — the engine simply has no versions available
            }
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Loading versions from DOCIR failed: {0}", e.getMessage());
            throw new IllegalStateException("Could not load versions from central repository (DOCIR)", e);
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
