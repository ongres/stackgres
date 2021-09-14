/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.RequiredResourceDecoratorTestHelper;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BackupRequiredResourceDecoratorTest extends RequiredResourceDecoratorTestHelper {

  @Inject
  BackupRequiredResourceDecorator resourceDecorator;

  private StackGresBackup crd;

  private StackGresCluster cluster;

  private StackGresBackupConfig backupConfig;

  private ImmutableStackGresBackupContext context;

  private List<HasMetadata> decorateResources;

  @BeforeEach
  public void setup() {
    this.crd = JsonUtil.readFromJson("backup/default.json",
        StackGresBackup.class);
    this.cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
    this.backupConfig = JsonUtil.readFromJson("backup_config/default.json",
        StackGresBackupConfig.class);

  }

  @Test
  void shouldCreateResourceSuccessfully_OnceUsingTheCurrentCrdMaxLength() throws Exception {

    withSelectedCrd("SGBackup.yaml");

    String validResourceName = StringUtils.getRandomClusterName(withCurrentCrdMaxLength());
    crd.getMetadata().setName(validResourceName);
    this.context = ImmutableStackGresBackupContext.builder()
        .source(crd)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .build();
    this.decorateResources = resourceDecorator.decorateResources(context);
    decorateResources.stream().forEach(
        resource -> {
          resource.getMetadata().getLabels().entrySet().stream().forEach(label -> {
            asserThatLabelIsComplaint(label);
          });
          assertThatStatefulSetResourceLabelsAreComplaints(resource);
          assertThatCronJobResourceLabelsAreComplaints(resource);
          assertThatJobResourceLabelsAreComplaints(resource);
        });
  }

  

}
