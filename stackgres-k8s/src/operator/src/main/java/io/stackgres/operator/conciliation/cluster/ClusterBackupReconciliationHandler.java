/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresCluster.class, kind = StackGresBackup.KIND)
@ApplicationScoped
public class ClusterBackupReconciliationHandler
    extends FireAndForgetReconciliationHandler<StackGresCluster> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ResourceScanner<Pod> podScanner;

  @Inject
  public ClusterBackupReconciliationHandler(
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresCluster> handler,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceScanner<Pod> podScanner) {
    super(handler);
    this.labelFactory = labelFactory;
    this.podScanner = podScanner;
  }

  /**
   * If all the replicas that exists are ready or the next SGBackup for replication initialization
   *  is set and is not the same SGBackup created for replication initialization.
   */
  @Override
  protected boolean canForget(StackGresCluster context, HasMetadata resource) {
    var replicas = podScanner.findByLabelsAndNamespace(
        context.getMetadata().getNamespace(),
        labelFactory.clusterReplicaLabels(context));
    return replicas.stream()
        .allMatch(pod -> Optional.ofNullable(pod.getStatus())
            .map(PodStatus::getContainerStatuses)
            .stream()
            .flatMap(List::stream)
            .allMatch(ContainerStatus::getReady))
        || Optional.ofNullable(context.getStatus())
        .map(StackGresClusterStatus::getReplicationInitializationSgBackup)
        .filter(Predicate.not(resource.getMetadata().getName()::equals))
        .isPresent();
  }

}
