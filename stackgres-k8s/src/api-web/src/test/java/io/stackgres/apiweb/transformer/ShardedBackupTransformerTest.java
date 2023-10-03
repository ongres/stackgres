/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupSpec;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedBackupTransformerTest {

  @Inject
  ShardedBackupTransformer transformer;

  public static TransformerTuple<ShardedBackupDto, StackGresShardedBackup> createShardedBackup() {

    StackGresShardedBackup source = new StackGresShardedBackup();
    ShardedBackupDto target = new ShardedBackupDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedBackupSpec.class,
            StackGresShardedBackupSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            ShardedBackupStatus.class,
            StackGresShardedBackupStatus.class
        );
    source.setStatus(status.source());
    target.setStatus(status.target());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testShardedBackupTransformation() {

    var tuple = createShardedBackup();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
