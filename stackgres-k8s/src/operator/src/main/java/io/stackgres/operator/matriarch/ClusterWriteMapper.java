/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.HashMap;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.proto.api.v1.CreateClusterRequest;

/**
 * Maps an api.v1 {@link CreateClusterRequest} to a minimal {@code SGCluster} CR. Deliberately emits only
 * the few fields the request carries — the operator's admission mutators default the rest
 * ({@code sgInstanceProfile}, {@code postgresServices}, {@code replication}, {@code configurations}…),
 * exactly as they do for a hand-applied {@code kubectl} manifest, so this stays the same minimal shape as
 * {@code sgcluster-demo.yaml}. The operator (not matriarch) remains the source of truth; we only translate.
 */
final class ClusterWriteMapper {

  private ClusterWriteMapper() {
  }

  /**
   * The request as a minimal {@code SGCluster}. {@code replicas} is the api.v1 replica count (0 =
   * standalone primary), so {@code spec.instances = replicas + 1}. {@code version} is passed through
   * ("latest" resolves operator-side). {@code storageSize} defaults the PV the request doesn't carry.
   */
  static StackGresCluster toCluster(CreateClusterRequest req, String namespace, String storageSize) {
    StackGresCluster cr = new StackGresCluster();
    ObjectMetaBuilder meta = new ObjectMetaBuilder().withName(req.getName()).withNamespace(namespace);
    if (!req.getTagsMap().isEmpty()) {
      meta.withLabels(new HashMap<>(req.getTagsMap()));
    }
    cr.setMetadata(meta.build());

    StackGresClusterSpec spec = new StackGresClusterSpec();

    StackGresClusterPostgres postgres = new StackGresClusterPostgres();
    postgres.setVersion(req.getVersion() == null || req.getVersion().isBlank() ? "latest" : req.getVersion());
    spec.setPostgres(postgres);

    spec.setInstances(req.getReplicas() + 1);

    StackGresClusterPodsPersistentVolume pv = new StackGresClusterPodsPersistentVolume();
    pv.setSize(storageSize);
    StackGresClusterPods pods = new StackGresClusterPods();
    pods.setPersistentVolume(pv);
    spec.setPods(pods);

    cr.setSpec(spec);
    return cr;
  }

  /** A name+namespace-only CR, enough for fabric8 to address an existing object for deletion. */
  static StackGresCluster ref(String name, String namespace) {
    StackGresCluster cr = new StackGresCluster();
    cr.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
    return cr;
  }

  /**
   * An {@code SGDbOps} that asks the operator to roll-restart {@code clusterName}. {@code restart} is left
   * at defaults (the operator picks the method); the op's status conditions drive the accepted-then-watch.
   */
  /**
   * A valid Kubernetes object name for a restart's SGDbOps. Deterministic in {@code idempotencyKey} (a
   * resent restart reuses the name and attaches to the running op via 409), prefixed by the cluster name
   * so it always starts with a letter — an idempotency key is often a UUID, which starts with a digit and
   * is not a legal name on its own — then sanitized to DNS-1123 and capped at 63 chars.
   */
  static String restartOpName(String clusterName, String idempotencyKey) {
    String suffix = idempotencyKey == null || idempotencyKey.isBlank()
        ? UUID.randomUUID().toString().substring(0, 8)
        : idempotencyKey;
    String name = (clusterName + "-restart-" + suffix).toLowerCase().replaceAll("[^a-z0-9-]", "-");
    if (name.length() > 63) {
      name = name.substring(0, 63);
    }
    while (name.endsWith("-")) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  static StackGresDbOps restartDbOps(String opName, String namespace, String clusterName) {
    StackGresDbOps op = new StackGresDbOps();
    op.setMetadata(new ObjectMetaBuilder().withName(opName).withNamespace(namespace).build());
    StackGresDbOpsSpec spec = new StackGresDbOpsSpec();
    spec.setSgCluster(clusterName);
    spec.setOp("restart");
    spec.setRestart(new StackGresDbOpsRestart());
    op.setSpec(spec);
    return op;
  }
}
