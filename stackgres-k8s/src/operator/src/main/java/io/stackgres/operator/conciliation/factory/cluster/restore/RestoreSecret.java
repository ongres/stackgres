/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.restore;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class RestoreSecret
    implements ResourceGenerator<StackGresClusterContext> {

  private static final String RESTORE_SECRET_SUFFIX = "-restore";

  private LabelFactory<StackGresCluster> labelFactory;

  private BackupEnvVarFactory envVarFactory;

  public static String name(ClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + RESTORE_SECRET_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    if (context.getRestoreBackup().isPresent()) {
      var restoreBackup = context.getRestoreBackup().get();
      Map<String, String> data = new HashMap<String, String>();

      data.put("BACKUP_RESOURCE_VERSION",
          restoreBackup.getMetadata().getResourceVersion());

      String backupNamespace = restoreBackup.getMetadata().getNamespace();

      final StackGresBackupConfigSpec backupConfig = restoreBackup.getStatus().getBackupConfig();
      data.putAll(envVarFactory.getSecretEnvVar(backupNamespace, backupConfig));

      final StackGresCluster cluster = context.getSource();
      return Stream.of(
          new SecretBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(context))
              .withLabels(labelFactory.clusterLabels(cluster))
              .endMetadata()
              .withType("Opaque")
              .withStringData(StackGresUtil.addMd5Sum(data))
              .build()
      );
    } else {
      return Stream.of();
    }
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setEnvVarFactory(BackupEnvVarFactory envVarFactory) {
    this.envVarFactory = envVarFactory;
  }
}
