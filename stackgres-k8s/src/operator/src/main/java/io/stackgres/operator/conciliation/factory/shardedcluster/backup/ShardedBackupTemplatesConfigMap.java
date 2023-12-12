/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.backup;

import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedBackupTemplatesConfigMap
    extends AbstractTemplatesConfigMap
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private LabelFactoryForShardedCluster labelFactory;

  public static String name(StackGresShardedClusterContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream.of(
        createConfigMap(context));
  }

  private HasMetadata createConfigMap(StackGresShardedClusterContext context) {
    Map<String, String> data = getShardedClusterTemplates();

    final StackGresShardedCluster cluster = context.getSource();
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

}
