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
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class BackupConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterResourceStreamFactory {

  private static final String BACKUP_SUFFIX = "-backup";

  public static String backupName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + BACKUP_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    final Map<String, String> data = new HashMap<>();

    context.getClusterContext().getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .ifPresent(backupConfig -> {
          data.put("BACKUP_CONFIG_RESOURCE_VERSION",
              backupConfig.getMetadata().getResourceVersion());
          data.putAll(getBackupEnvVars(
              context.getClusterContext().getCluster().getMetadata().getNamespace(),
              context.getClusterContext().getCluster().getMetadata().getName(),
              backupConfig.getSpec()));
        });

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withName(backupName(context.getClusterContext()))
        .withLabels(ResourceUtil.patroniClusterLabels(context.getClusterContext().getCluster()))
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
        .endMetadata()
        .withData(ResourceUtil.addMd5Sum(data))
        .build());
  }

}
