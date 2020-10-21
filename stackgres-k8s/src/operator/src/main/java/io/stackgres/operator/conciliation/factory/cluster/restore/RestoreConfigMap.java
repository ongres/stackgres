/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.restore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements ResourceGenerator<StackGresClusterContext> {

  private static final String RESTORE_SUFFIX = "-restore";

  private LabelFactory<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + RESTORE_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    if (context.getRestoreBackup().isPresent()) {
      var restoreBackup = context.getRestoreBackup().get();
      final Map<String, String> data = new HashMap<>();

      data.put("BACKUP_RESOURCE_VERSION",
          restoreBackup.getMetadata().getResourceVersion());
      data.put("RESTORE_BACKUP_ID",
          restoreBackup.getStatus().getInternalName());

      data.putAll(getBackupEnvVars(context,
          restoreBackup.getMetadata().getNamespace(),
          restoreBackup.getSpec().getSgCluster(),
          restoreBackup.getStatus().getBackupConfig()));

      final StackGresCluster cluster = context.getSource();
      Optional.ofNullable(cluster.getSpec())
          .map(StackGresClusterSpec::getInitData)
          .map(StackGresClusterInitData::getRestore)
          .map(StackGresClusterRestore::getDownloadDiskConcurrency)
          .ifPresent(downloadDiskConcurrency -> data.put(
              "WALG_DOWNLOAD_CONCURRENCY", convertEnvValue(downloadDiskConcurrency)));
      return Stream.of(new ConfigMapBuilder()
          .withNewMetadata()
          .withNamespace(cluster.getMetadata().getNamespace())
          .withName(name(context))
          .withLabels(labelFactory.patroniClusterLabels(cluster))
          .endMetadata()
          .withData(StackGresUtil.addMd5Sum(data))
          .build());
    } else {
      return Stream.of();
    }
  }

  @Override
  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.RESTORE_SECRET_PATH.path(context)
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private <T> String convertEnvValue(T value) {
    return value.toString();
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
