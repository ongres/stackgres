/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterControllerReconciliator
    extends Reconciliator<StackGresClusterContext> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterControllerReconciliator.class);

  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ClusterExtensionReconciliator extensionReconciliator;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterControllerReconciliator(Parameters parameters) {
    this.clusterScheduler = parameters.clusterScheduler;
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.clusterFinder = parameters.clusterFinder;
  }

  public ClusterControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.clusterScheduler = null;
    this.extensionReconciliator = null;
    this.clusterFinder = null;
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected ReconciliationResult<?> reconcile(KubernetesClient client,
                                              StackGresClusterContext context) throws Exception {
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    if (extensionReconciliationResult.result().orElse(false)) {
      final StackGresCluster cluster = context.getCluster();
      final StackGresClusterStatus status = cluster.getStatus();
      try {
        LOGGER.debug("Updating status " + status.toString());
        clusterScheduler.updateStatus(cluster);
      } catch (Exception ex) {
        LOGGER.error("Failed to update cluster status, retrying", ex);
        final ObjectMeta metadata = cluster.getMetadata();
        clusterFinder.findByNameAndNamespace(metadata.getName(), metadata.getNamespace())
            .ifPresent(c -> {
              c.setStatus(status);
            });
      }
    }
    return extensionReconciliationResult;
  }

  @Dependent
  public static class Parameters {
    @Inject
    CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject
    ClusterExtensionReconciliator extensionReconciliator;
    @Inject
    CustomResourceFinder<StackGresCluster> clusterFinder;
  }

}
