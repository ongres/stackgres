/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static io.stackgres.operator.common.StackGresDistributedLogsUtil.TIMESCALEDB_EXTENSION_NAME;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsStatusManager
    extends ConditionUpdater<StackGresDistributedLogs, Condition>
    implements StatusManager<StackGresDistributedLogs, Condition> {

  private final KubernetesClient client;
  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsStatusManager(KubernetesClient client,
      LabelFactoryForDistributedLogs labelFactory) {
    this.client = client;
    this.labelFactory = labelFactory;
  }

  @Override
  public StackGresDistributedLogs refreshCondition(StackGresDistributedLogs source) {
    Optional<StackGresCluster> foundCluster = client.resources(StackGresCluster.class)
        .inNamespace(source.getMetadata().getNamespace())
        .withLabels(labelFactory.genericLabels(source))
        .list()
        .getItems()
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getName(),
            source.getMetadata().getName()))
        .findFirst();
    foundCluster.ifPresent(cluster -> {
      if (source.getStatus() == null) {
        source.setStatus(new StackGresDistributedLogsStatus());
      }
      source.getStatus().setPostgresVersion(cluster.getSpec().getPostgres().getVersion());
      source.getStatus().setTimescaledbVersion(
          Optional.ofNullable(cluster.getSpec().getPostgres().getExtensions())
          .stream()
          .flatMap(List::stream)
          .filter(extension -> Objects.equals(
              extension.getName(),
              TIMESCALEDB_EXTENSION_NAME))
          .map(StackGresClusterExtension::getVersion)
          .findFirst()
          .orElse(null));
    });
    return source;
  }

  @Override
  protected List<Condition> getConditions(
      StackGresDistributedLogs distributedLogs) {
    return Optional.ofNullable(distributedLogs.getStatus())
        .map(StackGresDistributedLogsStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(
      StackGresDistributedLogs distributedLogs,
      List<Condition> conditions) {
    if (distributedLogs.getStatus() == null) {
      distributedLogs.setStatus(new StackGresDistributedLogsStatus());
    }
    distributedLogs.getStatus().setConditions(conditions);
  }

}
