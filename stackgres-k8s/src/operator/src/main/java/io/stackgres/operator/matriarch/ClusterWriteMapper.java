/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.HashMap;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
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
}
