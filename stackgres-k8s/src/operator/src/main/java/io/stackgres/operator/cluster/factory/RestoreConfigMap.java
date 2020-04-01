/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterOptionalResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterOptionalResourceStreamFactory {

  private static final String RESTORE_SUFFIX = "-restore";

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + RESTORE_SUFFIX);
  }

  @Override
  public Stream<Optional<HasMetadata>> streamOptionalResources(StackGresGeneratorContext context) {
    return Seq.of(context.getClusterContext().getRestoreContext()
        .map(restoreContext -> {
          final Map<String, String> data = new HashMap<>();

          data.put("BACKUP_RESOURCE_VERSION",
              restoreContext.getBackup().getMetadata().getResourceVersion());

          data.putAll(getBackupEnvVars(
              restoreContext.getBackup().getMetadata().getNamespace(),
              restoreContext.getBackup().getSpec().getSgCluster(),
              restoreContext.getBackup().getStatus().getBackupConfig()));

          putOrRemoveIfNull(data, "WALG_DOWNLOAD_CONCURRENCY",
              String.valueOf(restoreContext.getRestore().getDownloadDiskConcurrency()));

          return new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
              .withName(name(context.getClusterContext()))
              .withLabels(StackGresUtil.patroniClusterLabels(
                  context.getClusterContext().getCluster()))
              .withOwnerReferences(ImmutableList.of(
                  ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
              .endMetadata()
              .withData(StackGresUtil.addMd5Sum(data))
              .build();
        }));
  }

  @Override
  protected String getGcsCredentialsFilePath() {
    return ClusterStatefulSetPath.RESTORE_SECRET_PATH.path()
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private void putOrRemoveIfNull(Map<String, String> data, String key, String value) {
    if (value != null) {
      data.put(key, value);
    } else {
      data.remove(key);
    }
  }

}
