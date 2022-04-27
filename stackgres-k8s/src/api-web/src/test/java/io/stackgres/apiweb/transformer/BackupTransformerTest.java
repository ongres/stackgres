/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.backup.BackupSpec;
import io.stackgres.apiweb.dto.backup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackupTransformerTest {

  @Inject
  BackupTransformer transformer;

  public static TransformerTuple<BackupDto, StackGresBackup> createBackup() {

    StackGresBackup source = new StackGresBackup();
    BackupDto target = new BackupDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.getSource());
    target.setMetadata(metadata.getTarget());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupSpec.class,
            StackGresBackupSpec.class
        );
    source.setSpec(spec.getSource());
    target.setSpec(spec.getTarget());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupStatus.class,
            StackGresBackupStatus.class
        );
    source.setStatus(status.getSource());
    target.setStatus(status.getTarget());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testBackupTransformation() {

    var tuple = createBackup();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
