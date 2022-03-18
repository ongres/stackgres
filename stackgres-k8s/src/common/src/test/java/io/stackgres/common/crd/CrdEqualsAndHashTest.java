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
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.testutil.ResourceTestUtil;
import org.junit.jupiter.api.Test;

public class CrdEqualsAndHashTest {

  @Test
  void shouldStackGresCluster_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresCluster.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresPostgresConfig_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresPostgresConfig.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresPoolingConfig_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresPoolingConfig.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresBackupConfig_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresBackupConfig.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresBackup_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresBackup.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresDbOps_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresDbOps.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

  @Test
  void shouldStackGresDistributedLogs_haveEqualsAndHash() {
    var resource = ResourceTestUtil.createWithRandomData(StackGresDistributedLogs.class);
    ResourceTestUtil.assertEqualsAndHashCode(resource);
  }

}
