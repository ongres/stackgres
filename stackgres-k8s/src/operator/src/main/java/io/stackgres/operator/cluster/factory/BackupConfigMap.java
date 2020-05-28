/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class BackupConfigMap extends AbstractBackupConfigMap
    implements StackGresClusterResourceStreamFactory {

  private static final String BACKUP_SUFFIX = "-backup";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + BACKUP_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final Map<String, String> data = new HashMap<>();

    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    clusterContext.getBackupContext()
        .ifPresent(backupContext -> {
          data.put("BACKUP_CONFIG_RESOURCE_VERSION",
              backupContext.getBackupConfig().getMetadata().getResourceVersion());
          data.putAll(getBackupEnvVars(
              cluster.getMetadata().getNamespace(),
              cluster.getMetadata().getName(),
              backupContext.getBackupConfig().getSpec()));
        });

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(clusterContext))
        .withLabels(labelFactory.patroniClusterLabels(cluster))
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
