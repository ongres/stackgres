package io.stackgres.matriarch.quarkus.identity;

import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.types.v1.ApiSurface;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * The identity of THIS standalone (bare-metal) matriarch as one api.v1 {@link Environment} — shared by
 * the api.v1 server ({@code StackGresApiResource}) and the cloud uplink ({@code CloudUplinkClient}) so a
 * matriarch presents ONE consistent identity everywhere. Two parts:
 *
 * <ul>
 *   <li>The <b>id</b> — a stable installation id the cloud keys its cache by; users never address an
 *       environment by it. Resolved once: {@code stackgres.cloud.environment.id} config if set, else
 *       the id persisted by an earlier run, else a fresh one. Generation mirrors the StackGres
 *       operator's <em>installation id</em>: four random bytes, Base64-encoded, 6 chars. With the
 *       durable store ({@code matriarch.store=sqlite}) a generated id is persisted next to the database
 *       file ({@code <db path>.environment-id}) so restarts keep the same environment — the cloud assigns
 *       a name per id, so a new id on every boot would pile up {@code name-1}, {@code name-2}, … entries.
 *       With the in-memory store (dev/tests) it stays ephemeral.</li>
 *   <li>The <b>name</b> — the environment's user-facing identifier: {@code stackgres.cloud.environment.name}
 *       config if set, else this host's name, else the id. Toward the cloud it is only a
 *       <em>proposal</em>: the cloud owns the per-tenant name registry, de-duplicates it ({@code name},
 *       {@code name-1}, …) and lets the user rename it, so the name the cloud serves can differ. This
 *       matriarch's own API serves (and accepts) the name resolved here.</li>
 * </ul>
 */
@ApplicationScoped
public class EnvironmentIdentity {

    private static final Logger LOG = Logger.getLogger(EnvironmentIdentity.class);

    @ConfigProperty(name = "stackgres.cloud.environment.id")
    Optional<String> configuredId;

    @ConfigProperty(name = "stackgres.cloud.environment.name")
    Optional<String> configuredName;

    @ConfigProperty(name = "stackgres.cloud.environment.kind", defaultValue = "KIND_BARE_METAL")
    String configuredKind;

    @ConfigProperty(name = "matriarch.store", defaultValue = "memory")
    String store;

    @ConfigProperty(name = "matriarch.store.sqlite.path", defaultValue = "matriarch.db")
    String sqlitePath;

    private volatile String id;
    private volatile String name;

    /**
     * The resolved environment id: config if set, else the id persisted by an earlier run (durable store
     * only), else a fresh id generated once — and persisted, when the store is durable.
     */
    public synchronized String id() {
        if (id != null) {
            return id;
        }
        if (configuredId.isPresent() && !configuredId.get().isBlank()) {
            id = configuredId.get().trim();
            LOG.infof("environment id from config: %s", id);
            return id;
        }
        Path file = idFile();
        if (file == null) {
            id = generateId();
            LOG.infof("environment id generated: %s (ephemeral — the in-memory store keeps nothing across restarts)", id);
            return id;
        }
        try {
            if (Files.isRegularFile(file)) {
                String persisted = Files.readString(file, StandardCharsets.UTF_8).strip();
                if (!persisted.isEmpty()) {
                    id = persisted;
                    LOG.infof("environment id from %s: %s", file, id);
                    return id;
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warnf("could not read the environment id from %s: %s", file, e.getMessage());
        }
        id = generateId();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, id + System.lineSeparator(), StandardCharsets.UTF_8);
            LOG.infof("environment id generated: %s (persisted to %s)", id, file);
        } catch (IOException | RuntimeException e) {
            LOG.warnf("environment id generated: %s — could not persist it to %s (%s); it will change on restart "
                    + "unless stackgres.cloud.environment.id is set", id, file, e.getMessage());
        }
        return id;
    }

    /** Where a generated id is kept across restarts, or {@code null} when the store is not durable. */
    private Path idFile() {
        if (!"sqlite".equalsIgnoreCase(store) || sqlitePath == null || sqlitePath.isBlank()) {
            return null;
        }
        return Path.of(sqlitePath + ".environment-id");
    }

    /** The environment's name: config if set, else this host's name, else the id. Resolved once. */
    public synchronized String name() {
        if (name != null) {
            return name;
        }
        if (configuredName.isPresent() && !configuredName.get().isBlank()) {
            name = configuredName.get().trim();
            LOG.infof("environment name from config: %s", name);
            return name;
        }
        String host = hostName();
        name = host != null ? host : id();
        LOG.infof("environment name: %s (%s — set stackgres.cloud.environment.name to choose one)",
                name, host != null ? "this host's name" : "the environment id");
        return name;
    }

    /**
     * True if {@code selector} addresses this environment: empty (the only environment here), its name,
     * or its id.
     */
    public boolean matches(String selector) {
        return selector == null || selector.isBlank() || selector.equals(name()) || selector.equals(id());
    }

    private static String hostName() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (host != null && !host.isBlank() && !"localhost".equalsIgnoreCase(host)) {
                return host.strip();
            }
        } catch (IOException | RuntimeException e) {
            LOG.debugf("could not resolve this host's name: %s", e.getMessage());
        }
        return null;
    }

    public Environment.Kind kind() {
        try {
            return Environment.Kind.valueOf(configuredKind);
        } catch (IllegalArgumentException e) {
            return Environment.Kind.KIND_BARE_METAL;
        }
    }

    /** This matriarch as a full api.v1 Environment (id, name, kind, advertised surfaces). */
    public Environment environment() {
        return Environment.newBuilder()
                .setId(Id.newBuilder().setValue(id()))
                .setName(name())
                .setKind(kind())
                .addSurface(ApiSurface.API_SURFACE_CLUSTER_LIFECYCLE)
                .addSurface(ApiSurface.API_SURFACE_EVENTS)
                .addSurface(ApiSurface.API_SURFACE_CATALOG)
                .addSurface(ApiSurface.API_SURFACE_LOG_TAIL)
                .build();
    }

    /**
     * Mirror the operator's installation-id generator: 4 random bytes, Base64, first 6 chars (URL-safe).
     */
    private static String generateId() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 6);
    }
}
