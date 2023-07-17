/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatus;
import io.stackgres.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedClusterTransformerTest {

  @Inject
  ShardedClusterTransformer transformer;

  public static TransformerTuple<ShardedClusterDto, StackGresShardedCluster>
      createShardedCluster() {
    StackGresShardedCluster source = new StackGresShardedCluster();
    ShardedClusterDto target = new ShardedClusterDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedClusterSpec.class,
            StackGresShardedClusterSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedClusterStatus.class,
            StackGresShardedClusterStatus.class
        );
    status.target().setClusters(List.of(StringUtils.getRandomClusterName()));
    source.setStatus(status.source());
    target.setStatus(status.target());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testShardedClusterTransformation() {
    var tuple = createShardedCluster();

    tuple.source().getSpec().getShards().setClusters(3);
    tuple.target().getSpec().getShards().setClusters(3);
    tuple.target().getStatus().setClusters(List.of(
        StackGresShardedClusterForCitusUtil.getCoordinatorClusterName(tuple.source()),
        StackGresShardedClusterForCitusUtil.getShardClusterName(tuple.source(), 0),
        StackGresShardedClusterForCitusUtil.getShardClusterName(tuple.source(), 1),
        StackGresShardedClusterForCitusUtil.getShardClusterName(tuple.source(), 2)));
    TransformerTestUtil.assertTransformation(transformer, tuple);
  }

}
