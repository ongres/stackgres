/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterResourceStreamFactory {

  private static final String RESTORE_SUFFIX = "-restore";

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + RESTORE_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    final Map<String, String> data = new HashMap<>();

    context.getClusterContext().getRestoreContext().ifPresent(restoreContext -> {
      data.putAll(getBackupEnvVars(
          restoreContext.getBackup().getMetadata().getNamespace(),
          restoreContext.getBackup().getSpec().getCluster(),
          restoreContext.getBackup().getStatus().getBackupConfig()));
      putOrRemoveIfNull(data, "WALG_DOWNLOAD_CONCURRENCY",
          String.valueOf(restoreContext.getRestore().getDownloadDiskConcurrency()));

      putOrRemoveIfNull(data, "WALG_COMPRESSION_METHOD", restoreContext.getBackup().getStatus()
          .getBackupConfig().getCompressionMethod());

      putOrRemoveIfNull(data, "RESTORE_BACKUP_ID", restoreContext.getBackup()
          .getStatus().getName());
    });

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withName(name(context.getClusterContext()))
        .withLabels(ResourceUtil.patroniClusterLabels(context.getClusterContext().getCluster()))
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
        .endMetadata()
        .withData(ResourceUtil.addMd5Sum(data))
        .build());
  }

  @Override
  protected String getGcsCredentialsFilePath() {
    return ClusterStatefulSet.GCS_RESTORE_CONFIG_PATH
        + "/" + ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_FILE_NAME;
  }

  private void putOrRemoveIfNull(Map<String, String> data, String key, String value) {
    if (value != null) {
      data.put(key, value);
    } else {
      data.remove(key);
    }
  }

}
