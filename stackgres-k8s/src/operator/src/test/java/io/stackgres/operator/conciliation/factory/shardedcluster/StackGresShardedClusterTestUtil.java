/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.testutil.ModelTestUtil.createWithRandomData;

import java.util.List;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.testutil.ModelTestUtil;
import org.jooq.lambda.Seq;

class StackGresShardedClusterTestUtil {

  static StackGresShardedCluster createShardedCluster() {
    var shardedCluster = createWithRandomData(StackGresShardedCluster.class);
    List<String> sgBackups = Seq.range(0, shardedCluster.getSpec().getShards().getClusters() + 1)
        .<String>map(index -> ModelTestUtil.generateRandom(String.class))
        .toList();
    List<String> hosts = Seq.range(0, shardedCluster.getSpec().getShards().getClusters() + 1)
        .<String>map(index -> ModelTestUtil.generateRandom(String.class))
        .toList();
    List<Integer> ports = Seq.range(0, shardedCluster.getSpec().getShards().getClusters() + 1)
        .<Integer>map(index -> ModelTestUtil.generateRandom(Integer.class))
        .toList();
    List<String> paths = Seq.range(0, shardedCluster.getSpec().getShards().getClusters() + 1)
        .<String>map(index -> ModelTestUtil.generateRandom(String.class))
        .toList();
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal().setHosts(hosts);
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal().setPorts(ports);
    shardedCluster.getSpec().getReplicateFrom().getStorage().setPaths(paths);
    shardedCluster.getStatus().setSgBackups(sgBackups);
    Seq.seq(shardedCluster.getSpec().getShards().getOverrides())
        .zipWithIndex()
        .forEach(override -> override.v1.setIndex(
            shardedCluster.getSpec().getShards().getClusters() - override.v2.intValue() - 1));
    shardedCluster.getSpec().getShards().setOverrides(
        shardedCluster.getSpec().getShards().getOverrides()
        .subList(0, Math.min(
            shardedCluster.getSpec().getShards().getOverrides().size(),
            shardedCluster.getSpec().getShards().getClusters())));
    return shardedCluster;
  }

}
