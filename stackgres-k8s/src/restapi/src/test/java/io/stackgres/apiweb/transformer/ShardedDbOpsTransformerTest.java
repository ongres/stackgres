/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsSpec;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedDbOpsTransformerTest {

  @Inject
  ShardedDbOpsTransformer transformer;

  public static TransformerTuple<ShardedDbOpsDto, StackGresShardedDbOps> createShardedDbOps() {

    StackGresShardedDbOps source = new StackGresShardedDbOps();
    ShardedDbOpsDto target = new ShardedDbOpsDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedDbOpsSpec.class,
            StackGresShardedDbOpsSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedDbOpsStatus.class,
            StackGresShardedDbOpsStatus.class
        );
    source.setStatus(status.source());
    target.setStatus(status.target());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testShardedDbOpsTransformation() {

    var tuple = createShardedDbOps();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
