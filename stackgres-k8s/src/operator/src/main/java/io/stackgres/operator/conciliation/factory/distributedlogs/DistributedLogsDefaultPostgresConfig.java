/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class DistributedLogsDefaultPostgresConfig implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsDefaultPostgresConfig(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getPostgresConfig().isEmpty()
            || context.getPostgresConfig()
            .map(postgresConfig -> postgresConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .map(ignored -> getDefaultConfig(context));
  }

  private StackGresPostgresConfig getDefaultConfig(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();
    return new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getConfigurations().getSgPostgresConfig())
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion(getPostgresMajorVersion(context))
        .endSpec()
        .build();
  }

  private String getPostgresMajorVersion(StackGresDistributedLogsContext context) {
    StackGresCluster cluster = DistributedLogsCluster.getCluster(
        labelFactory, context.getSource(), context.getCluster());
    String version = cluster.getSpec().getPostgres().getVersion();
    return version.split("\\.")[0];
  }

}
