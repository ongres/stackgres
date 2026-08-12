package io.stackgres.matriarch.model.spec;
import io.stackgres.matriarch.model.ClusterId;

import java.util.List;
import java.util.Map;

/**
 * Desired cluster spec — the durable source of truth. Engine-independent and
 * instance-centric: a cluster <em>is</em> its instances (a standalone holds
 * exactly one PRIMARY). Carries no secret values — credentials are referenced
 * via {@link CredentialSpec} (§3.7). Mirrors the desired half of
 * {@code stackgres.api.v1.Cluster}.
 */
public record ClusterSpec(
        ClusterId id,
        String name,
        DatabaseEngine engine,
        String version,
        List<InstanceSpec> instances,
        CredentialSpec credential,
        TlsMode tls,
        EngineSpec engineSpec,
        Map<String, String> tags) {

    public boolean standalone() {
        return instances.size() == 1 && instances.get(0).role() == InstanceRole.PRIMARY;
    }
}
