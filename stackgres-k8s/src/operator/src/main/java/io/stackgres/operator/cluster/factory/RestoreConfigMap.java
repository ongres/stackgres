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
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterOptionalResourceStreamFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterOptionalResourceStreamFactory {

  private static final String RESTORE_SUFFIX = "-restore";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + RESTORE_SUFFIX);
  }

  @Override
  public Stream<Optional<HasMetadata>> streamOptionalResources(StackGresClusterContext context) {
    return Seq.of(context.getRestoreContext()
        .map(restoreContext -> {
          final Map<String, String> data = new HashMap<>();

          data.put("BACKUP_RESOURCE_VERSION",
              restoreContext.getBackup().getMetadata().getResourceVersion());
          data.put("RESTORE_BACKUP_ID",
              restoreContext.getBackup().getStatus().getInternalName());

          data.putAll(getBackupEnvVars(context,
              restoreContext.getBackup().getMetadata().getNamespace(),
              restoreContext.getBackup().getSpec().getSgCluster(),
              restoreContext.getBackup().getStatus().getBackupConfig()));

          Optional.ofNullable(restoreContext.getRestore().getDownloadDiskConcurrency())
              .ifPresent(downloadDiskConcurrency -> data.put(
                  "WALG_DOWNLOAD_CONCURRENCY", convertEnvValue(downloadDiskConcurrency)));

          final StackGresCluster cluster = context.getCluster();
          final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(context);
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(context))
              .withLabels(labelFactory.patroniClusterLabels(cluster))
              .withOwnerReferences(context.getOwnerReferences())
              .endMetadata()
              .withData(StackGresUtil.addMd5Sum(data))
              .build();
        }));
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
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
