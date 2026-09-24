/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.proto.api.v1.CreateClusterRequest;
import org.junit.jupiter.api.Test;

/**
 * The api.v1 {@code CreateClusterRequest} -> minimal {@code SGCluster} mapping: only the fields the
 * request carries are emitted (mirroring {@code sgcluster-demo.yaml}); the operator's admission mutators
 * default the rest. {@code replicas} (0 = standalone) becomes {@code instances = replicas + 1}, a blank
 * version falls back to "latest", and the PV size the request lacks is defaulted by the caller.
 */
class ClusterWriteMapperTest {

  @Test
  void mapsTheDemoRequestToTheMinimalCr() {
    CreateClusterRequest req = CreateClusterRequest.newBuilder()
        .setName("demo")
        .setVersion("latest")
        .setReplicas(0)
        .build();

    StackGresCluster cr = ClusterWriteMapper.toCluster(req, "demo-ns", "1Gi");

    assertEquals("demo", cr.getMetadata().getName());
    assertEquals("demo-ns", cr.getMetadata().getNamespace());
    assertEquals("latest", cr.getSpec().getPostgres().getVersion());
    assertEquals(1, cr.getSpec().getInstances());                       // replicas 0 -> one primary
    assertEquals("1Gi", cr.getSpec().getPods().getPersistentVolume().getSize());
    assertTrue(cr.getMetadata().getLabels() == null || cr.getMetadata().getLabels().isEmpty(),
        "no tags -> no labels");
  }

  @Test
  void blankVersionFallsBackToLatest() {
    CreateClusterRequest req = CreateClusterRequest.newBuilder().setName("c").setVersion("").build();
    assertEquals("latest", ClusterWriteMapper.toCluster(req, "ns", "1Gi").getSpec().getPostgres().getVersion());
  }

  @Test
  void replicasBecomeInstancesPlusOneAndTagsBecomeLabels() {
    CreateClusterRequest req = CreateClusterRequest.newBuilder()
        .setName("ha")
        .setVersion("17")
        .setReplicas(2)
        .putTags("team", "db")
        .build();

    StackGresCluster cr = ClusterWriteMapper.toCluster(req, "ns", "10Gi");

    assertEquals(3, cr.getSpec().getInstances());                       // 2 replicas + 1 primary
    assertEquals("17", cr.getSpec().getPostgres().getVersion());
    assertEquals("10Gi", cr.getSpec().getPods().getPersistentVolume().getSize());
    assertEquals("db", cr.getMetadata().getLabels().get("team"));
  }

  @Test
  void refCarriesOnlyNameAndNamespace() {
    StackGresCluster ref = ClusterWriteMapper.ref("demo", "demo-ns");
    assertEquals("demo", ref.getMetadata().getName());
    assertEquals("demo-ns", ref.getMetadata().getNamespace());
    assertNull(ref.getSpec());
  }
}
