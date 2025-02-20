/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class PgBouncerDefaultPoolingConfig implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public PgBouncerDefaultPoolingConfig(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream
        .of(Optional.of(context.getSource().getSpec().getPods())
            .map(StackGresClusterPods::getDisableConnectionPooling)
            .orElse(false))
        .filter(disabled -> !disabled)
        .filter(ignored -> context.getPoolingConfig().isEmpty()
            || context.getPoolingConfig()
            .filter(poolingConfig -> labelFactory.defaultConfigLabels(context.getSource())
                .entrySet()
                .stream()
                .allMatch(label -> Optional
                    .ofNullable(poolingConfig.getMetadata().getLabels())
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .anyMatch(label::equals)))
            .map(poolingConfig -> poolingConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .map(ignored -> getDefaultConfig(context.getSource()));
  }

  private StackGresPoolingConfig getDefaultConfig(StackGresCluster cluster) {
    return new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getConfigurations().getSgPoolingConfig())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withNewPgBouncer()
        .withNewPgbouncerIni()
        .endPgbouncerIni()
        .endPgBouncer()
        .endSpec()
        .build();
  }

}
