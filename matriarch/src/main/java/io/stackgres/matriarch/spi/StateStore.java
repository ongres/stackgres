package io.stackgres.matriarch.spi;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.spec.ClusterSpec;

import java.util.List;

/**
 * The durable <strong>source of truth</strong> (§3.2): desired cluster specs plus the
 * idempotency record that makes a retried create safe. Written synchronously before any
 * provisioning. Observed status lives in {@link StatusCache} (a lossy accelerant, never
 * truth).
 * Implementations could include {@code SQLite}, {@code etcd}, {@code K8sConfigMaps}, optional {@code CRDs}.
 *
 * <p>The store keeps only the <strong>latest spec per cluster</strong> — no history, and
 * (since the matriarch is the sole writer) no compare-and-swap. A revision/resourceVersion
 * is reintroduced only if a multi-writer backend needs CAS, or a client-facing change token
 * is required — until then there is nothing for it to carry.
 */
public interface StateStore {

    /**
     * The cluster's current desired spec, or {@code null} if none.
     */
    ClusterSpec getDesired(ClusterId id);

    /**
     * Every cluster's current desired spec.
     */
    List<ClusterSpec> listDesired();

    /**
     * Persist a new desired spec and, atomically, its idempotency key. Throws if a spec
     * with the same id already exists; a blank key is not recorded.
     */
    void createDesired(ClusterSpec spec, String idempotencyKey);

    /**
     * Replace an existing desired spec.
     */
    void updateDesired(ClusterSpec spec);

    /**
     * Remove a desired spec. Idempotent.
     */
    void deleteDesired(ClusterId id);

    /**
     * The cluster a prior create with this idempotency key produced, or {@code null} (§5.1).
     */
    ClusterId findByIdempotencyKey(String key);

    /**
     * Atomically record an idempotency key for a non-create mutation (e.g. delete), so the
     * operation runs only once. Returns {@code true} if newly recorded, {@code false} if the
     * key was already present (a retry). ({@code createDesired} records the key for creates.)
     */
    boolean recordIdempotency(String key, ClusterId clusterId);

    /**
     * Hold a cluster's superuser password by reference (§3.7): the plaintext lives here, never in
     * the desired {@link ClusterSpec}. Resolved back out via {@code GetClusterCredentials} and by the
     * bridge executor when it provisions. Cleared with {@link #deleteDesired}.
     */
    void putCredential(ClusterId id, String password);

    /**
     * The cluster's stored superuser password, or {@code null} if none.
     */
    String getCredential(ClusterId id);

}