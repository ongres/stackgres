/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.backup.BackupSpec;
import io.stackgres.apiweb.dto.backup.BackupStatus;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class BackupTransformerTest {

  @Inject
  BackupTransformer transformer;

  public static TransformerTuple<BackupDto, StackGresBackup> createBackup() {

    StackGresBackup source = new StackGresBackup();
    BackupDto target = new BackupDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupSpec.class,
            StackGresBackupSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupStatus.class,
            StackGresBackupStatus.class
        );
    source.setStatus(status.source());
    target.setStatus(status.target());

    return new TransformerTuple<>(target, source);
  }

  @Test
  void testBackupTransformation() {

    var tuple = createBackup();
    TransformerTestUtil.assertTransformation(transformer, tuple);

  }
}
