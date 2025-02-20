/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterDefaultPostgresConfig implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public ClusterDefaultPostgresConfig(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getPostgresConfig().isEmpty()
            || context.getPostgresConfig()
            .filter(postgresConfig -> labelFactory.defaultConfigLabels(context.getSource())
                .entrySet()
                .stream()
                .allMatch(label -> Optional
                    .ofNullable(postgresConfig.getMetadata().getLabels())
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .anyMatch(label::equals)))
            .map(postgresConfig -> postgresConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .map(ignored -> getDefaultConfig(context.getSource()));
  }

  private StackGresPostgresConfig getDefaultConfig(StackGresCluster cluster) {
    return new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getConfigurations().getSgPostgresConfig())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion(getPostgresMajorVersion(cluster))
        .endSpec()
        .build();
  }

  private String getPostgresMajorVersion(StackGresCluster cluster) {
    String version = cluster.getSpec().getPostgres().getVersion();
    return version.split("\\.")[0];
  }

}
