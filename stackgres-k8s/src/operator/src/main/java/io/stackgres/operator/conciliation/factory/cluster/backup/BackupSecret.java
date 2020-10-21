/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class BackupSecret
    implements ResourceGenerator<StackGresClusterContext> {

  private static final String BACKUP_SECRET_SUFFIX = "-backup";

  private LabelFactory<StackGresCluster> labelFactory;

  private BackupEnvVarFactory backupEnvVarFactory;

  public static String name(ClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + BACKUP_SECRET_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    Map<String, String> data = new HashMap<String, String>();

    context.getBackupConfig().ifPresent(backupConfig -> {
      data.put("BACKUP_CONFIG_RESOURCE_VERSION",
          backupConfig.getMetadata().getResourceVersion());
      data.putAll(backupEnvVarFactory.getSecretEnvVar(backupConfig));
    });

    StackGresCluster cluster = context.getSource();

    return Seq.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .endMetadata()
        .withType("Opaque")
        .withStringData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setBackupEnvVarFactory(BackupEnvVarFactory backupEnvVarFactory) {
    this.backupEnvVarFactory = backupEnvVarFactory;
  }
}
