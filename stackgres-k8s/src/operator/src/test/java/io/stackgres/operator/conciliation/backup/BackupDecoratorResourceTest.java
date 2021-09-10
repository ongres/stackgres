/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BackupDecoratorResourceTest {

  private static final int EXCEEDED_NAME_LIMIT = 35;
  @Inject
  BackupDecoratorResource decorator;

  @Test
  void shouldCreateANewClusteAndGetAnErrorDueNamingLimit() throws Exception {

    var resource = JsonUtil.readFromJson("backup/default.json",
        StackGresBackup.class);
    resource.getMetadata().setName(StringUtils.getRandomString(EXCEEDED_NAME_LIMIT));

    var cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    var backupConfig = JsonUtil.readFromJson("backup_config/default.json",
        StackGresBackupConfig.class);

    StackGresBackupContext context = ImmutableStackGresBackupContext.builder()
        .source(resource)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .build();

    List<HasMetadata> decorateResources = decorator.decorateResources(context);
    assertNotNull(decorateResources);
    System.out.println(decorateResources.toString());
  }

}
