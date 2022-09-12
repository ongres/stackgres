/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CrdIgnoreUnknownPropertiesTest {

  @ParameterizedTest
  @ValueSource(classes = {
    StackGresCluster.class,
    StackGresPostgresConfig.class,
    StackGresPoolingConfig.class,
    StackGresBackupConfig.class,
    StackGresBackup.class,
    StackGresDbOps.class,
    StackGresDistributedLogs.class,
    StackGresObjectStorage.class,
    StackGresScript.class,
  })
  void crdShouldInoreUnknownProperties(Class<?> resourceClazz) {
    ModelTestUtil.assertJsonInoreUnknownProperties(resourceClazz);
  }

}
