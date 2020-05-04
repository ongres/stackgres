/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class BackupConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterResourceStreamFactory {

  private static final String BACKUP_SUFFIX = "-backup";

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + BACKUP_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final Map<String, String> data = new HashMap<>();

    context.getClusterContext().getBackupContext()
        .ifPresent(backupContext -> {
          data.put("BACKUP_CONFIG_RESOURCE_VERSION",
              backupContext.getBackupConfig().getMetadata().getResourceVersion());
          data.putAll(getBackupEnvVars(
              context.getClusterContext().getCluster().getMetadata().getNamespace(),
              context.getClusterContext().getCluster().getMetadata().getName(),
              backupContext.getBackupConfig().getSpec()));
        });

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withName(name(context.getClusterContext()))
        .withLabels(context.getClusterContext().patroniClusterLabels())
        .withOwnerReferences(context.getClusterContext().ownerReferences())
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build());
  }

}
