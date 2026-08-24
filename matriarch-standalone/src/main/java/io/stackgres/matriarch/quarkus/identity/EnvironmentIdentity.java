package io.stackgres.matriarch.quarkus.identity;

import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.types.v1.ApiSurface;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * The identity of THIS standalone (bare-metal) matriarch as one api.v1 {@link Environment} — shared by
 * the api.v1 server ({@code StackGresApiResource}) and the cloud uplink ({@code CloudUplinkClient}) so a
 * matriarch presents ONE consistent id everywhere.
 *
 * <p>The id is resolved once: {@code stackgres.cloud.environment.id} config if set (supplied from
 * outside — an env var / the deployment's {@code .env} / a system property), otherwise a fresh id is
 * generated at startup. "local" was a poor default: non-unique across standalone matriarchs and
 * confusing once several dial into the cloud.
 *
 * <p>Generation mirrors the StackGres operator's <em>installation id</em> (introduced in 1.19): four
 * random bytes, Base64-encoded, truncated to 6 chars — URL-safe here since the id flows into REST paths
 * ({@code /environments/{id}}) and gRPC. This matriarch does NOT persist it (persistence belongs to
 * whatever provisions the deployment): a generated id is ephemeral and will differ across restarts, so
 * a long-lived deployment should set {@code stackgres.cloud.environment.id} explicitly to keep the same
 * environment across restarts (otherwise the cloud sees a new environment each boot).
 */
@ApplicationScoped
public class EnvironmentIdentity {

    private static final Logger LOG = Logger.getLogger(EnvironmentIdentity.class);

    @ConfigProperty(name = "stackgres.cloud.environment.id")
    Optional<String> configuredId;

    @ConfigProperty(name = "stackgres.cloud.environment.kind", defaultValue = "KIND_BARE_METAL")
    String configuredKind;

    private volatile String id;

    /** The resolved environment id (config if set, else a fresh id generated once for this run). */
    public synchronized String id() {
        if (id != null) {
            return id;
        }
        if (configuredId.isPresent() && !configuredId.get().isBlank()) {
            id = configuredId.get().trim();
            LOG.infof("environment id from config: %s", id);
            return id;
        }
        id = generateId();
        LOG.infof("environment id generated: %s (ephemeral — set stackgres.cloud.environment.id to keep it stable across restarts)", id);
        return id;
    }

    public Environment.Kind kind() {
        try {
            return Environment.Kind.valueOf(configuredKind);
        } catch (IllegalArgumentException e) {
            return Environment.Kind.KIND_BARE_METAL;
        }
    }

    /** This matriarch as a full api.v1 Environment (id, kind, advertised surfaces). */
    public Environment environment() {
        return Environment.newBuilder()
                .setId(Id.newBuilder().setValue(id()))
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
