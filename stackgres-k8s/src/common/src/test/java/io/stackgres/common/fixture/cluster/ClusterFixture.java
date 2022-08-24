/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.cluster;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.fixture.VersionedFixture;

public class ClusterFixture extends VersionedFixture<StackGresCluster> {

  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();

  public ClusterSchedulingFixture scheduling() {
    return new ClusterSchedulingFixture();
  }

  public PodListFixture podList() {
    return new PodListFixture();
  }

  public ClusterFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_DEFAULT_JSON);
    return this;
  }

  public ClusterFixture loadWithoutDistributedLogs() {
    fixture = readFromJson(STACKGRES_CLUSTER_WITHOUT_DISTRIBUTED_LOGS_JSON);
    return this;
  }

  public ClusterFixture loadUsingLoadBalancerIp() {
    fixture = readFromJson(STACKGRES_CLUSTER_USING_LOAD_BALANCER_IP_JSON);
    return this;
  }

  public ClusterFixture loadManagedSql() {
    fixture = readFromJson(STACKGRES_CLUSTER_MANAGED_SQL_JSON);
    return this;
  }

  public ClusterFixture loadSchedulingBackup() {
    fixture = readFromJson(STACKGRES_CLUSTER_SCHEDULING_BACKUP_JSON);
    return this;
  }

  public ClusterFixture empty() {
    fixture = new StackGresCluster();
    return this;
  }

  public ClusterFixture withSpec() {
    if (fixture.getSpec() == null) {
      fixture.setSpec(new StackGresClusterSpec());
    }
    return this;
  }

  public ClusterFixture withPods() {
    withSpec();
    if (fixture.getSpec().getPod() == null) {
      fixture.getSpec().setPod(new StackGresClusterPod());
    }
    return this;
  }

  public ClusterFixture withScheduling() {
    withPods();
    if (fixture.getSpec().getPod().getScheduling() == null) {
      fixture.getSpec().getPod().setScheduling(new StackGresClusterPodScheduling());
    }
    return this;
  }

  public ClusterFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    withScheduling();
    fixture.getSpec().getPod().getScheduling().setNodeAffinity(nodeAffinity);
    return this;
  }

  public ClusterFixture withLatestPostgresVersion() {
    fixture.getSpec().getPostgres().setVersion(POSTGRES_LATEST_VERSION);
    return this;
  }

  public StackGresClusterBuilder getBuilder() {
    return new StackGresClusterBuilder(fixture);
  }

}
