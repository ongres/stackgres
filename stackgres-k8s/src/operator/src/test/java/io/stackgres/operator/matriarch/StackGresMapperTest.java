/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfileSpec;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.InstanceRole;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import org.junit.jupiter.api.Test;

class StackGresMapperTest {

  @Test
  void mapsClusterFromCrAndProfile() {
    StackGresCluster cr = new StackGresCluster();
    cr.setMetadata(new ObjectMetaBuilder()
        .withUid("11111111-1111-1111-1111-111111111111")
        .withName("mycluster")
        .withNamespace("default")
        .addToLabels("team", "db")
        .build());
    StackGresClusterPostgres pg = new StackGresClusterPostgres();
    pg.setVersion("16.2");
    pg.setFlavor("vanilla");
    StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPostgres(pg);
    spec.setInstances(2);
    spec.setSgInstanceProfile("size-s");
    cr.setSpec(spec);
    StackGresClusterStatus status = new StackGresClusterStatus();
    status.setPostgresVersion("16.2");
    status.setInstances(2);
    StackGresClusterPodStatus p0 = new StackGresClusterPodStatus();
    p0.setName("mycluster-0");
    p0.setPrimary(true);
    StackGresClusterPodStatus p1 = new StackGresClusterPodStatus();
    p1.setName("mycluster-1");
    p1.setPrimary(false);
    status.setPodStatuses(List.of(p0, p1));
    cr.setStatus(status);

    StackGresInstanceProfileSpec pspec = new StackGresInstanceProfileSpec();
    pspec.setCpu("500m");
    pspec.setMemory("512Mi");
    StackGresInstanceProfile profile = new StackGresInstanceProfile();
    profile.setSpec(pspec);

    Cluster c = StackGresMapper.toCluster(cr, profile);

    assertEquals("11111111-1111-1111-1111-111111111111", c.spec().id().value());
    assertEquals("mycluster", c.spec().name());
    assertEquals(DatabaseEngine.POSTGRES, c.spec().engine());
    assertEquals("16.2", c.spec().version());
    assertEquals(RunStatus.HEALTHY, c.status().runStatus());
    assertEquals("default", c.spec().tags().get("namespace"));

    assertEquals(2, c.spec().instances().size());
    assertEquals(InstanceRole.PRIMARY, c.spec().instances().get(0).role());
    assertEquals(InstanceRole.REPLICA, c.spec().instances().get(1).role());
    // instance ids must be UUID-shaped (the CLI parses them)
    assertDoesNotThrow(() -> UUID.fromString(c.spec().instances().get(0).id().value()));

    InstanceStatus is0 = c.status().instances().get(0);
    assertEquals(0.5, is0.cpu(), 0.0001);
    assertEquals(512L * 1024 * 1024, is0.memory());
    assertEquals(ReplicationStatus.PRIMARY, is0.replication());
    assertEquals(ReplicationStatus.REPLICA, c.status().instances().get(1).replication());
  }

  @Test
  void babelfishMapsToIvoryAndSynthesizesInstancesWithoutPodStatuses() {
    StackGresCluster cr = new StackGresCluster();
    cr.setMetadata(new ObjectMetaBuilder()
        .withUid("22222222-2222-2222-2222-222222222222")
        .withName("bbf")
        .withNamespace("ns")
        .build());
    StackGresClusterPostgres pg = new StackGresClusterPostgres();
    pg.setVersion("15");
    pg.setFlavor("babelfish");
    StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPostgres(pg);
    spec.setInstances(1);
    spec.setSgInstanceProfile("p");
    cr.setSpec(spec);
    // no status → UNKNOWN, and instances synthesized from spec.instances

    Cluster c = StackGresMapper.toCluster(cr, null);

    assertEquals(DatabaseEngine.IVORY, c.spec().engine());
    assertEquals("15", c.spec().version());
    assertEquals(RunStatus.UNKNOWN, c.status().runStatus());
    assertEquals(1, c.spec().instances().size());
    assertEquals(InstanceRole.PRIMARY, c.spec().instances().get(0).role());
    assertEquals(0.0, c.status().instances().get(0).cpu(), 0.0001);
  }
}
