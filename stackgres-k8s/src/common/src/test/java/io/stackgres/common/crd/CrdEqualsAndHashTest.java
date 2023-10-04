/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CrdEqualsAndHashTest {

  @ParameterizedTest
  @ValueSource(classes = {
      StackGresCluster.class,
      StackGresProfile.class,
      StackGresPostgresConfig.class,
      StackGresPoolingConfig.class,
      StackGresBackupConfig.class,
      StackGresBackup.class,
      StackGresDbOps.class,
      StackGresDistributedLogs.class,
      StackGresObjectStorage.class,
      StackGresScript.class,
      StackGresShardedCluster.class,
      StackGresShardedBackup.class,
      StackGresShardedDbOps.class,
  })
  void crdShouldHaveEqualsAndHash(Class<?> resourceClazz) {
    var resource = ModelTestUtil.createWithRandomData(resourceClazz);
    ModelTestUtil.assertEqualsAndHashCode(resource);
    var anotherResource = ModelTestUtil.createWithRandomData(resourceClazz);
    assertNotEquals(anotherResource, resource);
  }

}
